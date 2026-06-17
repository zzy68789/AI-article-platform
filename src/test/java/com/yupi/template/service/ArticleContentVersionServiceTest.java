package com.yupi.template.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.yupi.template.mapper.ArticleContentVersionMapper;
import com.yupi.template.model.dto.article.ArticleContentRollbackRequest;
import com.yupi.template.model.dto.article.ArticleContentUpdateRequest;
import com.yupi.template.model.entity.Article;
import com.yupi.template.model.entity.ArticleContentVersion;
import com.yupi.template.model.entity.User;
import com.yupi.template.model.enums.ArticleContentVersionSourceEnum;
import com.yupi.template.model.enums.ArticleStatusEnum;
import com.yupi.template.model.vo.ArticleContentVersionVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArticleContentVersionServiceTest {

    private ArticleContentVersionService versionService;
    private ArticleContentVersionMapper versionMapper;
    private ArticleService articleService;
    private List<ArticleContentVersion> storedVersions;
    private Article article;
    private User owner;

    @BeforeEach
    void setUp() {
        versionService = new ArticleContentVersionService();
        versionMapper = mock(ArticleContentVersionMapper.class);
        articleService = mock(ArticleService.class);
        storedVersions = new ArrayList<>();
        article = Article.builder()
                .id(10L)
                .taskId("task-1")
                .userId(7L)
                .mainTitle("Title")
                .subTitle("Subtitle")
                .content("AI draft")
                .fullContent("AI draft with image")
                .status(ArticleStatusEnum.COMPLETED.getValue())
                .build();
        owner = User.builder().id(7L).userRole("user").build();

        ReflectionTestUtils.setField(versionService, "versionMapper", versionMapper);
        ReflectionTestUtils.setField(versionService, "articleService", articleService);
        when(articleService.getByTaskId("task-1")).thenReturn(article);
        when(versionMapper.selectListByQuery(any(QueryWrapper.class))).thenAnswer(invocation ->
                storedVersions.stream()
                        .sorted(Comparator.comparing(ArticleContentVersion::getVersionNo).reversed())
                        .toList()
        );
        doAnswer(invocation -> {
            ArticleContentVersion version = invocation.getArgument(0);
            version.setId((long) storedVersions.size() + 1);
            storedVersions.add(version);
            return 1;
        }).when(versionMapper).insert(any(ArticleContentVersion.class));
    }

    @Test
    void updateContentInitializesAiVersionAndSavesManualVersion() {
        ArticleContentUpdateRequest request = new ArticleContentUpdateRequest();
        request.setTaskId("task-1");
        request.setMarkdown("edited markdown");
        request.setRemark("manual edit");

        ArticleContentVersionVO result = versionService.updateContent(request, owner);

        assertThat(result.getVersionNo()).isEqualTo(2);
        assertThat(result.getSource()).isEqualTo(ArticleContentVersionSourceEnum.MANUAL_SAVE.getValue());
        assertThat(article.getFullContent()).isEqualTo("edited markdown");
        assertThat(storedVersions)
                .extracting(ArticleContentVersion::getSource)
                .containsExactly(
                        ArticleContentVersionSourceEnum.AI_GENERATED.getValue(),
                        ArticleContentVersionSourceEnum.MANUAL_SAVE.getValue()
                );
        verify(articleService).updateById(article);
    }

    @Test
    void updateContentReturnsLatestVersionWhenMarkdownHashIsUnchanged() {
        ArticleContentVersion existing = ArticleContentVersion.builder()
                .articleId(10L)
                .taskId("task-1")
                .userId(7L)
                .versionNo(1)
                .markdown("same markdown")
                .contentHash(ArticleContentVersionService.hashMarkdown("same markdown"))
                .source(ArticleContentVersionSourceEnum.MANUAL_SAVE.getValue())
                .build();
        storedVersions.add(existing);

        ArticleContentUpdateRequest request = new ArticleContentUpdateRequest();
        request.setTaskId("task-1");
        request.setMarkdown("same markdown");

        ArticleContentVersionVO result = versionService.updateContent(request, owner);

        assertThat(result.getVersionNo()).isEqualTo(1);
        assertThat(storedVersions).hasSize(1);
    }

    @Test
    void rollbackContentCreatesNewRollbackVersionAndUpdatesArticle() {
        storedVersions.add(ArticleContentVersion.builder()
                .articleId(10L)
                .taskId("task-1")
                .userId(7L)
                .versionNo(1)
                .markdown("AI draft")
                .contentHash(ArticleContentVersionService.hashMarkdown("AI draft"))
                .source(ArticleContentVersionSourceEnum.AI_GENERATED.getValue())
                .build());
        storedVersions.add(ArticleContentVersion.builder()
                .articleId(10L)
                .taskId("task-1")
                .userId(7L)
                .versionNo(2)
                .markdown("edited")
                .contentHash(ArticleContentVersionService.hashMarkdown("edited"))
                .source(ArticleContentVersionSourceEnum.MANUAL_SAVE.getValue())
                .build());

        ArticleContentRollbackRequest request = new ArticleContentRollbackRequest();
        request.setTaskId("task-1");
        request.setVersionNo(1);
        request.setRemark("rollback");

        ArticleContentVersionVO result = versionService.rollbackContent(request, owner);

        assertThat(result.getVersionNo()).isEqualTo(3);
        assertThat(result.getSource()).isEqualTo(ArticleContentVersionSourceEnum.ROLLBACK.getValue());
        assertThat(result.getRollbackFromVersionNo()).isEqualTo(1);
        assertThat(article.getFullContent()).isEqualTo("AI draft");
        verify(articleService).updateById(article);
    }
}
