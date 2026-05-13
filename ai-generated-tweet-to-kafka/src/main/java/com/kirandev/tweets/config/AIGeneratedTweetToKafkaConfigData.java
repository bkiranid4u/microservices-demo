package com.kirandev.tweets.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "ai-generated-tweet-to-kafka")
@Data
public class AIGeneratedTweetToKafkaConfigData {
    List<String> streamingDataKeywords;
    private Duration schedulerDuration;
    private String prompt;
    private String keywordsPlaceholder;
    private OpenAI openAI;

    private GoogleGenAI googleGenAI;

    @Data
    public static class OpenAI {
        private String url;
        private String apiKey;
        private String contentType;
        private String model;
        private Integer maxCompletionTokens;
        private String prompt;
        private Double temperature;
        private List<Message> messages;
    }

    @Data
    public static  class GoogleGenAI {
        private String projectId;
        private String location;
        private String modelName;
        private Integer maxOutputTokens;
        private Float temperature;
        private Integer candidateCount;
    }

    @Data
    public static class Message {
        private String role;
        private List<Content> content;
    }
    @Data
    public static class Content {
        private String type;
        private String text;
    }
}
