use ai_passage_creator;

ALTER TABLE article
    MODIFY COLUMN content MEDIUMTEXT NULL COMMENT '正文（Markdown格式）',
    MODIFY COLUMN fullContent MEDIUMTEXT NULL COMMENT '完整图文（Markdown格式，含配图）';

CREATE TABLE IF NOT EXISTS wechat_publish_record
(
    id                  BIGINT AUTO_INCREMENT COMMENT 'id' PRIMARY KEY,
    articleId           BIGINT                                NOT NULL COMMENT '文章ID',
    taskId              VARCHAR(64)                           NOT NULL COMMENT '任务ID',
    userId              BIGINT                                NOT NULL COMMENT '用户ID',
    mediaId             VARCHAR(128)                          NULL COMMENT '微信草稿 media_id',
    publishId           VARCHAR(128)                          NULL COMMENT '微信发布 publish_id',
    articleIdFromWechat VARCHAR(128)                          NULL COMMENT '微信返回的 article_id',
    articleUrl          VARCHAR(1024)                         NULL COMMENT '发布后的文章 URL',
    status              VARCHAR(32)                           NOT NULL COMMENT 'DRAFT_CREATED/SUBMITTED/PUBLISHING/SUCCESS/FAILED',
    mode                VARCHAR(32)                           NOT NULL COMMENT 'DRAFT/PUBLISH',
    articleTitle        VARCHAR(255)                          NULL COMMENT '发布标题',
    officialStatusCode  VARCHAR(64)                           NULL COMMENT '微信官方状态码',
    officialResponse    MEDIUMTEXT                            NULL COMMENT '微信官方响应',
    errorMessage        TEXT                                  NULL COMMENT '错误信息',
    attemptNo           INT      DEFAULT 1                    NOT NULL COMMENT '发布尝试次数',
    createTime          DATETIME DEFAULT CURRENT_TIMESTAMP    NOT NULL COMMENT '创建时间',
    updateTime          DATETIME DEFAULT CURRENT_TIMESTAMP    NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete            TINYINT  DEFAULT 0                    NOT NULL COMMENT '是否删除',
    INDEX idx_taskId (taskId),
    INDEX idx_userId (userId),
    INDEX idx_publishId (publishId),
    INDEX idx_status (status),
    INDEX idx_userId_taskId (userId, taskId)
) COMMENT '微信公众号发布记录' COLLATE = utf8mb4_unicode_ci;
