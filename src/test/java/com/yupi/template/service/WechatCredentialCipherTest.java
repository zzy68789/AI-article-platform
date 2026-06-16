package com.yupi.template.service;

import com.yupi.template.config.WechatOpenPlatformConfig;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class WechatCredentialCipherTest {

    @Test
    void encryptsAndDecryptsWechatCredentials() {
        WechatOpenPlatformConfig config = new WechatOpenPlatformConfig();
        config.setCredentialEncryptionKey(Base64.getEncoder()
                .encodeToString("0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8)));
        WechatCredentialCipher cipher = new WechatCredentialCipher(config);

        String encrypted = cipher.encrypt("authorizer-refresh-token");

        assertThat(encrypted).startsWith("v1:");
        assertThat(encrypted).doesNotContain("authorizer-refresh-token");
        assertThat(cipher.decrypt(encrypted)).isEqualTo("authorizer-refresh-token");
    }
}
