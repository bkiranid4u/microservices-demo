package com.kirandev.sharedlogginglib;

import com.kirandev.sharedlogginglib.kafka.KafkaMdcProducerInterceptor;
import com.kirandev.sharedlogginglib.kafka.KafkaMdcRecordInterceptor;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;

import java.util.Map;

/**
 * Auto-configures Kafka MDC trace-propagation when Spring Kafka is on the classpath.
 *
 * <p>Two BeanPostProcessors are registered:
 * <ol>
 *   <li><b>Producer</b>: appends {@link KafkaMdcProducerInterceptor} to every
 *       {@link DefaultKafkaProducerFactory} via {@code updateConfigs()}, injecting
 *       W3C {@code traceparent} and B3 headers into outgoing records.</li>
 *   <li><b>Consumer</b>: wires {@link KafkaMdcRecordInterceptor} into every
 *       {@link ConcurrentKafkaListenerContainerFactory} that has no interceptor set,
 *       extracting trace headers into MDC before each record is processed.</li>
 * </ol>
 *
 * <p>Disable entirely with {@code kirandev.logging.kafka.enabled=false}.
 */
@AutoConfiguration
@ConditionalOnClass(DefaultKafkaProducerFactory.class)
@ConditionalOnProperty(prefix = "kirandev.logging.kafka", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class KafkaLoggingAutoConfiguration {

    /**
     * Appends {@link KafkaMdcProducerInterceptor} to every {@link DefaultKafkaProducerFactory}
     * after it is initialized. Works with both Spring Boot auto-config and custom factory beans.
     */
    @Bean
    @SuppressWarnings("rawtypes")
    static BeanPostProcessor kafkaProducerMdcConfigurer() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (!(bean instanceof DefaultKafkaProducerFactory<?, ?> factory)) {
                    return bean;
                }
                String interceptorFqn = KafkaMdcProducerInterceptor.class.getName();
                Object existing = factory.getConfigurationProperties()
                        .get(ProducerConfig.INTERCEPTOR_CLASSES_CONFIG);
                String current = existing instanceof String s ? s
                        : (existing != null ? existing.toString() : "");
                if (!current.contains(interceptorFqn)) {
                    String updated = current.isBlank() ? interceptorFqn : current + "," + interceptorFqn;
                    factory.updateConfigs(Map.of(ProducerConfig.INTERCEPTOR_CLASSES_CONFIG, updated));
                }
                return bean;
            }
        };
    }

    /**
     * Wires {@link KafkaMdcRecordInterceptor} into every
     * {@link ConcurrentKafkaListenerContainerFactory} that does not already have an interceptor.
     * Override the bean or set {@code kirandev.logging.kafka.enabled=false} to prevent this.
     */
    @Bean
    @ConditionalOnClass(ConcurrentKafkaListenerContainerFactory.class)
    @SuppressWarnings({"rawtypes", "unchecked"})
    static BeanPostProcessor kafkaListenerMdcConfigurer() {
        KafkaMdcRecordInterceptor<Object, Object> interceptor = new KafkaMdcRecordInterceptor<>();
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof ConcurrentKafkaListenerContainerFactory factory
                        && factory.getRecordInterceptor() == null) {
                    factory.setRecordInterceptor(interceptor);
                }
                return bean;
            }
        };
    }
}
