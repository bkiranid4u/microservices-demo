package com.kirandev.tweets.service;

import com.kirandev.tweets.exception.AIGeneratedTweetKafkaException;

public interface AIService {

    String generateTweet() throws AIGeneratedTweetKafkaException;
}
