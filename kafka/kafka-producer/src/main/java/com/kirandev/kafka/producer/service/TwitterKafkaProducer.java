package com.kirandev.kafka.producer.service;

import com.kirandev.kafka.avro.model.TwitterAvroModel;
import jakarta.annotation.PreDestroy;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class TwitterKafkaProducer implements KafkaProducer<Long, TwitterAvroModel> {

    private static final Logger logger = LoggerFactory.getLogger(TwitterKafkaProducer.class);

    private final KafkaTemplate kafkaTemplate;

    public TwitterKafkaProducer(KafkaTemplate kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @PreDestroy
    public void close() {
        if (kafkaTemplate != null) {
            logger.info("Closing kafka producer!");
            kafkaTemplate.destroy();
        }
    }

    @Override
    public void send(String topic, Long key, TwitterAvroModel value) {
        logger.info("Sending Twitter Message={} to topic={}", value, topic);

        CompletableFuture<SendResult<Long, TwitterAvroModel>> kafkaResultFuture = kafkaTemplate.send(topic, key, value);

        kafkaResultFuture.thenAccept(sendResult -> {
                    RecordMetadata metadata = sendResult.getRecordMetadata();
                    logger.debug("Received new metadata. Topic: {}; Partition {}; Offset {}; Timestamp {}, at time {}",
                            metadata.topic(),
                            metadata.partition(),
                            metadata.offset(),
                            metadata.timestamp(),
                            System.nanoTime());
                })
                .exceptionallyCompose(throwable -> {
                    logger.error("Error while sending message {} to topic {}", value, topic, throwable);
                    return CompletableFuture.completedFuture(null);
                });


    }
}
