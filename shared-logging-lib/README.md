Shared logging utilities and OpenTelemetry integration

Overview
- Provides JSON console logging (Logback + Logstash encoder) suitable for Docker/Kubernetes/stdout collection.
- Populates `trace_id` and `span_id` into MDC so logs can be correlated with OpenTelemetry traces.
- Optional OTLP exporter can be enabled with `OTEL_EXPORTER_OTLP_ENDPOINT`.

Usage
- Add a dependency on `shared-logging-lib` in your Spring Boot module's `pom.xml`.
- Ensure your application uses `logback-spring.xml` from the library (Spring Boot will pick it up automatically).
- For web apps the `OTelMdcFilter` will populate MDC per request. For non-web threads call `OTelMdc.populate()`.

Environment variables
- `OTEL_EXPORTER_OTLP_ENDPOINT` — if set, spans are exported to this OTLP endpoint (e.g. http://otel-collector:4317).
- `SPRING_APPLICATION_NAME` or `spring.application.name` — used as `service.name` field in logs.

Integration notes
- Docker/Kubernetes: logs are written to stdout in JSON — configure your log collector (Fluentd/Fluent Bit/Filebeat) to forward to Splunk, Grafana Loki, or other sinks.
- Dynatrace: use the OneAgent or OpenTelemetry Collector configured to forward to Dynatrace; set `OTEL_EXPORTER_OTLP_ENDPOINT` accordingly.
- Splunk: recommend using a centralized log forwarder or Splunk HEC — collect container stdout and forward to HEC.
- Grafana: traces can be sent to Tempo via OTLP; logs to Loki via your log forwarder.
