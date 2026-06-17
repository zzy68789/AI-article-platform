package com.yupi.template.model.enums;

import lombok.Getter;

@Getter
public enum ArticleContentVersionSourceEnum {

    AI_GENERATED("AI_GENERATED", "AI generated content"),
    MANUAL_SAVE("MANUAL_SAVE", "Manual save"),
    ROLLBACK("ROLLBACK", "Rollback");

    private final String value;

    private final String description;

    ArticleContentVersionSourceEnum(String value, String description) {
        this.value = value;
        this.description = description;
    }
}
