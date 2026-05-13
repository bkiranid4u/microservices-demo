package com.kirandev.tweets.service.googlegenai;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.kirandev.tweets.config.AIGeneratedTweetToKafkaConfigData;
import com.kirandev.tweets.exception.AIGeneratedTweetKafkaException;
import com.kirandev.tweets.service.AIService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;

@ConditionalOnProperty(name="ai-generated-tweet-to-kafka.ai-service", havingValue = "Google-GenAI")
@Service
@Slf4j
public class GoogleGenAIService implements AIService {

    private final AIGeneratedTweetToKafkaConfigData configData;

    private final Client googleGenAIClient;

    public GoogleGenAIService(AIGeneratedTweetToKafkaConfigData configData) {
        this.configData = configData;
        this.googleGenAIClient = Client.
                builder()
                .project(configData.getGoogleGenAI().getProjectId())
                .location(configData.getGoogleGenAI().getLocation())
                .vertexAI(true)
                .build();
    }
    @PreDestroy
    public void close() throws AIGeneratedTweetKafkaException {
        if(this.googleGenAIClient != null) {
            this.googleGenAIClient.close();
        }
    }
    @Override
    public String generateTweet() throws AIGeneratedTweetKafkaException {
        log.info("Start to generate tweet - Google Gen AI");

        String prompt = configData.getPrompt().replace(configData.getKeywordsPlaceholder(),
                String.join(",", configData.getStreamingDataKeywords()));

        return getPromptResponse(prompt);
    }

    private String getPromptResponse(String prompt){
        GenerateContentConfig config = GenerateContentConfig.builder()
                .maxOutputTokens(configData.getGoogleGenAI().getMaxOutputTokens())
                .temperature(configData.getGoogleGenAI().getTemperature())
                .candidateCount(configData.getGoogleGenAI().getCandidateCount())
                .build();

        String modelname = configData.getGoogleGenAI().getModelName();
        GenerateContentResponse response = this.googleGenAIClient.models.generateContent(modelname,prompt,config);
        return response.text();
    }
}
