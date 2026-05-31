package com.yupi.template.service;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WechatMarkdownRenderServiceTest {

    @Test
    void renderForWechatReplacesImagesAndRemovesUnsafeTags() {
        WechatApiClient wechatApiClient = mock(WechatApiClient.class);
        when(wechatApiClient.uploadContentImage("https://example.com/a.png"))
                .thenReturn("https://mmbiz.qpic.cn/a.png");

        WechatMarkdownRenderService renderService = new WechatMarkdownRenderService();
        ReflectionTestUtils.setField(renderService, "wechatApiClient", wechatApiClient);

        String html = renderService.renderForWechat("""
                ## 小标题

                正文内容

                ![配图](https://example.com/a.png)

                <script>alert('xss')</script>
                """);

        assertThat(html).contains("https://mmbiz.qpic.cn/a.png");
        assertThat(html).contains("font-size:20px");
        assertThat(html).doesNotContain("<script>");
        assertThat(html).doesNotContain("https://example.com/a.png");
    }
}
