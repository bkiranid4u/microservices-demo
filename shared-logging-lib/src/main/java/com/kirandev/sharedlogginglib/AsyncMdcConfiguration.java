package com.kirandev.sharedlogginglib;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Propagates MDC to {@link TaskExecutor} threads (virtual or platform).
 */
@Configuration
@ConditionalOnClass(TaskExecutor.class)
public class AsyncMdcConfiguration {

    @Bean
    static BeanPostProcessor mdcTaskExecutorPostProcessor(MdcTaskDecorator decorator) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(Object bean, String beanName) {
                if (bean instanceof ThreadPoolTaskExecutor executor) {
                    executor.setTaskDecorator(decorator);
                }
                return bean;
            }
        };
    }
}
