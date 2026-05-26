package com.kirandev.kafkatoelastic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.kirandev")
public class KafkaToElasticApplication {

    public static void main(String[] args) {
        SpringApplication.run(KafkaToElasticApplication.class, args);
    }

}
