package com.kirandev.common.config;


import com.kirandev.configdata.RetryConfigData;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;


@Configuration
public class RetryConfig {

    private  RetryConfigData retryConfigData;

    public RetryConfig(final RetryConfigData retryConfigData) {
        this.retryConfigData = retryConfigData;
    }

    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        /*Exponential Backoff Policy*/
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(retryConfigData.getInitialInterval().toMillis());
        backOffPolicy.setMultiplier(retryConfigData.getMultiplier());
        backOffPolicy.setMaxInterval(retryConfigData.getMaxInterval().toMillis());

        retryTemplate.setBackOffPolicy(backOffPolicy);

        /*Simple Retry Policy Configuration*/
        SimpleRetryPolicy simpleRetryPolicy = new SimpleRetryPolicy();
        simpleRetryPolicy.setMaxAttempts(retryConfigData.getMaxAttempts());
        retryTemplate.setRetryPolicy(simpleRetryPolicy);

        return retryTemplate;
    }
}