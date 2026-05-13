package com.kirandev.tweets.service.springai.model;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.ZonedDateTime;

public record TweetResponse(
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "EEE MMM dd HH:mm:ss zzz yyyy")
        ZonedDateTime createdate,
        Long id,
        String  text,
        User user
) {
}
