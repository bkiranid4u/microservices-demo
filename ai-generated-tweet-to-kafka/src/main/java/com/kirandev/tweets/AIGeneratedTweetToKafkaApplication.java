package com.kirandev.tweets;

import com.kirandev.tweets.config.AIGeneratedTweetToKafkaConfigData;
import com.kirandev.tweets.init.StreamInitializer;
import com.kirandev.tweets.runner.AIStreamRunner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@Slf4j
@EnableScheduling
public class AIGeneratedTweetToKafkaApplication implements CommandLineRunner {

    private final AIGeneratedTweetToKafkaConfigData configData;

    private final StreamInitializer streamInitializer;

    private final AIStreamRunner aimStreamRunner;
    private final TaskScheduler taskScheduler;


    public AIGeneratedTweetToKafkaApplication(AIGeneratedTweetToKafkaConfigData configData,
                                              StreamInitializer streamInitializer,
                                              AIStreamRunner aimStreamRunner,
                                              TaskScheduler taskScheduler) {
        this.configData = configData;
        this.streamInitializer = streamInitializer;
        this.aimStreamRunner = aimStreamRunner;
        this.taskScheduler = taskScheduler;
    }

    public static void main(String[] args) {
        SpringApplication.run(AIGeneratedTweetToKafkaApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("Application started...");
        boolean initResult = streamInitializer.init();
        if(initResult) {
            log.info("Stream initialized...");
            taskScheduler.scheduleAtFixedRate(aimStreamRunner,configData.getSchedulerDuration());
            log.info("Keyword: {}", configData.getStreamingDataKeywords());
        }else {
            log.error("Error while initializing stream");
        }

    }
}
