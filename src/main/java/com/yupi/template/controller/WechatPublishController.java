package com.yupi.template.controller;

import com.yupi.template.common.BaseResponse;
import com.yupi.template.common.ResultUtils;
import com.yupi.template.model.dto.wechat.WechatPublishRequest;
import com.yupi.template.model.entity.User;
import com.yupi.template.model.vo.WechatPublishVO;
import com.yupi.template.service.UserService;
import com.yupi.template.service.WechatPublishService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/article/wechat")
@Slf4j
@Tag(name = "WechatPublishController", description = "微信公众号发布接口")
public class WechatPublishController {

    @Resource
    private WechatPublishService wechatPublishService;

    @Resource
    private UserService userService;

    @PostMapping("/draft/{taskId}")
    @Operation(summary = "保存到微信公众号草稿箱")
    public BaseResponse<WechatPublishVO> saveDraft(
            @PathVariable String taskId,
            @RequestBody(required = false) WechatPublishRequest request,
            HttpServletRequest httpServletRequest) {
        User loginUser = userService.getLoginUser(httpServletRequest);
        return ResultUtils.success(wechatPublishService.saveDraft(taskId, request, loginUser));
    }

    @PostMapping("/publish/{taskId}")
    @Operation(summary = "提交发布到微信公众号")
    public BaseResponse<WechatPublishVO> publish(
            @PathVariable String taskId,
            @RequestBody(required = false) WechatPublishRequest request,
            HttpServletRequest httpServletRequest) {
        User loginUser = userService.getLoginUser(httpServletRequest);
        return ResultUtils.success(wechatPublishService.publish(taskId, request, loginUser));
    }

    @GetMapping("/status/{taskId}")
    @Operation(summary = "查询微信公众号发布记录")
    public BaseResponse<WechatPublishVO> getStatus(
            @PathVariable String taskId,
            @RequestParam(required = false) Long wechatAccountId,
            HttpServletRequest httpServletRequest) {
        User loginUser = userService.getLoginUser(httpServletRequest);
        return ResultUtils.success(wechatPublishService.getLatestStatus(taskId, wechatAccountId, loginUser));
    }

    @GetMapping("/official-status/{publishId}")
    @Operation(summary = "查询微信官方发布状态")
    public BaseResponse<WechatPublishVO> getOfficialStatus(
            @PathVariable String publishId,
            HttpServletRequest httpServletRequest) {
        User loginUser = userService.getLoginUser(httpServletRequest);
        return ResultUtils.success(wechatPublishService.refreshOfficialStatus(publishId, loginUser));
    }
}
