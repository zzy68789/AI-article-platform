package com.yupi.template.model.dto.article;

import lombok.Data;

import java.io.Serializable;

/**
 * 文章生成页面离开/恢复请求。
 */
@Data
public class ArticleGenerationLeaveRequest implements Serializable {

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 客户端会话ID，用于区分刷新恢复和真正关闭页面。
     */
    private String clientSessionId;

    private static final long serialVersionUID = 1L;
}
