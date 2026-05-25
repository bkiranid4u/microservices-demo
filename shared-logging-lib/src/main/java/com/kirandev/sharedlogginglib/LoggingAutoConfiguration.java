package com.kirandev.sharedlogginglib;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

/**
 * Core MDC propagation beans usable in both web and non-web Spring Boot apps.
 *
 * <p>Web-only beans (servlet {@link jakarta.servlet.Filter} registration) live
 * in {@link ServletMdcAutoConfiguration} — kept separate so its servlet-API
 * references never trigger classloading in non-web apps such as Kafka consumers
 * or batch jobs.
 */
@AutoConfiguration
@EnableConfigurationProperties(LoggingProperties.class)
@Import(AsyncMdcConfiguration.class)
public class LoggingAutoConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "kirandev.logging", name = "initialize-service-context",
            havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean
    public ServiceMdcInitializer serviceMdcInitializer(Environment environment) {
        return new ServiceMdcInitializer(environment);
    }

    @Bean
    @ConditionalOnMissingBean
    public MdcTaskDecorator mdcTaskDecorator() {
        return new MdcTaskDecorator();
    }
}
