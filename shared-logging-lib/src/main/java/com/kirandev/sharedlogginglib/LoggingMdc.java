package com.kirandev.sharedlogginglib;

/**
 * MDC keys aligned with OpenTelemetry semantic conventions and common
 * observability backends (Dynatrace, Splunk, Grafana/Loki, Elastic).
 *
 * @see <a href="https://opentelemetry.io/docs/specs/semconv/general/logs/">OTel log conventions</a>
 */
public final class LoggingMdc {

    public static final String TRACE_ID = "traceId";
    public static final String SPAN_ID = "spanId";
    public static final String SERVICE_ID = "serviceId";
    public static final String REQUEST_ID = "requestId";
    public static final String CORRELATION_ID = "correlationId";
    public static final String USER_ID = "userId";
    public static final String HTTP_METHOD = "httpMethod";
    public static final String HTTP_PATH = "httpPath";

    /** Kafka message context keys — set by KafkaMdcRecordInterceptor. */
    public static final String KAFKA_TOPIC = "kafka.topic";
    public static final String KAFKA_PARTITION = "kafka.partition";
    public static final String KAFKA_OFFSET = "kafka.offset";
    public static final String KAFKA_CONSUMER_GROUP = "kafka.consumer.group";

    private LoggingMdc() {
    }
}
