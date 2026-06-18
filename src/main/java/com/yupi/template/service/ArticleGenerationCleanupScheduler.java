package com.yupi.template.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 文章生成任务清理器。
 */
@Component
@Slf4j
public class ArticleGenerationCleanupScheduler {

    private final ArticleService articleService;

    @Value("${article.generation.stale-timeout-minutes:30}")
    private int staleTimeoutMinutes;

    @Value("${article.generation.cleanup-on-startup:true}")
    private boolean cleanupOnStartup;

    public ArticleGenerationCleanupScheduler(ArticleService articleService) {
        this.articleService = articleService;
    }

    @PostConstruct
    public void cleanupOnStartup() {
        if (cleanupOnStartup) {
            cleanupStaleArticles();
        }
    }

    @Scheduled(
            initialDelayString = "${article.generation.client-leave-check-fixed-delay-ms:1000}",
            fixedDelayString = "${article.generation.client-leave-check-fixed-delay-ms:1000}"
    )
    public void cleanupExpiredClientLeaves() {
        int expiredClientLeftCount = articleService.markExpiredClientLeftTasksFailed();
        if (expiredClientLeftCount > 0) {
            log.info("文章生成页面离开任务清理完成, expiredClientLeft={}", expiredClientLeftCount);
        }
    }

    @Scheduled(
            initialDelayString = "${article.generation.cleanup-initial-delay-ms:300000}",
            fixedDelayString = "${article.generation.cleanup-fixed-delay-ms:300000}"
    )
    public void cleanupStaleArticles() {
        int staleProcessingCount = articleService.markStaleProcessingArticlesFailed(staleTimeoutMinutes);
        if (staleProcessingCount > 0) {
            log.info("陈旧文章生成任务清理完成, staleProcessing={}", staleProcessingCount);
        }
    }
}
