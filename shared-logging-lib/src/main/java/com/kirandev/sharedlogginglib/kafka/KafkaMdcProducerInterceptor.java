package com.kirandev.sharedlogginglib.kafka;

import com.kirandev.sharedlogginglib.LoggingMdc;
import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.MDC;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Kafka ProducerInterceptor that propagates the current MDC trace context into
 * Kafka record headers so downstream consumers can correlate logs.
 *
 * <p>Injects W3C {@code traceparent}, B3 {@code X-B3-TraceId / X-B3-SpanId}, and
 * {@code X-Correlation-Id} headers. Compatible with Dynatrace OneAgent, Splunk OTel,
 * Grafana Alloy, and any W3C/B3-aware consumer.
 *
 * <p>Wire-up (choose one):
 * <ol>
 *   <li>Auto — add {@code shared-logging-lib} as a dependency; KafkaLoggingAutoConfiguration
 *       will register a BeanPostProcessor that calls
 *       {@code DefaultKafkaProducerFactory.updateConfigs()} to append this class.</li>
 *   <li>Manual YAML — {@code spring.kafka.producer.properties.interceptor.classes:
 *       com.kirandev.sharedlogginglib.kafka.KafkaMdcProducerInterceptor}</li>
 *   <li>Custom config — add {@code ProducerConfig.INTERCEPTOR_CLASSES_CONFIG} to your
 *       producer properties map.</li>
 * </ol>
 */
public class KafkaMdcProducerInterceptor<K, V> implements ProducerInterceptor<K, V> {

    @Override
    public ProducerRecord<K, V> onSend(ProducerRecord<K, V> record) {
        String traceId = MDC.get(LoggingMdc.TRACE_ID);
        String spanId = MDC.get(LoggingMdc.SPAN_ID);
        String correlationId = MDC.get(LoggingMdc.CORRELATION_ID);

        if (traceId != null && spanId != null) {
            // W3C Trace Context: 00-{32-hex-traceId}-{16-hex-spanId}-{flags}
            String traceparent = "00-" + pad(traceId, 32) + "-" + pad(spanId, 16) + "-01";
            injectHeader(record, "traceparent", traceparent);
        }
        injectHeader(record, "X-B3-TraceId", traceId);
        injectHeader(record, "X-B3-SpanId", spanId);
        injectHeader(record, "X-Correlation-Id", correlationId);
        return record;
    }

    @Override
    public void onAcknowledgement(RecordMetadata metadata, Exception exception) {}

    @Override
    public void close() {}

    @Override
    public void configure(Map<String, ?> configs) {}

    private void injectHeader(ProducerRecord<K, V> record, String name, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        record.headers().remove(name);
        record.headers().add(name, value.getBytes(StandardCharsets.UTF_8));
    }

    private String pad(String value, int targetLength) {
        if (value.length() >= targetLength) {
            return value.substring(0, targetLength);
        }
        return "0".repeat(targetLength - value.length()) + value;
    }


}
