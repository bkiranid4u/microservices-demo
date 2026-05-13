package com.kirandev.tweets.exception;

public class AIGeneratedTweetKafkaException extends RuntimeException {
    public AIGeneratedTweetKafkaException() {
    }
    public AIGeneratedTweetKafkaException(String message) {
        super(message);
    }

    public AIGeneratedTweetKafkaException(String message, Throwable cause) {
        super(message, cause);
    }
}
