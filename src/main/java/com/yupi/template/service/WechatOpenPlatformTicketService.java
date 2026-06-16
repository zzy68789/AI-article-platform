package com.yupi.template.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.yupi.template.mapper.WechatOpenPlatformTicketMapper;
import com.yupi.template.model.entity.WechatOpenPlatformTicket;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class WechatOpenPlatformTicketService
        extends ServiceImpl<WechatOpenPlatformTicketMapper, WechatOpenPlatformTicket> {

    private final WechatCredentialCipher credentialCipher;

    public WechatOpenPlatformTicketService(WechatCredentialCipher credentialCipher) {
        this.credentialCipher = credentialCipher;
    }

    public void saveTicket(String componentAppid, String ticket) {
        WechatOpenPlatformTicket record = this.getOne(QueryWrapper.create()
                .eq("componentAppid", componentAppid));
        if (record == null) {
            record = WechatOpenPlatformTicket.builder()
                    .componentAppid(componentAppid)
                    .build();
        }
        record.setComponentVerifyTicketEncrypted(credentialCipher.encrypt(ticket));
        record.setUpdateTime(LocalDateTime.now());
        if (record.getId() == null) {
            this.save(record);
        } else {
            this.updateById(record);
        }
    }

    public String getTicket(String componentAppid) {
        WechatOpenPlatformTicket record = this.getOne(QueryWrapper.create()
                .eq("componentAppid", componentAppid));
        return record == null ? null : credentialCipher.decrypt(record.getComponentVerifyTicketEncrypted());
    }
}
