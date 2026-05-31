package com.yupi.template.config;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class AiChatModelConfig {

    @Bean
    @Primary
    public ChatModel primaryChatModel(DeepSeekChatModel deepSeekChatModel) {
        return deepSeekChatModel;
    }
}
