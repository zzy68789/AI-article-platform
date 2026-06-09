use ai_passage_creator;

CREATE TABLE IF NOT EXISTS wechat_authorizer_account
(
    id                              BIGINT AUTO_INCREMENT COMMENT 'id' PRIMARY KEY,
    ownerUserId                     BIGINT                              NOT NULL COMMENT '平台用户ID',
    authorizerAppid                 VARCHAR(128)                        NOT NULL COMMENT '授权公众号 AppID',
    nickName                        VARCHAR(255)                        NULL COMMENT '公众号名称',
    headImg                         VARCHAR(1024)                       NULL COMMENT '公众号头像',
    principalName                   VARCHAR(255)                        NULL COMMENT '公众号主体名称',
    serviceTypeInfo                 INT                                 NULL COMMENT '公众号类型',
    verifyTypeInfo                  INT                                 NULL COMMENT '认证类型',
    funcInfo                        TEXT                                NULL COMMENT '授权权限集 JSON',
    authorizerRefreshTokenEncrypted TEXT                                NOT NULL COMMENT '加密后的刷新令牌',
    authorizerAccessTokenEncrypted  TEXT                                NULL COMMENT '加密后的访问令牌',
    accessTokenExpireTime           DATETIME                            NULL COMMENT '访问令牌过期时间',
    authStatus                      VARCHAR(32)                         NOT NULL COMMENT 'AUTHORIZED/UNAUTHORIZED',
    isDefault                       TINYINT  DEFAULT 0                  NOT NULL COMMENT '是否默认公众号',
    createTime                      DATETIME DEFAULT CURRENT_TIMESTAMP  NOT NULL COMMENT '创建时间',
    updateTime                      DATETIME DEFAULT CURRENT_TIMESTAMP  NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete                        TINYINT  DEFAULT 0                  NOT NULL COMMENT '是否删除',
    UNIQUE INDEX uk_authorizerAppid (authorizerAppid),
    INDEX idx_ownerUserId (ownerUserId),
    INDEX idx_ownerUserId_status (ownerUserId, authStatus)
) COMMENT '用户授权的微信公众号' COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS wechat_open_platform_ticket
(
    id                             BIGINT AUTO_INCREMENT COMMENT 'id' PRIMARY KEY,
    componentAppid                 VARCHAR(128)                       NOT NULL COMMENT '第三方平台 AppID',
    componentVerifyTicketEncrypted TEXT                               NOT NULL COMMENT '加密后的 component_verify_ticket',
    updateTime                     DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE INDEX uk_componentAppid (componentAppid)
) COMMENT '微信开放平台票据' COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS wechat_authorization_state
(
    id         BIGINT AUTO_INCREMENT COMMENT 'id' PRIMARY KEY,
    state      VARCHAR(64)                        NOT NULL COMMENT '授权状态随机值',
    userId     BIGINT                             NOT NULL COMMENT '发起授权的平台用户ID',
    expireTime DATETIME                           NOT NULL COMMENT '过期时间',
    used       TINYINT  DEFAULT 0                 NOT NULL COMMENT '是否已使用',
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    UNIQUE INDEX uk_state (state),
    INDEX idx_userId (userId)
) COMMENT '微信公众号授权临时状态' COLLATE = utf8mb4_unicode_ci;

ALTER TABLE wechat_publish_record
    ADD COLUMN wechatAccountId BIGINT NULL COMMENT '授权公众号记录ID' AFTER userId,
    ADD COLUMN authorizerAppid VARCHAR(128) NULL COMMENT '发布目标公众号 AppID' AFTER wechatAccountId,
    ADD INDEX idx_wechatAccountId (wechatAccountId),
    ADD INDEX idx_taskId_accountId (taskId, wechatAccountId);
