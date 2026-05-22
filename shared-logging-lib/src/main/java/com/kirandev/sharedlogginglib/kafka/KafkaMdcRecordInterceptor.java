package com.kirandev.sharedlogginglib.kafka;

import com.kirandev.sharedlogginglib.LoggingMdc;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.MDC;
import org.springframework.kafka.listener.RecordInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Spring Kafka RecordInterceptor that extracts W3C/B3 trace headers from an incoming
 * Kafka ConsumerRecord and populates MDC before the listener is invoked, then cleans up
 * the injected keys after the record is processed.
 *
 * <p>Supports {@code traceparent} (W3C), {@code X-B3-TraceId / X-B3-SpanId} (Zipkin B3),
 * {@code X-Correlation-Id}, and Kafka position metadata (topic, partition, offset).
 *
 * <p>Register via KafkaLoggingAutoConfiguration (automatic when shared-logging-lib is a
 * dependency) or manually:
 * <pre>{@code
 * factory.setRecordInterceptor(new KafkaMdcRecordInterceptor<>());
 * }</pre>
 */
public class KafkaMdcRecordInterceptor<K, V> implements RecordInterceptor<K, V> {

    private static final ThreadLocal<List<String>> INJECTED_KEYS = new ThreadLocal<>();

    @Override
    public ConsumerRecord<K, V> intercept(ConsumerRecord<K, V> record, Consumer<K, V> consumer) {
        List<String> injected = new ArrayList<>();

        String traceparent = headerValue(record, "traceparent");
        if (traceparent != null) {
            String[] parts = traceparent.split("-");
            if (parts.length >= 4) {
                set(LoggingMdc.TRACE_ID, parts[1], injected);
                set(LoggingMdc.SPAN_ID, parts[2], injected);
            }
        } else {
            setFromHeader(record, "X-B3-TraceId", LoggingMdc.TRACE_ID, injected);
            setFromHeader(record, "X-B3-SpanId", LoggingMdc.SPAN_ID, injected);
        }

        if (MDC.get(LoggingMdc.TRACE_ID) == null) {
            set(LoggingMdc.TRACE_ID, UUID.randomUUID().toString().replace("-", ""), injected);
        }

        setFromHeader(record, "X-Correlation-Id", LoggingMdc.CORRELATION_ID, injected);

        set(LoggingMdc.KAFKA_TOPIC, record.topic(), injected);
        set(LoggingMdc.KAFKA_PARTITION, String.valueOf(record.partition()), injected);
        set(LoggingMdc.KAFKA_OFFSET, String.valueOf(record.offset()), injected);

        String consumerGroup = consumer.groupMetadata() != null
                ? consumer.groupMetadata().groupId()
                : null;
        if (consumerGroup != null) {
            set(LoggingMdc.KAFKA_CONSUMER_GROUP, consumerGroup, injected);
        }

        INJECTED_KEYS.set(injected);
        return record;
    }

    @Override
    public void success(ConsumerRecord<K, V> record, Consumer<K, V> consumer) {
        clearInjected();
    }

    @Override
    public void failure(ConsumerRecord<K, V> record, Exception exception, Consumer<K, V> consumer) {
        clearInjected();
    }

    private void set(String key, String value, List<String> injected) {
        if (value != null && !value.isBlank()) {
            MDC.put(key, value);
            injected.add(key);
        }
    }

    private void setFromHeader(ConsumerRecord<K, V> record, String headerName,
                               String mdcKey, List<String> injected) {
        String value = headerValue(record, headerName);
        if (value != null) {
            set(mdcKey, value, injected);
        }
    }

    private String headerValue(ConsumerRecord<K, V> record, String name) {
        Header header = record.headers().lastHeader(name);
        return header != null ? new String(header.value(), StandardCharsets.UTF_8) : null;
    }

    private void clearInjected() {
        List<String> keys = INJECTED_KEYS.get();
        if (keys != null) {
            keys.forEach(MDC::remove);
            INJECTED_KEYS.remove();
        }
    }
}
