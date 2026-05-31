package com.yupi.template.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "wechat")
@Data
public class WechatConfig {

    private String appId;

    private String appSecret;

    private String defaultAuthor;

    private String baseUrl = "https://api.weixin.qq.com/cgi-bin";

    private Integer connectTimeoutMs = 10000;

    private Integer readTimeoutMs = 30000;
}
