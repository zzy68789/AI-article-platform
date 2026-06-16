package com.yupi.template.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "wechat.open-platform")
@Data
public class WechatOpenPlatformConfig {

    private Boolean enabled = false;

    private String componentAppId;

    private String componentAppSecret;

    private String componentToken;

    private String componentAesKey;

    private String authorizationRedirectUri;

    private String frontendSuccessUrl;

    private String credentialEncryptionKey;
}
