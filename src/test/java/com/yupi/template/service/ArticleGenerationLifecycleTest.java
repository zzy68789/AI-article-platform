package com.yupi.template.service;

import com.yupi.template.model.entity.Article;
import com.yupi.template.model.entity.User;
import com.yupi.template.model.enums.ArticlePhaseEnum;
import com.yupi.template.model.enums.ArticleStatusEnum;
import com.yupi.template.exception.BusinessException;
import com.yupi.template.service.impl.ArticleServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.springframework.test.util.ReflectionTestUtils;

class ArticleGenerationLifecycleTest {

    private TestableArticleService service;
    private User owner;

    @BeforeEach
    void setUp() {
        service = new TestableArticleService();
        ReflectionTestUtils.setField(service, "clientLeaveGraceSeconds", 30);
        owner = User.builder().id(7L).userRole("user").build();
    }

    @Test
    void resumedClientLeaveDoesNotFailArticle() {
        Article article = processingArticle("task-1", LocalDateTime.now());
        service.put(article);

        service.markGenerationClientLeft("task-1", "session-1", owner);
        service.resumeGenerationClient("task-1", "session-1", owner);

        service.setNow(service.now().plusSeconds(31));
        int failedCount = service.markExpiredClientLeftTasksFailed();

        assertThat(failedCount).isZero();
        assertThat(article.getStatus()).isEqualTo(ArticleStatusEnum.PROCESSING.getValue());
    }

    @Test
    void expiredClientLeaveFailsActiveArticle() {
        Article article = processingArticle("task-1", LocalDateTime.now());
        service.put(article);

        service.markGenerationClientLeft("task-1", "session-1", owner);
        service.setNow(service.now().plusSeconds(31));
        int failedCount = service.markExpiredClientLeftTasksFailed();

        assertThat(failedCount).isEqualTo(1);
        assertThat(article.getStatus()).isEqualTo(ArticleStatusEnum.FAILED.getValue());
        assertThat(article.getPhase()).isEqualTo(ArticlePhaseEnum.FAILED.getValue());
        assertThat(article.getErrorMessage()).contains("页面已关闭");
    }

    @Test
    void clientLeaveDoesNotFailBeforeGraceWindowExpires() {
        Article article = processingArticle("task-1", LocalDateTime.now());
        service.put(article);

        service.markGenerationClientLeft("task-1", "session-1", owner);
        service.setNow(service.now().plusSeconds(29));

        assertThat(service.markExpiredClientLeftTasksFailed()).isZero();
        assertThat(article.getStatus()).isEqualTo(ArticleStatusEnum.PROCESSING.getValue());
    }

    @Test
    void concurrentResumePreventsExpiredLeaveFromFailingArticle() {
        Article article = processingArticle("task-1", LocalDateTime.now());
        service.put(article);
        service.markGenerationClientLeft("task-1", "session-1", owner);
        service.setNow(service.now().plusSeconds(31));
        service.beforeNextArticleLookup(() ->
                service.resumeGenerationClient("task-1", "session-1", owner));

        assertThat(service.markExpiredClientLeftTasksFailed()).isZero();
        assertThat(article.getStatus()).isEqualTo(ArticleStatusEnum.PROCESSING.getValue());
    }

    @Test
    void completedArticleIsNotFailedByLateClientLeave() {
        Article article = processingArticle("task-1", LocalDateTime.now());
        article.setStatus(ArticleStatusEnum.COMPLETED.getValue());
        article.setPhase(ArticlePhaseEnum.COMPLETED.getValue());
        service.put(article);

        service.markGenerationClientLeft("task-1", "session-1", owner);
        service.setNow(service.now().plusSeconds(31));
        int failedCount = service.markExpiredClientLeftTasksFailed();

        assertThat(failedCount).isZero();
        assertThat(article.getStatus()).isEqualTo(ArticleStatusEnum.COMPLETED.getValue());
    }

    @Test
    void staleProcessingArticlesAreMarkedFailed() {
        Article stale = processingArticle("stale", service.now().minusMinutes(31));
        Article fresh = processingArticle("fresh", service.now().minusMinutes(5));
        Article completed = processingArticle("completed", service.now().minusHours(2));
        completed.setStatus(ArticleStatusEnum.COMPLETED.getValue());
        completed.setPhase(ArticlePhaseEnum.COMPLETED.getValue());
        service.put(stale);
        service.put(fresh);
        service.put(completed);

        int failedCount = service.markStaleProcessingArticlesFailed(30);

        assertThat(failedCount).isEqualTo(1);
        assertThat(stale.getStatus()).isEqualTo(ArticleStatusEnum.FAILED.getValue());
        assertThat(stale.getErrorMessage()).contains("超过30分钟未更新");
        assertThat(fresh.getStatus()).isEqualTo(ArticleStatusEnum.PROCESSING.getValue());
        assertThat(completed.getStatus()).isEqualTo(ArticleStatusEnum.COMPLETED.getValue());
    }

