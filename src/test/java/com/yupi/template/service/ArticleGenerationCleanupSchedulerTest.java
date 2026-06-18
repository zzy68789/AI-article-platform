package com.yupi.template.service;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class ArticleGenerationCleanupSchedulerTest {

    @Test
    void clientLeaveCleanupOnlyChecksExpiredLeaveRecords() {
        ArticleService articleService = mock(ArticleService.class);
        ArticleGenerationCleanupScheduler scheduler = new ArticleGenerationCleanupScheduler(articleService);

        scheduler.cleanupExpiredClientLeaves();

        verify(articleService).markExpiredClientLeftTasksFailed();
        verifyNoMoreInteractions(articleService);
    }

    @Test
    void staleCleanupOnlyChecksStaleArticles() {
        ArticleService articleService = mock(ArticleService.class);
        ArticleGenerationCleanupScheduler scheduler = new ArticleGenerationCleanupScheduler(articleService);
        ReflectionTestUtils.setField(scheduler, "staleTimeoutMinutes", 30);

        scheduler.cleanupStaleArticles();

        verify(articleService).markStaleProcessingArticlesFailed(30);
        verifyNoMoreInteractions(articleService);
    }
}
