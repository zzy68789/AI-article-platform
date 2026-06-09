package com.yupi.template.controller;

import com.yupi.template.common.BaseResponse;
import com.yupi.template.common.ResultUtils;
import com.yupi.template.model.entity.User;
import com.yupi.template.model.vo.WechatAuthorizerAccountVO;
import com.yupi.template.model.vo.WechatAuthorizationUrlVO;
import com.yupi.template.service.UserService;
import com.yupi.template.service.WechatOpenPlatformService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/wechat")
@Tag(name = "WechatOpenPlatformController", description = "微信公众号第三方平台授权接口")
public class WechatOpenPlatformController {

    @Resource
    private WechatOpenPlatformService openPlatformService;

    @Resource
    private UserService userService;

    @PostMapping("/open-platform/ticket")
    @Operation(summary = "接收微信开放平台授权事件")
    public String receiveTicket(
            @RequestBody String encryptedXml,
            @RequestParam("msg_signature") String msgSignature,
            @RequestParam String timestamp,
            @RequestParam String nonce) {
        return openPlatformService.receiveTicket(encryptedXml, msgSignature, timestamp, nonce);
    }

    @GetMapping("/open-platform/auth-url")
    @Operation(summary = "生成微信公众号授权链接")
    public BaseResponse<WechatAuthorizationUrlVO> getAuthorizationUrl(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(openPlatformService.createAuthorizationUrl(loginUser));
    }

    @GetMapping("/open-platform/authorization/callback")
    @Operation(summary = "接收微信公众号授权回调")
    public void authorizationCallback(
            @RequestParam("auth_code") String authorizationCode,
            @RequestParam String state,
            HttpServletResponse response) throws IOException {
        openPlatformService.completeAuthorization(authorizationCode, state);
        String redirectUrl = UriComponentsBuilder
                .fromUriString(openPlatformService.getFrontendSuccessUrl())
                .queryParam("authorized", "1")
                .build()
                .toUriString();
        response.sendRedirect(redirectUrl);
    }

    @GetMapping("/accounts")
    @Operation(summary = "查询当前用户授权的公众号")
    public BaseResponse<List<WechatAuthorizerAccountVO>> listAccounts(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(openPlatformService.listAccounts(loginUser));
    }

    @PostMapping("/accounts/{accountId}/default")
    @Operation(summary = "设置默认公众号")
    public BaseResponse<Boolean> setDefault(@PathVariable Long accountId, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        openPlatformService.setDefault(accountId, loginUser);
        return ResultUtils.success(true);
    }

    @DeleteMapping("/accounts/{accountId}")
    @Operation(summary = "解除平台内的公众号绑定")
    public BaseResponse<Boolean> unbind(@PathVariable Long accountId, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        openPlatformService.unbind(accountId, loginUser);
        return ResultUtils.success(true);
    }
}
