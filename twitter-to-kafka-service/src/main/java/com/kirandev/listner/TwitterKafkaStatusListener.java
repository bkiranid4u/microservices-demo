package com.kirandev.listner;

import com.kirandev.configdata.KafkaConfigData;
import com.kirandev.kafka.avro.model.TwitterAvroModel;
import com.kirandev.transformer.TwitterMsgtoAvroSchemaTransformer;
import com.kirandev.kafka.producer.service.KafkaProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import twitter4j.Status;
import twitter4j.StatusAdapter;

@Component
public class TwitterKafkaStatusListener extends StatusAdapter {

    private static final Logger logger = LoggerFactory.getLogger(TwitterKafkaStatusListener.class);

    private final KafkaConfigData kafkaConfigData;

    private final KafkaProducer<Long, TwitterAvroModel> kafkaProducer;

    private final TwitterMsgtoAvroSchemaTransformer twitterMsgtoAvroSchemaTransformer;

    public TwitterKafkaStatusListener(KafkaConfigData kafkaConfigData, KafkaProducer<Long, TwitterAvroModel> kafkaProducer, TwitterMsgtoAvroSchemaTransformer twitterMsgtoAvroSchemaTransformer) {
        this.kafkaConfigData = kafkaConfigData;
        this.kafkaProducer = kafkaProducer;
        this.twitterMsgtoAvroSchemaTransformer = twitterMsgtoAvroSchemaTransformer;
    }

    @Override
    public void onStatus(Status status) {

        logger.info("Twitter status with text {} sending to kafka topic {}", status.getText(), kafkaConfigData.getTopicName());
        TwitterAvroModel twitterAvroModel = twitterMsgtoAvroSchemaTransformer.getTwitterAvroModelFromMsg(status);
        kafkaProducer.send(kafkaConfigData.getTopicName(), twitterAvroModel.getUserId(), twitterAvroModel);
    }
}
