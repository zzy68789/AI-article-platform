package com.yupi.template.model.enums;

import lombok.Getter;

@Getter
public enum WechatAuthorizationStatusEnum {

    AUTHORIZED("AUTHORIZED", "已授权"),
    UNAUTHORIZED("UNAUTHORIZED", "已取消授权");

    private final String value;

    private final String description;

    WechatAuthorizationStatusEnum(String value, String description) {
        this.value = value;
        this.description = description;
    }
}
