package com.kirandev.tweets.service.springai;

import com.openai.client.OpenAIClient;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringAIConfig {

    @Bean("ooenAIChatClient")
    ChatClient openAIChatClient(OpenAiChatModel openAiChatModel){
        return  ChatClient.builder(openAiChatModel).build();
    }

    @Bean("ollamaChatClient")
    ChatClient ollamaChatClient(OllamaChatModel ollamaChatModel){
        return  ChatClient.builder(ollamaChatModel).build();
    }
}
