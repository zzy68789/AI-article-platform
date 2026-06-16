package com.yupi.template.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(value = "wechat_publish_record", camelToUnderline = false)
public class WechatPublishRecord implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    private Long id;

    private Long articleId;

    private String taskId;

    private Long userId;

    private Long wechatAccountId;

    private String authorizerAppid;

    private String mediaId;

    private String publishId;

    private String articleIdFromWechat;

    private String articleUrl;

    private String status;

    private String mode;

    private String articleTitle;

    private String officialStatusCode;

    private String officialResponse;

    private String errorMessage;

    private Integer attemptNo;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @Column(isLogicDelete = true)
    private Integer isDelete;
}
