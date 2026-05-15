package com.kirandev.transformer;

import com.kirandev.kafka.avro.model.TwitterAvroModel;
import org.springframework.stereotype.Component;
import twitter4j.Status;

@Component
public class TwitterMsgtoAvroSchemaTransformer {
    public TwitterAvroModel getTwitterAvroModelFromMsg(Status status) {

        return TwitterAvroModel
                .newBuilder()
                .setId(status.getId())
                .setUserId(status.getUser().getId())
                .setCreatedAt(status.getCreatedAt().getTime())
                .setText(status.getText())
                .build();


    }
}
