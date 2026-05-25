package com.kirandev.common.config;


import com.kirandev.configdata.RetryConfigData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import jakarta.annotation.PostConstruct;


@Configuration
public class RetryConfig {

    private static final Logger log = LoggerFactory.getLogger(RetryConfig.class);

    private final RetryConfigData retryConfigData;

    public RetryConfig(final RetryConfigData retryConfigData) {
        this.retryConfigData = retryConfigData;
    }

    /**
     * Startup sanity-log so you can confirm the {@code retry-config.*} block was loaded
     * from Spring Cloud Config Server. Look for this line in stdout right after context
     * refresh — if any field shows {@code null}, the binding failed.
     */
    @PostConstruct
    void logLoadedProperties() {
        log.info("retry-config loaded: initialInterval={}, maxInterval={}, multiplier={}, maxAttempts={}, sleepTime={}",
                retryConfigData.getInitialInterval(),
                retryConfigData.getMaxInterval(),
                retryConfigData.getMultiplier(),
                retryConfigData.getMaxAttempts(),
                retryConfigData.getSleepTime());
    }

    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(retryConfigData.getInitialInterval().toMillis());
        backOffPolicy.setMultiplier(retryConfigData.getMultiplier());
        backOffPolicy.setMaxInterval(retryConfigData.getMaxInterval().toMillis());

        retryTemplate.setBackOffPolicy(backOffPolicy);

        SimpleRetryPolicy simpleRetryPolicy = new SimpleRetryPolicy();
        simpleRetryPolicy.setMaxAttempts(retryConfigData.getMaxAttempts());
        retryTemplate.setRetryPolicy(simpleRetryPolicy);

        return retryTemplate;
    }
}
