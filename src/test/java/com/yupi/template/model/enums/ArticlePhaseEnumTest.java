package com.yupi.template.model.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArticlePhaseEnumTest {

    @Test
    void contentGeneratingCanTransitionToTerminalPhases() {
        assertTrue(ArticlePhaseEnum.CONTENT_GENERATING.canTransitionTo(ArticlePhaseEnum.COMPLETED));
        assertTrue(ArticlePhaseEnum.CONTENT_GENERATING.canTransitionTo(ArticlePhaseEnum.FAILED));
    }

    @Test
    void terminalPhasesDoNotTransitionFurther() {
        assertFalse(ArticlePhaseEnum.COMPLETED.canTransitionTo(ArticlePhaseEnum.FAILED));
        assertFalse(ArticlePhaseEnum.FAILED.canTransitionTo(ArticlePhaseEnum.COMPLETED));
    }
}
