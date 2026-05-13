package com.kirandev.tweets.service.springai;

import com.kirandev.tweets.config.AIGeneratedTweetToKafkaConfigData;
import com.kirandev.tweets.exception.AIGeneratedTweetKafkaException;
import com.kirandev.tweets.service.AIService;
import com.kirandev.tweets.service.springai.model.TweetResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@ConditionalOnProperty(name = "ai-generated-tweet-to-kafka.ai-service", havingValue = "SpringAI-OpenAI")
@Slf4j
public class SpringAIService implements AIService {

    private final ChatClient chatClient;
    private final AIGeneratedTweetToKafkaConfigData configData;

    @Value("classpath:/prompts/tweet-prompt.st")
    private Resource tweetPrompt;

    public SpringAIService(ChatClient chatClient, AIGeneratedTweetToKafkaConfigData configData) {
        this.chatClient = chatClient;
        this.configData = configData;
    }

    @Override
    public String generateTweet() throws AIGeneratedTweetKafkaException {
        BeanOutputConverter<TweetResponse> converter = new BeanOutputConverter<>(TweetResponse.class);
        log.info("Converter Format: {}", converter);
        PromptTemplate promptTemplate = new PromptTemplate(tweetPrompt);
        Prompt prompt = promptTemplate.create(
                Map.of(
                        configData.getKeywordsPlaceholder().replace("{", "").replace("}",""),
                        String.join(",", configData.getStreamingDataKeywords()),
                        "format",converter.getFormat())
        );
        return chatClient.prompt(prompt)
                .call()
                .content();
    }
}
