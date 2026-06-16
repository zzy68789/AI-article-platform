package com.yupi.template.service;

import cn.hutool.core.util.StrUtil;
import com.yupi.template.config.WechatOpenPlatformConfig;
import com.yupi.template.exception.BusinessException;
import com.yupi.template.exception.ErrorCode;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class WechatCredentialCipher {

    private static final String PREFIX = "v1:";

    private static final int IV_LENGTH = 12;

    private static final int GCM_TAG_LENGTH = 128;

    private final WechatOpenPlatformConfig config;

    private final SecureRandom secureRandom = new SecureRandom();

    public WechatCredentialCipher(WechatOpenPlatformConfig config) {
        this.config = config;
    }

    public String encrypt(String plaintext) {
        if (StrUtil.isBlank(plaintext)) {
            return plaintext;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey(), new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] payload = ByteBuffer.allocate(iv.length + encrypted.length)
                    .put(iv)
                    .put(encrypted)
                    .array();
            return PREFIX + Base64.getEncoder().encodeToString(payload);
        } catch (GeneralSecurityException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "微信公众号凭证加密失败");
        }
    }

    public String decrypt(String ciphertext) {
        if (StrUtil.isBlank(ciphertext)) {
            return ciphertext;
        }
        if (!ciphertext.startsWith(PREFIX)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "微信公众号凭证格式无效");
        }
        try {
            byte[] payload = Base64.getDecoder().decode(ciphertext.substring(PREFIX.length()));
            ByteBuffer buffer = ByteBuffer.wrap(payload);
            byte[] iv = new byte[IV_LENGTH];
            buffer.get(iv);
            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey(), new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "微信公众号凭证解密失败");
        }
    }

    private SecretKeySpec encryptionKey() {
        if (StrUtil.isBlank(config.getCredentialEncryptionKey())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未配置微信公众号凭证加密密钥");
        }
        byte[] key;
        try {
            key = Base64.getDecoder().decode(config.getCredentialEncryptionKey());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "微信公众号凭证加密密钥不是有效 Base64");
        }
        if (key.length != 16 && key.length != 24 && key.length != 32) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "微信公众号凭证加密密钥长度必须为 16、24 或 32 字节");
        }
        return new SecretKeySpec(key, "AES");
    }
}
