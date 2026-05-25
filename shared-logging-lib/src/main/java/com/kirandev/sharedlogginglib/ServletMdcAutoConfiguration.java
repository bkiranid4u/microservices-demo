package com.kirandev.sharedlogginglib;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

import jakarta.servlet.Filter;

/**
 * Servlet-only logging beans. Kept in a separate auto-config class so that the
 * presence of {@code jakarta.servlet.Filter} guards the entire file: when this class
 * is absent (non-web apps like twitter-to-kafka-service), Spring's ASM-based
 * {@link ConditionalOnClass} evaluator never asks the JVM to load this class,
 * and {@link MdcFilter}'s inheritance chain (which transitively references
 * {@code jakarta.servlet.Filter}) is never resolved.
 *
 * <p>If these beans lived on {@link LoggingAutoConfiguration} instead, evaluating
 * a sibling {@link ConditionalOnMissingBean} on that class would force
 * {@code Class.getDeclaredMethods()} to introspect their return types, eagerly
 * loading {@code MdcFilter} and failing with {@code NoClassDefFoundError} in
 * non-web apps.
 */
@AutoConfiguration
@ConditionalOnClass(Filter.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class ServletMdcAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(MdcFilter.class)
    public MdcFilter mdcFilter() {
        return new MdcFilter();
    }

    @Bean
    @ConditionalOnMissingBean(name = "mdcFilterRegistration")
    public FilterRegistrationBean<MdcFilter> mdcFilterRegistration(MdcFilter mdcFilter) {
        FilterRegistrationBean<MdcFilter> registration = new FilterRegistrationBean<>(mdcFilter);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.addUrlPatterns("/*");
        return registration;
    }
}
