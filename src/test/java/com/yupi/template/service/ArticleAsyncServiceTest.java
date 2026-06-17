package com.yupi.template.service;

import com.yupi.template.model.dto.article.ArticleState;
import com.yupi.template.model.enums.SseMessageTypeEnum;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ArticleAsyncServiceTest {

    @Test
    void imageCompleteMessageIncludesStableProgress() {
        ArticleAsyncService service = new ArticleAsyncService();
        String message = SseMessageTypeEnum.IMAGE_COMPLETE.getStreamingPrefix()
                + """
                {
                  "image": {
                    "position": 2,
                    "url": "https://example.com/image.png",
                    "method": "PEXELS",
                    "keywords": "machine learning"
                  },
                  "current": 2,
                  "total": 6
                }
                """;

        Map<String, Object> data = ReflectionTestUtils.invokeMethod(
                service,
                "buildMessageData",
                message,
                new ArticleState()
        );

        assertThat(data).isNotNull();
        assertThat(data).containsEntry("type", SseMessageTypeEnum.IMAGE_COMPLETE.getValue());
        assertThat(data).containsEntry("current", 2);
        assertThat(data).containsEntry("total", 6);
        assertThat(data.get("image")).isInstanceOf(ArticleState.ImageResult.class);
        ArticleState.ImageResult image = (ArticleState.ImageResult) data.get("image");
        assertThat(image.getPosition()).isEqualTo(2);
        assertThat(image.getUrl()).isEqualTo("https://example.com/image.png");
    }
}
