package com.yupi.template.model.enums;

import lombok.Getter;

@Getter
public enum WechatPublishStatusEnum {

    DRAFT_CREATED("DRAFT_CREATED", "草稿已创建"),
    SUBMITTED("SUBMITTED", "已提交发布"),
    PUBLISHING("PUBLISHING", "发布中"),
    SUCCESS("SUCCESS", "发布成功"),
    FAILED("FAILED", "发布失败");

    private final String value;

    private final String description;

    WechatPublishStatusEnum(String value, String description) {
        this.value = value;
        this.description = description;
    }
}
