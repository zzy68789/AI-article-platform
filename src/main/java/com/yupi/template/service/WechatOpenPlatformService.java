package com.yupi.template.service;

import cn.hutool.core.util.StrUtil;
import com.yupi.template.config.WechatOpenPlatformConfig;
import com.yupi.template.exception.BusinessException;
import com.yupi.template.exception.ErrorCode;
import com.yupi.template.model.entity.User;
import com.yupi.template.model.entity.WechatAuthorizerAccount;
import com.yupi.template.model.vo.WechatAuthorizerAccountVO;
import com.yupi.template.model.vo.WechatAuthorizationUrlVO;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.open.api.WxOpenComponentService;
import me.chanjar.weixin.open.api.WxOpenService;
import me.chanjar.weixin.open.bean.auth.WxOpenAuthorizationInfo;
import me.chanjar.weixin.open.bean.message.WxOpenXmlMessage;
import me.chanjar.weixin.open.bean.result.WxOpenAuthorizerInfoResult;
import me.chanjar.weixin.open.bean.result.WxOpenQueryAuthResult;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Service
@Slf4j
public class WechatOpenPlatformService {

    @Resource
    private WechatOpenPlatformConfig config;

    @Resource
    private WxOpenService wxOpenService;

    @Resource
    private WechatOpenPlatformTicketService ticketService;

    @Resource
    private WechatAuthorizationStateService stateService;

    @Resource
    private WechatAuthorizerAccountService accountService;

    @PostConstruct
    public void hydrateWechatOpenStorage() {
        if (!Boolean.TRUE.equals(config.getEnabled())) {
            return;
        }
        String ticket = ticketService.getTicket(config.getComponentAppId());
        if (StrUtil.isNotBlank(ticket)) {
            wxOpenService.getWxOpenConfigStorage().setComponentVerifyTicket(ticket);
        }
        for (WechatAuthorizerAccount account : accountService.listAuthorizedAccounts()) {
            String refreshToken = accountService.getDecryptedRefreshToken(account);
            if (StrUtil.isNotBlank(refreshToken)) {
                wxOpenService.getWxOpenConfigStorage()
                        .setAuthorizerRefreshToken(account.getAuthorizerAppid(), refreshToken);
            }
        }
    }

    public String receiveTicket(String encryptedXml, String msgSignature, String timestamp, String nonce) {
        validateConfig();
        try {
            WxOpenXmlMessage message = WxOpenXmlMessage.fromEncryptedXml(
                    encryptedXml,
                    wxOpenService.getWxOpenConfigStorage(),
                    timestamp,
                    nonce,
                    msgSignature
            );
            String result = wxOpenService.getWxOpenComponentService().route(message);
            if ("component_verify_ticket".equalsIgnoreCase(message.getInfoType())) {
                ticketService.saveTicket(config.getComponentAppId(), message.getComponentVerifyTicket());
            } else if ("unauthorized".equalsIgnoreCase(message.getInfoType())) {
                accountService.markUnauthorized(message.getAuthorizerAppid());
            }
            return result;
        } catch (WxErrorException | RuntimeException e) {
            log.error("处理微信开放平台授权事件失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "处理微信开放平台授权事件失败");
        }
    }

    public WechatAuthorizationUrlVO createAuthorizationUrl(User loginUser) {
        validateConfig();
        String state = stateService.createState(loginUser.getId());
        String redirectUri = UriComponentsBuilder.fromUriString(config.getAuthorizationRedirectUri())
                .queryParam("state", state)
                .build()
                .toUriString();
        try {
            String url = wxOpenService.getWxOpenComponentService()
                    .getPreAuthUrl(redirectUri, "1", null);
            return new WechatAuthorizationUrlVO(url);
        } catch (WxErrorException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "生成微信公众号授权链接失败：" + e.getMessage());
        }
    }

    public WechatAuthorizerAccountVO completeAuthorization(String authorizationCode, String state) {
        validateConfig();
        if (StrUtil.isBlank(authorizationCode) || StrUtil.isBlank(state)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "授权回调参数不完整");
        }
        Long userId = stateService.consumeState(state);
        WxOpenComponentService componentService = wxOpenService.getWxOpenComponentService();
        try {
            WxOpenQueryAuthResult queryAuthResult = componentService.getQueryAuth(authorizationCode);
            WxOpenAuthorizationInfo authorizationInfo = queryAuthResult.getAuthorizationInfo();
            WxOpenAuthorizerInfoResult authorizerInfoResult =
                    componentService.getAuthorizerInfo(authorizationInfo.getAuthorizerAppid());
            if (authorizerInfoResult.isMiniProgram()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "当前仅支持微信公众号授权，不支持小程序");
            }
            wxOpenService.getWxOpenConfigStorage().setAuthorizerRefreshToken(
                    authorizationInfo.getAuthorizerAppid(),
                    authorizationInfo.getAuthorizerRefreshToken()
            );
            WechatAuthorizerAccount account = accountService.saveAuthorization(
                    userId,
                    authorizationInfo,
                    authorizerInfoResult.getAuthorizerInfo()
            );
            return WechatAuthorizerAccountVO.objToVo(account);
        } catch (WxErrorException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "完成微信公众号授权失败：" + e.getMessage());
        }
    }

    public List<WechatAuthorizerAccountVO> listAccounts(User loginUser) {
        return accountService.listForUser(loginUser.getId());
    }

    public void setDefault(Long accountId, User loginUser) {
        accountService.setDefault(accountId, loginUser);
    }

    public void unbind(Long accountId, User loginUser) {
        accountService.unbind(accountId, loginUser);
    }

    public String getFrontendSuccessUrl() {
        return config.getFrontendSuccessUrl();
    }

    private void validateConfig() {
        if (!Boolean.TRUE.equals(config.getEnabled())
                || StrUtil.isBlank(config.getComponentAppId())
                || StrUtil.isBlank(config.getComponentAppSecret())
                || StrUtil.isBlank(config.getComponentToken())
                || StrUtil.isBlank(config.getComponentAesKey())
                || StrUtil.isBlank(config.getAuthorizationRedirectUri())
                || StrUtil.isBlank(config.getFrontendSuccessUrl())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "微信开放平台第三方平台配置不完整");
        }
    }
}
