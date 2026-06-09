package com.yupi.template.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WechatCredential {

    private Long wechatAccountId;

    private String authorizerAppid;

    private String accessToken;
}
