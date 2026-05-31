package com.yupi.template.model.vo;

import com.yupi.template.model.entity.WechatPublishRecord;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class WechatPublishVO implements Serializable {

    private Long recordId;

    private String taskId;

    private String mode;

    private String status;

    private String mediaId;

    private String publishId;

    private String articleIdFromWechat;

    private String articleUrl;

    private String officialStatusCode;

    private String officialResponse;

    private String errorMessage;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    public static WechatPublishVO objToVo(WechatPublishRecord record) {
        if (record == null) {
            return null;
        }
        WechatPublishVO vo = new WechatPublishVO();
        BeanUtils.copyProperties(record, vo);
        vo.setRecordId(record.getId());
        return vo;
    }

    private static final long serialVersionUID = 1L;
}
