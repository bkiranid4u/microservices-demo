package com.kirandev.tweets.runner;

import com.kirandev.tweets.service.AIService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AIStreamRunner implements Runnable{

    AIService aiService;
    public AIStreamRunner(AIService aiService) {
        this.aiService = aiService;
    }

    @Override
    public void run() {
        String generatedTweet = aiService.generateTweet();
        log.info("Generated tweet: {}", generatedTweet);
    }
}