    @Test
    void failedArticleIsNotActiveForLateAsyncWrites() {
        Article article = processingArticle("task-1", LocalDateTime.now());
        article.setStatus(ArticleStatusEnum.FAILED.getValue());
        article.setPhase(ArticlePhaseEnum.FAILED.getValue());
        service.put(article);

        assertThat(service.isArticleActive("task-1")).isFalse();
    }

    @Test
    void failedArticleCannotBeOverwrittenByLateCompletion() {
        Article article = processingArticle("task-1", LocalDateTime.now());
        article.setStatus(ArticleStatusEnum.FAILED.getValue());
        article.setPhase(ArticlePhaseEnum.FAILED.getValue());
        service.put(article);

        service.updateArticleStatus("task-1", ArticleStatusEnum.COMPLETED, null);

        assertThat(article.getStatus()).isEqualTo(ArticleStatusEnum.FAILED.getValue());
        assertThat(article.getPhase()).isEqualTo(ArticlePhaseEnum.FAILED.getValue());
    }

    @Test
    void failedArticlePhaseCannotBeOverwrittenByLateCompletion() {
        Article article = processingArticle("task-1", LocalDateTime.now());
        article.setStatus(ArticleStatusEnum.FAILED.getValue());
        article.setPhase(ArticlePhaseEnum.FAILED.getValue());
        service.put(article);

        service.updatePhase("task-1", ArticlePhaseEnum.COMPLETED);

        assertThat(article.getPhase()).isEqualTo(ArticlePhaseEnum.FAILED.getValue());
    }

    @Test
    void nonOwnerCannotMarkLeaveOrResume() {
        Article article = processingArticle("task-1", LocalDateTime.now());
        service.put(article);
        User anotherUser = User.builder().id(8L).userRole("user").build();

        assertThatThrownBy(() -> service.markGenerationClientLeft("task-1", "session-1", anotherUser))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.resumeGenerationClient("task-1", "session-1", anotherUser))
                .isInstanceOf(BusinessException.class);
    }

    private Article processingArticle(String taskId, LocalDateTime updateTime) {
        return Article.builder()
                .id((long) taskId.hashCode())
                .taskId(taskId)
                .userId(owner.getId())
                .topic("topic")
                .status(ArticleStatusEnum.PROCESSING.getValue())
                .phase(ArticlePhaseEnum.CONTENT_GENERATING.getValue())
                .updateTime(updateTime)
                .isDelete(0)
                .build();
    }

    private static class TestableArticleService extends ArticleServiceImpl {
        private final Map<String, Article> articles = new HashMap<>();
        private LocalDateTime now = LocalDateTime.of(2026, 6, 18, 10, 0);
        private Runnable beforeNextArticleLookup;

        void put(Article article) {
            articles.put(article.getTaskId(), article);
        }

        void setNow(LocalDateTime now) {
            this.now = now;
        }

        void beforeNextArticleLookup(Runnable action) {
            this.beforeNextArticleLookup = action;
        }

        @Override
        protected LocalDateTime now() {
            return now;
        }

        @Override
        public Article getByTaskId(String taskId) {
            Runnable action = beforeNextArticleLookup;
            beforeNextArticleLookup = null;
            if (action != null) {
                action.run();
            }
            return articles.get(taskId);
        }

        @Override
        public boolean updateById(Article article) {
            articles.put(article.getTaskId(), article);
            return true;
        }

        @Override
        protected boolean updateActiveArticle(Article article) {
            articles.put(article.getTaskId(), article);
            return true;
        }

        @Override
        protected List<Article> findStaleActiveArticles(LocalDateTime cutoffTime) {
            return articles.values().stream()
                    .filter(article -> ArticleStatusEnum.PROCESSING.getValue().equals(article.getStatus())
                            || ArticleStatusEnum.PENDING.getValue().equals(article.getStatus()))
                    .filter(article -> article.getUpdateTime() != null && article.getUpdateTime().isBefore(cutoffTime))
                    .toList();
        }
    }
}
