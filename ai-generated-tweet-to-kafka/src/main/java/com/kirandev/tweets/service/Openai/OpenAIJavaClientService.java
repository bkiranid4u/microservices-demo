package com.kirandev.tweets.service.Openai;

import com.kirandev.tweets.config.AIGeneratedTweetToKafkaConfigData;
import com.kirandev.tweets.exception.AIGeneratedTweetKafkaException;
import com.kirandev.tweets.service.AIService;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatModel;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

@ConditionalOnProperty(name="ai-generated-tweet-to-kafka.ai-service", havingValue = "OpenAI-JavaClient")
@Service
@Slf4j
public class OpenAIJavaClientService implements AIService {

    private final AIGeneratedTweetToKafkaConfigData configData;

    public OpenAIJavaClientService(AIGeneratedTweetToKafkaConfigData configData) {
        this.configData = configData;
    }

    @Override
    public String generateTweet() throws AIGeneratedTweetKafkaException {
        log.info("Generating tweet via OpenAI JAVA SDK Client");
        String prompt = configData.getPrompt().replace(configData.getKeywordsPlaceholder(),
                String.join(",", configData.getStreamingDataKeywords()));

        return getPromptResponse(prompt);
    }

    private String getPromptResponse(String prompt){
        OpenAIClient openAIClient = OpenAIOkHttpClient.fromEnv();

        ChatCompletionCreateParams.Builder createParams = ChatCompletionCreateParams.builder()
                .model(ChatModel.of(configData.getOpenAI().getModel()))
                .addDeveloperMessage("You're helping to create tweet contnt based on the given format and keywords")
                .maxCompletionTokens(configData.getOpenAI().getMaxCompletionTokens())
                .temperature(configData.getOpenAI().getTemperature())
                .addUserMessage(prompt);
        List<ChatCompletionMessage> messages = openAIClient.chat()
                .completions()
                .create(createParams.build())
                .choices()
                .stream()
                .map(ChatCompletion.Choice::message)
                .toList();

        return messages.getFirst().content().orElse("EmptyResponse");
    }
}
