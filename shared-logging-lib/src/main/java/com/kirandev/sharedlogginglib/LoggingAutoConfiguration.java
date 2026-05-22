package com.kirandev.sharedlogginglib;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;

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

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnMissingBean(MdcFilter.class)
    public MdcFilter mdcFilter() {
        return new MdcFilter();
    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnMissingBean(name = "mdcFilterRegistration")
    public FilterRegistrationBean<MdcFilter> mdcFilterRegistration(MdcFilter mdcFilter) {
        FilterRegistrationBean<MdcFilter> registration = new FilterRegistrationBean<>(mdcFilter);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.addUrlPatterns("/*");
        return registration;
    }
}
