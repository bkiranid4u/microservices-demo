package com.kirandev;

import com.kirandev.configdata.TwitterToKafkaServiceConfigData;
import com.kirandev.init.StreamInitializer;
import com.kirandev.runner.StreamRunner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Arrays;

@SpringBootApplication(scanBasePackages = "com.kirandev")
public class TwitterToKafkaServiceApplication implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(TwitterToKafkaServiceApplication.class);


    private final StreamRunner streamRunner;
    private final StreamInitializer streamInitializer;

    public TwitterToKafkaServiceApplication(StreamRunner streamRunner, StreamInitializer streamInitializer) {
        this.streamRunner = streamRunner;
        this.streamInitializer = streamInitializer;
    }

    public static void main(String[] args) {
        SpringApplication.run(TwitterToKafkaServiceApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        logger.info("Starting Twitter To Kafka Service Application");
        streamInitializer.init();
        streamRunner.start();
    }
}

