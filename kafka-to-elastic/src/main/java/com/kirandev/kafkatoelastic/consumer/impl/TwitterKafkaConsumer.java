package com.kirandev.kafkatoelastic.consumer.impl;

import com.kirandev.configdata.KafkaConfigData;
import com.kirandev.kafka.admin.clients.KafkaAdminClient;
import com.kirandev.kafka.avro.model.TwitterAvroModel;
import com.kirandev.kafkatoelastic.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;

import java.util.List;

public class TwitterKafkaConsumer implements KafkaConsumer<Long, TwitterAvroModel> {

    private static final Logger logger = LoggerFactory.getLogger(TwitterKafkaConsumer.class);

    private final KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

    private final KafkaAdminClient kafkaAdminClient;

    private final KafkaConfigData kafkaConfigData;

    public TwitterKafkaConsumer(KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry, KafkaAdminClient kafkaAdminClient, KafkaConfigData kafkaConfigData) {
        this.kafkaListenerEndpointRegistry = kafkaListenerEndpointRegistry;
        this.kafkaAdminClient = kafkaAdminClient;
        this.kafkaConfigData = kafkaConfigData;
    }

    @Override
    @KafkaListener(id = "twitterTopicListner", topics = "${kafka-config.topic-name}")
    public void receive(@Payload List<TwitterAvroModel> messages,
                        @Header List<Integer> keys,
                        @Header List<Integer> partitions,
                        @Header List<Long> offsets) {

        logger.info("Received {} messages with keys: {} and offsets {}," +
                " sending it to elastic: Thread ID", messages.size(), keys.toString(), offsets.toString(),
                Thread.currentThread().getId());

    }
}
