package com.yupi.template.model.vo;

import com.yupi.template.model.entity.WechatAuthorizerAccount;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class WechatAuthorizerAccountVO implements Serializable {

    private Long id;

    private String authorizerAppid;

    private String nickName;

    private String headImg;

    private String principalName;

    private Integer serviceTypeInfo;

    private Integer verifyTypeInfo;

    private String authStatus;

    private Integer isDefault;

    private LocalDateTime updateTime;

    public static WechatAuthorizerAccountVO objToVo(WechatAuthorizerAccount account) {
        if (account == null) {
            return null;
        }
        WechatAuthorizerAccountVO vo = new WechatAuthorizerAccountVO();
        BeanUtils.copyProperties(account, vo);
        return vo;
    }

    private static final long serialVersionUID = 1L;
}
