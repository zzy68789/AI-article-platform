package com.yupi.template.service;

import cn.hutool.core.util.StrUtil;
import com.yupi.template.config.WechatConfig;
import com.yupi.template.config.WechatOpenPlatformConfig;
import com.yupi.template.constant.UserConstant;
import com.yupi.template.exception.BusinessException;
import com.yupi.template.exception.ErrorCode;
import com.yupi.template.model.entity.User;
import com.yupi.template.model.entity.WechatAuthorizerAccount;
import com.yupi.template.model.enums.WechatAuthorizationStatusEnum;
import jakarta.annotation.Resource;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.open.api.WxOpenService;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class WechatCredentialService {

    @Resource
    private WechatAuthorizerAccountService accountService;

    @Resource
    private WxOpenService wxOpenService;

    @Resource
    private WechatApiClient wechatApiClient;

    @Resource
    private WechatConfig wechatConfig;

    @Resource
    private WechatOpenPlatformConfig openPlatformConfig;

    public WechatCredential resolve(Long wechatAccountId, User loginUser) {
        if (wechatAccountId == null) {
            if (!UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole())) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "请先授权并选择微信公众号");
            }
            return WechatCredential.builder()
                    .authorizerAppid(wechatConfig.getAppId())
                    .accessToken(wechatApiClient.getAccessToken())
                    .build();
        }

        WechatAuthorizerAccount account = accountService.getById(wechatAccountId);
        if (account == null || !Objects.equals(account.getOwnerUserId(), loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权使用该微信公众号");
        }
        if (!WechatAuthorizationStatusEnum.AUTHORIZED.getValue().equals(account.getAuthStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "微信公众号授权已失效，请重新授权");
        }
        validateOpenPlatformConfig();

        String refreshToken = accountService.getDecryptedRefreshToken(account);
        if (StrUtil.isNotBlank(refreshToken)) {
            wxOpenService.getWxOpenConfigStorage()
                    .setAuthorizerRefreshToken(account.getAuthorizerAppid(), refreshToken);
        }
        try {
            String accessToken = wxOpenService.getWxOpenComponentService()
                    .getAuthorizerAccessToken(account.getAuthorizerAppid(), false);
            accountService.updateAccessToken(account, accessToken);
            return WechatCredential.builder()
                    .wechatAccountId(account.getId())
                    .authorizerAppid(account.getAuthorizerAppid())
                    .accessToken(accessToken)
                    .build();
        } catch (WxErrorException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取授权公众号 access_token 失败：" + e.getMessage());
        }
    }

    private void validateOpenPlatformConfig() {
        if (!Boolean.TRUE.equals(openPlatformConfig.getEnabled())
                || StrUtil.isBlank(openPlatformConfig.getComponentAppId())
                || StrUtil.isBlank(openPlatformConfig.getComponentAppSecret())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "微信开放平台第三方平台未配置");
        }
    }
}
