# Shared Logging Architecture

All microservices use `shared-logging-lib` for structured logs, MDC correlation, and OpenTelemetry-compatible trace fields.

## Modules

| Artifact | Role |
|----------|------|
| `shared-logging-lib` | Logback config, MDC filter, OTel MDC bridge, async MDC propagation |
| `app-config-data` | Pulls in the library for every service |
| `config-server-repository/application.yml` | Central logging defaults via Spring Cloud Config |

## Log format by environment

| Profile | Console output | File logging |
|---------|----------------|--------------|
| `local` | Human-readable, coloured | Optional (`file-logging` profile) |
| `docker`, `kubernetes`, `staging`, `prod` | JSON (Logstash schema) | Off by default (stdout collected by platform) |

Activate file appenders with Spring profile `file-logging` or property `kirandev.logging.file-enabled=true` on VMs.

## Correlation fields (MDC → JSON)

| Field | Source |
|-------|--------|
| `traceId` / `trace_id` | W3C `traceparent`, B3 `X-B3-TraceId`, or OpenTelemetry active span |
| `spanId` / `span_id` | B3 `X-B3-SpanId` or OpenTelemetry active span |
| `serviceId` | `spring.application.name` |
| `requestId` | `X-Request-Id` (generated if absent) |
| `correlationId` | `X-Correlation-Id` |
| `service.name`, `host.name`, `deployment.environment` | Static JSON fields on every log line |

## OpenTelemetry agents

Works with **Dynatrace OneAgent**, **Splunk OTel Collector**, **Grafana Alloy/Agent**, and the **OpenTelemetry Java agent**:

1. Attach the vendor agent to the JVM (`-javaagent:...`) or run the OTel operator sidecar in Kubernetes.
2. Logs written to stdout as JSON are scraped by Fluent Bit, Promtail, Splunk HEC, or Dynatrace log ingest.
3. `OpenTelemetryMdcTurboFilter` copies active span IDs into MDC so log lines link to traces in Grafana Tempo, Splunk APM, or Dynatrace distributed traces.

Example Kubernetes env:

```yaml
env:
  - name: SPRING_PROFILES_ACTIVE
    value: kubernetes
  - name: OTEL_SERVICE_NAME
    valueFrom:
      fieldRef:
        fieldPath: metadata.labels['app']
  - name: OTEL_RESOURCE_ATTRIBUTES
    value: deployment.environment=production,service.namespace=microservices-demo
  - name: DEPLOYMENT_ENVIRONMENT
    value: production
```

## Adding a new service

1. Depend on `app-config-data` (or `shared-logging-lib` directly).
2. Add `src/main/resources/logback-spring.xml`:

```xml
<configuration>
    <include resource="com/kirandev/logging/logback-shared.xml"/>
</configuration>
```

3. Set `spring.application.name` in `application.yaml`.
4. For local dev: `SPRING_PROFILES_ACTIVE=local`.

## Non-web workloads

Use `MdcContext.wrap()` for async/Kafka tasks:

```java
executor.execute(MdcContext.wrap(() -> processTweet(tweet)));
```
