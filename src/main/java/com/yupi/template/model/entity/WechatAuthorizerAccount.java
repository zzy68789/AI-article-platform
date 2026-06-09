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
@Table(value = "wechat_authorizer_account", camelToUnderline = false)
public class WechatAuthorizerAccount implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    private Long id;

    private Long ownerUserId;

    private String authorizerAppid;

    private String nickName;

    private String headImg;

    private String principalName;

    private Integer serviceTypeInfo;

    private Integer verifyTypeInfo;

    private String funcInfo;

    private String authorizerRefreshTokenEncrypted;

    private String authorizerAccessTokenEncrypted;

    private LocalDateTime accessTokenExpireTime;

    private String authStatus;

    private Integer isDefault;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @Column(isLogicDelete = true)
    private Integer isDelete;
}
