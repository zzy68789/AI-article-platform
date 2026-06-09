package com.yupi.template.service;

import com.yupi.template.config.WechatOpenPlatformConfig;
import com.yupi.template.exception.BusinessException;
import com.yupi.template.model.entity.User;
import com.yupi.template.model.entity.WechatAuthorizerAccount;
import me.chanjar.weixin.open.api.WxOpenComponentService;
import me.chanjar.weixin.open.api.WxOpenService;
import me.chanjar.weixin.open.api.WxOpenConfigStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WechatCredentialServiceTest {

    private WechatCredentialService credentialService;
    private WechatAuthorizerAccountService accountService;
    private WxOpenComponentService componentService;

    @BeforeEach
    void setUp() throws Exception {
        accountService = mock(WechatAuthorizerAccountService.class);
        WxOpenService wxOpenService = mock(WxOpenService.class);
        componentService = mock(WxOpenComponentService.class);
        WxOpenConfigStorage configStorage = mock(WxOpenConfigStorage.class);
        when(wxOpenService.getWxOpenComponentService()).thenReturn(componentService);
        when(wxOpenService.getWxOpenConfigStorage()).thenReturn(configStorage);

        credentialService = new WechatCredentialService();
        ReflectionTestUtils.setField(credentialService, "accountService", accountService);
        ReflectionTestUtils.setField(credentialService, "wxOpenService", wxOpenService);
        ReflectionTestUtils.setField(credentialService, "wechatApiClient", mock(WechatApiClient.class));
        WechatOpenPlatformConfig openPlatformConfig = new WechatOpenPlatformConfig();
        openPlatformConfig.setEnabled(true);
        openPlatformConfig.setComponentAppId("wx-component");
        openPlatformConfig.setComponentAppSecret("component-secret");
        ReflectionTestUtils.setField(credentialService, "openPlatformConfig", openPlatformConfig);
    }

    @Test
    void resolvesAccessTokenOnlyForAccountOwner() throws Exception {
        User owner = User.builder().id(7L).userRole("user").build();
        WechatAuthorizerAccount account = WechatAuthorizerAccount.builder()
                .id(11L)
                .ownerUserId(7L)
                .authorizerAppid("wx-owner")
                .authStatus("AUTHORIZED")
                .build();
        when(accountService.getById(11L)).thenReturn(account);
        when(componentService.getAuthorizerAccessToken("wx-owner", false)).thenReturn("owner-token");

        WechatCredential credential = credentialService.resolve(11L, owner);

        assertThat(credential.getAccessToken()).isEqualTo("owner-token");
        assertThat(credential.getAuthorizerAppid()).isEqualTo("wx-owner");
    }

    @Test
    void rejectsUsingAnotherUsersAccount() {
        User user = User.builder().id(8L).userRole("user").build();
        when(accountService.getById(11L)).thenReturn(WechatAuthorizerAccount.builder()
                .id(11L)
                .ownerUserId(7L)
                .authStatus("AUTHORIZED")
                .build());

        assertThatThrownBy(() -> credentialService.resolve(11L, user))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void rejectsPlatformDefaultAccountForNormalUsers() {
        User user = User.builder().id(8L).userRole("user").build();

        assertThatThrownBy(() -> credentialService.resolve(null, user))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("授权");
    }
}
