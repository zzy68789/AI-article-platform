package com.yupi.template.model.dto.article;

import lombok.Data;

import java.io.Serializable;

@Data
public class ArticleContentUpdateRequest implements Serializable {

    private String taskId;

    private String markdown;

    private String remark;

    private static final long serialVersionUID = 1L;
}
