package com.kirandev.tweets.init.impl;

import com.kirandev.tweets.init.StreamInitializer;
import org.springframework.stereotype.Component;

@Component
public class AIStreamInitializer implements StreamInitializer {

    @Override
    public boolean init() {
        return true;
    }
}
