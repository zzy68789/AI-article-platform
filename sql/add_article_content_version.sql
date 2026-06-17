use ai_passage_creator;

CREATE TABLE IF NOT EXISTS article_content_version (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    articleId BIGINT NOT NULL COMMENT '文章ID',
    taskId VARCHAR(64) NOT NULL COMMENT '任务ID',
    userId BIGINT NOT NULL COMMENT '用户ID',
    versionNo INT NOT NULL COMMENT '版本号',
    title VARCHAR(256) NULL COMMENT '标题',
    subTitle VARCHAR(512) NULL COMMENT '副标题',
    markdown MEDIUMTEXT NOT NULL COMMENT '正文 Markdown',
    contentHash VARCHAR(64) NOT NULL COMMENT '正文 SHA-256',
    source VARCHAR(32) NOT NULL COMMENT 'AI_GENERATED / MANUAL_SAVE / ROLLBACK',
    rollbackFromVersionNo INT NULL COMMENT '回滚来源版本号',
    remark VARCHAR(512) NULL COMMENT '备注',
    wordCount INT NULL COMMENT '字数',
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete TINYINT DEFAULT 0 NOT NULL COMMENT '是否删除',
    UNIQUE KEY uk_task_version (taskId, versionNo),
    INDEX idx_task_createTime (taskId, createTime),
    INDEX idx_articleId (articleId)
) COMMENT '文章正文版本表';

ALTER TABLE wechat_publish_record
    ADD COLUMN contentHash VARCHAR(64) NULL COMMENT '发布时正文hash' AFTER articleTitle,
    ADD COLUMN contentVersionNo INT NULL COMMENT '发布时正文版本号' AFTER contentHash;
