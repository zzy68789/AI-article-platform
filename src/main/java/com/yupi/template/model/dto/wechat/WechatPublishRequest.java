package com.yupi.template.model.dto.wechat;

import lombok.Data;

import java.io.Serializable;

@Data
public class WechatPublishRequest implements Serializable {

    private Boolean force = false;

    private String coverImageUrl;

    private Long wechatAccountId;

    private static final long serialVersionUID = 1L;
}
