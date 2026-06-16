package com.yupi.template.model.enums;

import lombok.Getter;

@Getter
public enum WechatPublishModeEnum {

    DRAFT("DRAFT", "保存草稿"),
    PUBLISH("PUBLISH", "提交发布");

    private final String value;

    private final String description;

    WechatPublishModeEnum(String value, String description) {
        this.value = value;
        this.description = description;
    }
}
