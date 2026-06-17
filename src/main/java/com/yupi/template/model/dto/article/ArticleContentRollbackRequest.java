package com.yupi.template.model.dto.article;

import lombok.Data;

import java.io.Serializable;

@Data
public class ArticleContentRollbackRequest implements Serializable {

    private String taskId;

    private Integer versionNo;

    private String remark;

    private static final long serialVersionUID = 1L;
}
