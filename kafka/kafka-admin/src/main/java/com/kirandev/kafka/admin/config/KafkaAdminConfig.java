package com.kirandev.kafka.admin.config;

import com.kirandev.configdata.KafkaConfigData;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.retry.annotation.EnableRetry;

import java.util.Map;

@EnableRetry
@Configuration
public class KafkaAdminConfig {

    private final KafkaConfigData kafkaConfigData;
    public KafkaAdminConfig(KafkaConfigData kafkaConfigData) {
        this.kafkaConfigData = kafkaConfigData;
    }


    @Bean
    public AdminClient kafkaAdmin() {
        return AdminClient.create(
                Map.of(
                        CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, this.kafkaConfigData.getBootstrapServers()

                )
        );
    }
}
