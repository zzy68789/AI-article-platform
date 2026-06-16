package com.yupi.template.config;

import me.chanjar.weixin.open.api.WxOpenService;
import me.chanjar.weixin.open.api.impl.WxOpenInMemoryConfigStorage;
import me.chanjar.weixin.open.api.impl.WxOpenServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WechatOpenPlatformConfiguration {

    @Bean
    public WxOpenService wxOpenService(WechatOpenPlatformConfig config) {
        WxOpenInMemoryConfigStorage storage = new WxOpenInMemoryConfigStorage();
        storage.setWxOpenInfo(
                config.getComponentAppId(),
                config.getComponentAppSecret(),
                config.getComponentToken(),
                config.getComponentAesKey()
        );
        WxOpenService service = new WxOpenServiceImpl();
        service.setWxOpenConfigStorage(storage);
        return service;
    }
}
