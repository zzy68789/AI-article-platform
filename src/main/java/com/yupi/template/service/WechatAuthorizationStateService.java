package com.yupi.template.service;

import cn.hutool.core.util.IdUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.yupi.template.exception.BusinessException;
import com.yupi.template.exception.ErrorCode;
import com.yupi.template.mapper.WechatAuthorizationStateMapper;
import com.yupi.template.model.entity.WechatAuthorizationState;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class WechatAuthorizationStateService
        extends ServiceImpl<WechatAuthorizationStateMapper, WechatAuthorizationState> {

    public String createState(Long userId) {
        String state = IdUtil.simpleUUID();
        this.save(WechatAuthorizationState.builder()
                .state(state)
                .userId(userId)
                .expireTime(LocalDateTime.now().plusMinutes(10))
                .used(0)
                .createTime(LocalDateTime.now())
                .build());
        return state;
    }

    public Long consumeState(String state) {
        WechatAuthorizationState authorizationState = this.getOne(QueryWrapper.create()
                .eq("state", state)
                .eq("used", 0));
        if (authorizationState == null
                || authorizationState.getExpireTime() == null
                || LocalDateTime.now().isAfter(authorizationState.getExpireTime())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "微信公众号授权状态已失效，请重新发起授权");
        }
        authorizationState.setUsed(1);
        this.updateById(authorizationState);
        return authorizationState.getUserId();
    }
}
