package com.yupi.template.service;

import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.yupi.template.exception.BusinessException;
import com.yupi.template.exception.ErrorCode;
import com.yupi.template.mapper.WechatAuthorizerAccountMapper;
import com.yupi.template.model.entity.User;
import com.yupi.template.model.entity.WechatAuthorizerAccount;
import com.yupi.template.model.enums.WechatAuthorizationStatusEnum;
import com.yupi.template.model.vo.WechatAuthorizerAccountVO;
import com.yupi.template.utils.GsonUtils;
import me.chanjar.weixin.open.bean.auth.WxOpenAuthorizationInfo;
import me.chanjar.weixin.open.bean.auth.WxOpenAuthorizerInfo;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class WechatAuthorizerAccountService
        extends ServiceImpl<WechatAuthorizerAccountMapper, WechatAuthorizerAccount> {

    private final WechatCredentialCipher credentialCipher;

    public WechatAuthorizerAccountService(WechatCredentialCipher credentialCipher) {
        this.credentialCipher = credentialCipher;
    }

    public WechatAuthorizerAccount saveAuthorization(
            Long userId,
            WxOpenAuthorizationInfo authorizationInfo,
            WxOpenAuthorizerInfo authorizerInfo) {
        WechatAuthorizerAccount account = this.getOne(QueryWrapper.create()
                .eq("authorizerAppid", authorizationInfo.getAuthorizerAppid())
                .eq("isDelete", 0));
        if (account != null
                && !Objects.equals(account.getOwnerUserId(), userId)
                && WechatAuthorizationStatusEnum.AUTHORIZED.getValue().equals(account.getAuthStatus())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "该公众号已绑定其他平台用户");
        }
        boolean isNew = account == null;
        if (isNew) {
            account = new WechatAuthorizerAccount();
            account.setOwnerUserId(userId);
            account.setAuthorizerAppid(authorizationInfo.getAuthorizerAppid());
            account.setIsDefault(hasAuthorizedAccount(userId) ? 0 : 1);
            account.setCreateTime(LocalDateTime.now());
            account.setIsDelete(0);
        } else {
            account.setOwnerUserId(userId);
        }
        if (!hasAuthorizedAccount(userId)) {
            account.setIsDefault(1);
        }
        account.setNickName(authorizerInfo.getNickName());
        account.setHeadImg(authorizerInfo.getHeadImg());
        account.setPrincipalName(authorizerInfo.getPrincipalName());
        account.setServiceTypeInfo(authorizerInfo.getServiceTypeInfo());
        account.setVerifyTypeInfo(authorizerInfo.getVerifyTypeInfo());
        account.setFuncInfo(GsonUtils.toJson(authorizationInfo.getFuncInfo()));
        account.setAuthorizerRefreshTokenEncrypted(
                credentialCipher.encrypt(authorizationInfo.getAuthorizerRefreshToken()));
        account.setAuthorizerAccessTokenEncrypted(
                credentialCipher.encrypt(authorizationInfo.getAuthorizerAccessToken()));
        account.setAccessTokenExpireTime(LocalDateTime.now()
                .plusSeconds(Math.max(60, authorizationInfo.getExpiresIn() - 300L)));
        account.setAuthStatus(WechatAuthorizationStatusEnum.AUTHORIZED.getValue());
        account.setUpdateTime(LocalDateTime.now());
        if (isNew) {
            this.save(account);
        } else {
            this.updateById(account);
        }
        return account;
    }

    public List<WechatAuthorizerAccountVO> listForUser(Long userId) {
        return this.list(QueryWrapper.create()
                        .eq("ownerUserId", userId)
                        .eq("isDelete", 0)
                        .orderBy("isDefault", false)
                        .orderBy("updateTime", false))
                .stream()
                .map(WechatAuthorizerAccountVO::objToVo)
                .toList();
    }

    public List<WechatAuthorizerAccount> listAuthorizedAccounts() {
        return this.list(QueryWrapper.create()
                .eq("authStatus", WechatAuthorizationStatusEnum.AUTHORIZED.getValue())
                .eq("isDelete", 0));
    }

    public void setDefault(Long accountId, User loginUser) {
        WechatAuthorizerAccount account = getOwnedAccount(accountId, loginUser.getId());
        for (WechatAuthorizerAccount item : this.list(QueryWrapper.create()
                .eq("ownerUserId", loginUser.getId())
                .eq("isDelete", 0))) {
            item.setIsDefault(Objects.equals(item.getId(), account.getId()) ? 1 : 0);
            item.setUpdateTime(LocalDateTime.now());
            this.updateById(item);
        }
    }

    public void unbind(Long accountId, User loginUser) {
        WechatAuthorizerAccount account = getOwnedAccount(accountId, loginUser.getId());
        account.setAuthStatus(WechatAuthorizationStatusEnum.UNAUTHORIZED.getValue());
        account.setIsDefault(0);
        account.setUpdateTime(LocalDateTime.now());
        this.updateById(account);
    }

    public void markUnauthorized(String authorizerAppid) {
        WechatAuthorizerAccount account = this.getOne(QueryWrapper.create()
                .eq("authorizerAppid", authorizerAppid)
                .eq("isDelete", 0));
        if (account != null) {
            account.setAuthStatus(WechatAuthorizationStatusEnum.UNAUTHORIZED.getValue());
            account.setIsDefault(0);
            account.setUpdateTime(LocalDateTime.now());
            this.updateById(account);
        }
    }

    public String getDecryptedRefreshToken(WechatAuthorizerAccount account) {
        return credentialCipher.decrypt(account.getAuthorizerRefreshTokenEncrypted());
    }

    public void updateAccessToken(WechatAuthorizerAccount account, String accessToken) {
        if (StrUtil.isBlank(accessToken)) {
            return;
        }
        account.setAuthorizerAccessTokenEncrypted(credentialCipher.encrypt(accessToken));
        account.setAccessTokenExpireTime(LocalDateTime.now().plusSeconds(6900));
        account.setUpdateTime(LocalDateTime.now());
        this.updateById(account);
    }

    private WechatAuthorizerAccount getOwnedAccount(Long accountId, Long userId) {
        WechatAuthorizerAccount account = this.getById(accountId);
        if (account == null || !Objects.equals(account.getOwnerUserId(), userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        return account;
    }

    private boolean hasAuthorizedAccount(Long userId) {
        return this.count(QueryWrapper.create()
                .eq("ownerUserId", userId)
                .eq("authStatus", WechatAuthorizationStatusEnum.AUTHORIZED.getValue())
                .eq("isDelete", 0)) > 0;
    }
}
