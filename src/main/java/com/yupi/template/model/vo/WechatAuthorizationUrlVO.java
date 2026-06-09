package com.yupi.template.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WechatAuthorizationUrlVO implements Serializable {

    private String authorizationUrl;

    private static final long serialVersionUID = 1L;
}
