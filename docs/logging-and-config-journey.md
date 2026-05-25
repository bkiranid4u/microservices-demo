# Logging & Config Server Journey — microservices-demo

A chronological log of what went wrong, what we learned, and the changes that
landed while standing up the common logging pattern and config-server-driven
property wiring for this project. Each chapter follows the shape:

> **Situation → Symptom → Diagnosis → Fix → Why it matters**

The intent is reference-quality: future readers should be able to skim the
table of contents, jump to a single chapter, and understand both the change
and the reasoning without re-running the conversation.

---

## Table of contents

1. [Initial scope — common logging pattern across modules](#chapter-1--initial-scope)
2. [`META-INF` and Spring Boot auto-configuration discovery](#chapter-2--meta-inf-and-spring-boot-auto-config)
3. [Duplicate YAML key in `application.yml`](#chapter-3--duplicate-yaml-key)
4. [Scope reduction: drop OTel and Kubernetes implementation](#chapter-4--scope-reduction-drop-otel-and-k8s)
5. [Logback `FILE` appender opened on startup even when unused](#chapter-5--logback-file-appender-eager-open)
6. [Docker `/var/log/app` permission denied for non-root user](#chapter-6--docker-log-directory-permissions)
7. [`RetryTemplateBuilder` assertion: `Max interval should be > than initial interval`](#chapter-7--spring-cloud-config-retry-assertion)
8. [Config-server → service wiring audit for `retry-config`](#chapter-8--config-server-wiring-audit)
9. [`optional:classpath:application-logging.yaml` explained](#chapter-9--config-import-directives-explained)
10. [Where `configserver:` comes from — Spring Cloud SPI](#chapter-10--configserver-spi-deep-dive)
11. [`NoClassDefFoundError: jakarta.servlet.Filter` in non-web service](#chapter-11--noclassdeffounderror-jakartaservletfilter)
12. [Kafka `createTopics` timeout — host/container address mismatch](#chapter-12--kafka-createtopics-timeout)
13. [Retry tuning for KRaft cluster cold-start](#chapter-13--retry-tuning-for-kraft-cold-start)
14. [`spring.factories` vs `AutoConfiguration.imports` — SPI and the Boot 4 metadata layout](#chapter-14--springfactories-vs-autoconfigurationimports)
15. [Final state — what is on disk now](#final-state)
16. [Reusable engineering lessons](#reusable-engineering-lessons)

---

## Chapter 1 — Initial scope

**Situation.** Lead-architect task: implement a common logging pattern usable
across all modules in `microservices-demo` (Spring Boot 4.0.6, Java 25,
Spring Cloud 2025.1.0). It had to work on local IDE runs, Docker, and
Kubernetes; produce structured JSON in containerised environments; and be
ready to integrate with OpenTelemetry-compatible agents (Dynatrace, Splunk,
Grafana Alloy).

**Starting state.** A `shared-logging-lib` skeleton already existed but had
problems we discovered while reading the code:

| Issue | Detail |
|---|---|
| Duplicate package | `com.kirandev.logging` (orphaned, manual OTel SDK setup) ran alongside `com.kirandev.sharedlogginglib` (the canonical one registered in `AutoConfiguration.imports`) |
| Broken include target | Services did `<include resource="com/kirandev/logging/logback-shared.xml"/>` but the target file used `<configuration>` root (only `<included>` is includable) |
| Missing dependency | `twitter-to-kafka-service` did not depend on `shared-logging-lib` at all |
| No Kafka MDC | Producer didn't stamp trace headers, consumer didn't extract them |
| No OTel Collector | Services exported nothing for log/trace correlation |

**Fix landed.** A clean MDC propagation toolkit plus auto-configuration:

- `LoggingMdc` — constants for MDC keys (`traceId`, `spanId`, `serviceId`, `correlationId`, `kafka.topic/partition/offset`, …).
- `MdcFilter` — servlet filter that reads W3C `traceparent` + B3 headers into MDC.
- `MdcTaskDecorator` + `AsyncMdcConfiguration` — propagates MDC across `TaskExecutor` threads (virtual or platform).
- `MdcContext` — utility for non-web flows (Kafka listeners, schedulers).
- `ServiceMdcInitializer` — seeds `serviceId` and `deployment.environment` on startup.
- `KafkaMdcProducerInterceptor` (Kafka `ProducerInterceptor`) — injects `traceparent`, `X-B3-TraceId/SpanId`, `X-Correlation-Id` into outgoing records.
- `KafkaMdcRecordInterceptor` (Spring Kafka `RecordInterceptor`) — extracts those headers into MDC before each listener fires, cleans up after.
- `LoggingAutoConfiguration` — top-level auto-config wiring the always-on beans.
- `ServletMdcAutoConfiguration` — *(added in chapter 11)* web-only beans isolated behind a class-level `@ConditionalOnClass(Filter.class)`.
- `KafkaLoggingAutoConfiguration` — auto-wires the two Kafka interceptors via `BeanPostProcessor` on existing `DefaultKafkaProducerFactory` / `ConcurrentKafkaListenerContainerFactory` beans, conditional on Spring Kafka being on the classpath.
- `logback-base.xml` — single `<included>` fragment with `local` (coloured) vs default (JSON via `LoggingEventCompositeJsonEncoder`) profiles, file appender gated behind `staging,prod,file-logging` profiles.
- `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` — registers the three auto-config classes.

The orphaned `com.kirandev.logging` package and `logback-shared.xml` were deleted. `twitter-to-kafka-service/pom.xml` got `shared-logging-lib` as a direct dependency. The whole reactor was compiled green before moving on.

**Why it matters.** A drop-in library: any new service adds the dependency, adds a one-line `logback-spring.xml` (`<include resource="logback-base.xml"/>`), and gets MDC propagation, JSON-or-coloured console, and Kafka header propagation for free. No `@Import` annotations needed.

---

## Chapter 2 — META-INF and Spring Boot auto-config

**Situation.** Question raised: what does `META-INF/` under `src/main/resources` actually do, and is it still relevant in Spring Boot 4?

**Key points captured.**

| Marker file | Owner | Purpose |
|---|---|---|
| `META-INF/MANIFEST.MF` | JAR spec | The only required file; entry point, classpath, version |
| `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` | Spring Boot 2.7+ / 3 / 4 | **Current standard** for declaring auto-configuration classes contributed by a library |
| `META-INF/spring.factories` | Spring Boot ≤ 2.6 | Legacy; still loads in Boot 4 with a deprecation warning |
| `META-INF/services/<interface>` | Java SPI (`ServiceLoader`) | Pure Java mechanism — used by JDBC drivers, annotation processors, etc. |
| `META-INF/spring-configuration-metadata.json` | Generated by `spring-boot-configuration-processor` | Powers IDE autocompletion of `@ConfigurationProperties` |
| `META-INF/additional-spring-configuration-metadata.json` | You, manually | Extra hints (enums, deprecation, descriptions for env-var-only props) |

**Why it matters in Boot 4 specifically.** Boot 4 still supports `spring.factories` but only the `.imports` file is the officially supported path forward; `@ConditionalOnClass` evaluation uses ASM bytecode scanning so it never triggers JVM classloading — a property we relied on in chapter 11.

---

## Chapter 3 — Duplicate YAML key

**Situation.** While writing `config-server-repository/application.yml`, we wrote:

```yaml
otel:
  traces:
    sampler: ${OTEL_TRACES_SAMPLER:parentbased_traceidratio}   # scalar
    sampler:                                                    # map — wins
      arg: ${OTEL_TRACES_SAMPLER_ARG:1.0}
```

**Symptom.** Effectively silent — the YAML parser doesn't complain, the scalar value just disappears.

**Diagnosis.** YAML spec says duplicate sibling keys are invalid, but `snakeyaml` (used by Spring Boot) and most parsers tolerate them by letting the last one overwrite. The sampler type was therefore being lost; only `sampler.arg` survived.

**Compounding problem.** The whole `otel:` block was wrong-headed in any case: `OTEL_TRACES_SAMPLER`, `OTEL_EXPORTER_OTLP_ENDPOINT` etc. are **OpenTelemetry agent environment-variable names** lowercased — they are not Spring properties. The OTel Java agent reads them from the OS environment, not from Spring's `Environment`. Mapping them into a YAML property path achieved nothing.

**Fix.** Removed the entire `otel:` block. Kept only the supported Spring paths:

```yaml
management:
  tracing:
    sampling:
      probability: ${OTEL_TRACES_SAMPLER_ARG:1.0}
  otlp:
    tracing:
      endpoint: ${OTEL_EXPORTER_OTLP_ENDPOINT:}
```

(later removed entirely in chapter 4).

**Why it matters.** Duplicate keys in YAML are one of the highest-leverage bug classes in config-driven systems — silent, easy to miss in review, and only manifest as a missing-feature bug far from the cause. Same with property-path drift: if a property name looks like it's binding to a framework feature but isn't, every value put there is dead. Lesson: write a `python3 -c "import yaml; yaml.safe_load(open('...'))"` check into CI for every YAML file.

---

## Chapter 4 — Scope reduction: drop OTel and K8s

**Situation.** Decision to remove OpenTelemetry integration and Kubernetes-specific files entirely. Keep the logging pattern, drop the observability stack.

**Changes landed.**

| Removed | Location |
|---|---|
| `opentelemetry-logback-mdc`, `opentelemetry-api`, `opentelemetry-sdk`, `opentelemetry-exporter-otlp` | `shared-logging-lib/pom.xml` (deps + properties) |
| `opentelemetry-logback-mdc.version` property and `dependencyManagement` entry | root `pom.xml` |
| `<turboFilter class="io.opentelemetry.instrumentation.logback.mdc.v1_0.OpenTelemetryMdcTurboFilter"/>` | `logback-base.xml` |
| `trace_id` / `span_id` MDC key references (OTel naming convention) | `logback-base.xml` JSON encoder |
| `io.opentelemetry` logger suppression line | `logback-base.xml` |
| `OTEL_TRACE_ID` / `OTEL_SPAN_ID` constants | `LoggingMdc.java` |
| `OTEL_DEPLOYMENT_ENVIRONMENT` env-var fallback | `ServiceMdcInitializer.java` |
| `firstNonBlank(...)` helper and `OTEL_*` MDC fallbacks | `KafkaMdcProducerInterceptor.java` |
| `management.tracing.sampling.probability`, `management.otlp.tracing.endpoint` | `application-logging.yaml`, `application.yml` |
| `OTEL_*` environment variables (six per service) | `docker-compose.yaml` |
| `observability.yml`, `otel-collector-config.yaml`, `grafana-datasources.yaml`, `tempo-config.yaml` | `docker-compose/` |
| `logging-env.example.yaml` and the `k8s/` directory | repo root |
| `${POD_NAME:-...}` fallback in `HOST_NAME` | `logback-base.xml` |

**Result.** `shared-logging-lib` now has **zero** external observability dependencies. Just `logback-classic` + `logstash-logback-encoder`. MDC propagation (HTTP → Kafka → async threads) still works fully — only the OTel bridge layer is gone.

**Why it matters.** Scope discipline: trying to fit OTel into the library while it was still finding its shape buried the actual goal. Pulling it out left a smaller, sharper logging pattern that the rest of the journey could be built on.

---

## Chapter 5 — Logback `FILE` appender eager open

**Symptom.** Running locally:

```
ERROR in ch.qos.logback.core.rolling.RollingFileAppender[FILE]
  - Failed to create parent directories for [/var/log/app/config-server.log]
  - openFile(/var/log/app/config-server.log,true) call failed.
    java.io.FileNotFoundException: /var/log/app/config-server.log
```

**Diagnosis.** Two real bugs in `logback-base.xml` after the OTel cleanup:

1. The `FILE` (`RollingFileAppender`) was declared **outside** any `<springProfile>`. Logback's `RollingFileAppender.start()` opens the underlying `<file>` at construction time — not lazily on first event. So just having the appender XML in the configuration was enough to attempt `mkdir(/var/log/app)` on every startup, regardless of profile.
2. The `<root>` element had been accidentally dropped during the same rewrite. Without a `<root>`, services were silently falling back to Logback's default (DEBUG → console), which is the wrong baseline.

**Fix.** Wrapped both `FILE` and `ASYNC_FILE` in `<springProfile name="staging,prod,file-logging">`, restored the `<root>` element with the file appender conditionally attached via the same profile guard, and added a comment explaining the constraint.

```xml
<springProfile name="staging,prod,file-logging">
  <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">…</appender>
  <appender name="ASYNC_FILE" class="ch.qos.logback.classic.AsyncAppender">…</appender>
</springProfile>

<root level="${LOG_LEVEL}">
  <appender-ref ref="ASYNC_CONSOLE"/>
  <springProfile name="staging,prod,file-logging">
    <appender-ref ref="ASYNC_FILE"/>
  </springProfile>
</root>
```

**Behaviour matrix afterwards.**

| `SPRING_PROFILES_ACTIVE` | Console | File |
|---|---|---|
| none / `mvn spring-boot:run` | JSON to stdout | none |
| `local` | coloured to stdout | none |
| `docker` | JSON to stdout | none |
| `staging` / `prod` / `file-logging` | JSON to stdout | rolling JSON file |

**Why it matters.** Logback appender lifecycle is not what most users assume. Declaring an appender is enough to open its sink — there is no “lazy on first event” behaviour for file-based appenders. Always gate IO-touching appenders behind a profile or class-level condition.

---

## Chapter 6 — Docker log directory permissions

**Symptom.** After the chapter 5 fix, running in Docker with `file-logging` profile still failed because the non-root container user couldn't write to `/var/log/app`.

**Diagnosis.** Both Dockerfile stages run as a non-root `appuser` (UID 1000). `LOGGING_FILE_PATH=/var/log/app` pointed into `/var/log/` which is root-owned on `eclipse-temurin:25-jre-alpine`. `RollingFileAppender.start()` calls `mkdirs()` itself, but `mkdirs()` fails when any ancestor (here `/var/log/`) is not writable by the runtime user.

**Fix.**

1. Dockerfile stages now pre-create a writable log directory **before** `USER appuser`:
   ```dockerfile
   RUN mkdir -p /app/logs/archived && chown -R appuser:appgroup /app
   USER appuser
   ```
   The `archived/` subdirectory is created up-front so the rolling policy's gzipped rotation files have somewhere to land without runtime `mkdir`.

2. `docker-compose.yaml` switched `LOGGING_FILE_PATH` from `/var/log/app` (not writable) to `/app/logs` (owned by appuser). Added a comment explaining when the value is actually used.

**Why it matters.** In container-native deployments stdout-only is still the right default — Docker / Kubernetes runtimes are designed to collect, rotate, and forward stdout. File logging in containers is a special case, but if you choose it, the only way to avoid silent permission failures is to bake the writable directory into the image at build time with the runtime user as owner.

---

## Chapter 7 — Spring Cloud Config retry assertion

**Symptom.** `twitter-to-kafka-service` failed to start with:

```
java.lang.IllegalArgumentException: Max interval should be > than initial interval
  at RetryTemplateBuilder.exponentialBackoff(RetryTemplateBuilder.java:257)
  at org.springframework.cloud.config.client.RetryTemplateFactory.create(...)
```

…thrown from the `EnvironmentPostProcessor` phase — before any application bean was even constructed.

**Diagnosis.** `application.yaml` had:

```yaml
spring.cloud.config.retry:
  initial-interval: 2000
  max-attempts: 6
  max-interval: 2000
```

`RetryTemplateBuilder.exponentialBackoff()` enforces `max > initial` (strict). Equal values fail the assertion. The intent was probably to ask for a fixed 2s retry, but Spring Cloud Config Client only models exponential backoff — there is no fixed-interval option without writing a custom `RetryTemplate`.

**Fix.**

```yaml
retry:
  initial-interval: 1000    # 1s first retry
  multiplier: 1.5
  max-interval: 10000       # 10s cap
  max-attempts: 6
```

Retry schedule with these values:

| Attempt | Wait |
|---|---|
| 1→2 | 1.0 s |
| 2→3 | 1.5 s |
| 3→4 | 2.25 s |
| 4→5 | 3.375 s |
| 5→6 | 5.06 s |

~13s worst-case startup delay if config-server is down, then `fail-fast: true` aborts the boot.

**Important distinction made here.** This retry config is for the **Config Client's** call to the config-server. It is **not** the same as `retry-config.*` in `config-server-repository/twitter-to-kafka-service.yml`, which configures the application's own `RetryTemplate` (used by `KafkaAdminClient`). Two retry configs at different layers, with similar names — easy to confuse.

---

## Chapter 8 — Config-server wiring audit

**Question.** Is `retry-config` from the config-server actually reaching the application's `RetryConfig` bean?

**Audit findings.**

1. **Wiring chain is correct (functionally).** `config-server-repository/twitter-to-kafka-service.yml` is served by config-server, fetched by Config Client at startup, bound into `RetryConfigData` (`@ConfigurationProperties(prefix="retry-config")` in `app-config-data`), consumed by `RetryConfig` (in `common-config`), which builds the `RetryTemplate` bean used by `KafkaAdminClient`.

2. **`common-config` was only a transitive dependency** of `twitter-to-kafka-service` (via `kafka-producer → common-config`). Made it an explicit direct dependency to keep it from silently disappearing if `kafka-producer`'s pom ever changes.

3. **No runtime visibility** that property binding actually succeeded. Added a `@PostConstruct` startup log on `RetryConfig` that prints all five `retry-config.*` values. Any `null` indicates the binding failed; all-`null` indicates the config-server fetch failed silently.

4. **Code quality.** Made `RetryConfig.retryConfigData` `final` to match the immutable-by-default pattern used in the rest of the project.

**Anti-pattern noted (left in place).** The `@ConfigurationProperties` classes (`RetryConfigData`, `KafkaConfigData`, `TwitterToKafkaServiceConfigData`, `KafkaProducerConfigData`) all use both `@Configuration` and `@ConfigurationProperties`. This works but the canonical Spring Boot 2.2+ pattern is `@ConfigurationProperties` alone + `@ConfigurationPropertiesScan` on the main app. Left untouched to keep blast radius small.

---

## Chapter 9 — Config import directives explained

**Question.** What does `spring.config.import: "optional:classpath:application-logging.yaml"` do?

**Anatomy.**

```
optional : classpath : application-logging.yaml
   |          |              |
   |          |              └── resource path (file at the root of some classpath entry)
   |          └────────────────── protocol prefix (other valid: file:, configserver:, configtree:)
   └──────────────────────────────── behaviour modifier — don't throw if the source can't be resolved
```

`classpath:application-logging.yaml` is shipped inside `shared-logging-lib.jar`. Any service that depends on the lib gets these defaults imported into its `Environment`. Service-specific `application.yaml` overrides them.

**Property-source priority** (higher wins):

```
HIGHEST
  ↑  Command-line args
  ↑  OS environment variables (KAFKA_CONFIG_BOOTSTRAP_SERVERS etc.)
  ↑  SPRING_APPLICATION_JSON
  ↑  config-server-served properties
  ↑  service's own application.yaml
  ↑  imported application-logging.yaml      ← this one
  ↓  Spring Boot defaults
LOWEST
```

**Why `optional:` matters.** Without it, a slim test classpath that doesn't include `shared-logging-lib` would fail at startup with `ConfigDataLocationNotFoundException`. The optional prefix makes the import a "load if present" hint — appropriate for library defaults.

---

## Chapter 10 — `configserver:` SPI deep dive

**Question.** Where is `configserver:` defined? It's not a Spring Boot built-in.

**Trace.**

1. `twitter-to-kafka-service/pom.xml` depends on `spring-cloud-starter-config`, which pulls in `spring-cloud-config-client-5.0.0.jar`.

2. That JAR contains:
   ```
   META-INF/spring.factories
   ```
   which registers two `ConfigData` SPI implementations:
   ```properties
   org.springframework.boot.context.config.ConfigDataLocationResolver=\
     org.springframework.cloud.config.client.ConfigServerConfigDataLocationResolver

   org.springframework.boot.context.config.ConfigDataLoader=\
     org.springframework.cloud.config.client.ConfigServerConfigDataLoader
   ```

3. At startup, Spring Boot's `ConfigDataEnvironment` calls `SpringFactoriesLoader.load(ConfigDataLocationResolver.class)`, which scans every JAR for `META-INF/spring.factories` and instantiates all registered resolvers.

4. When Boot processes `spring.config.import: "optional:configserver:"`, it asks each resolver in turn — `ConfigServerConfigDataLocationResolver` matches the `configserver:` prefix, reads `spring.cloud.config.*` properties, returns a `ConfigServerConfigDataResource`.

5. `ConfigServerConfigDataLoader.load(resource)` performs the HTTP fetch, wrapped in the `RetryTemplate` built from `spring.cloud.config.retry.*` (the one that threw in chapter 7). Response is flattened into property sources and merged into the `Environment`.

**Why it matters.** The `ConfigData` SPI is the supported extension point for adding new config-source protocols. Any library can plug in a new `<scheme>:` prefix via the same two-class pattern. Examples in the wild: Vault, Consul, Zookeeper, AWS SSM.

---

## Chapter 11 — `NoClassDefFoundError: jakarta.servlet.Filter`

**Symptom.** Local run of the non-web `twitter-to-kafka-service`:

```
java.lang.NoClassDefFoundError: jakarta/servlet/Filter
  …
Caused by: IllegalStateException: Failed to introspect Class
  [com.kirandev.sharedlogginglib.LoggingAutoConfiguration]
Caused by: BeanTypeDeductionException: Failed to deduce bean type for
  com.kirandev.sharedlogginglib.LoggingAutoConfiguration.serviceMdcInitializer
Caused by: @ConditionalOnMissingBean did not specify a bean using type, name or
  annotation and the attempt to deduce the bean's type failed
```

**Diagnosis (the subtle one).**

`LoggingAutoConfiguration` had four `@Bean` methods. Two of them (`mdcFilter`, `mdcFilterRegistration`) returned types that transitively reference `jakarta.servlet.Filter`. The other two were classpath-safe. Each web-only method was guarded by `@ConditionalOnWebApplication`.

When Spring evaluated `serviceMdcInitializer`'s `@ConditionalOnMissingBean` (no explicit type → Spring must deduce from return type), it called `Class.getDeclaredMethods(LoggingAutoConfiguration.class)`. The JVM's `getDeclaredMethods0()` resolves **all** method signatures eagerly, including return types — so it tried to load `MdcFilter` → `OncePerRequestFilter` → `jakarta.servlet.Filter`. That class isn't on the runtime classpath (it's `provided` scope), and verification dies before `@ConditionalOnWebApplication` ever gets a chance to skip the bean.

**Key insight.** `@ConditionalOnClass` is evaluated via ASM bytecode scanning — it never triggers the JVM classloader. `@ConditionalOnWebApplication` evaluates only after the configuration class has been introspected by reflection. So a method-level web condition cannot protect you against a sibling reflection trap.

**Fix.** Extracted the two servlet beans into their own auto-config file with a **class-level** `@ConditionalOnClass(Filter.class)`:

```java
@AutoConfiguration
@ConditionalOnClass(Filter.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class ServletMdcAutoConfiguration {
    @Bean MdcFilter mdcFilter() { … }
    @Bean FilterRegistrationBean<MdcFilter> mdcFilterRegistration(MdcFilter f) { … }
}
```

Registered in `AutoConfiguration.imports` separately. Now when the servlet API is missing, ASM evaluates the class-level condition false and the class is **never loaded** — its method signatures are never resolved, so no `NoClassDefFoundError`.

Verified via `javap`:
- `LoggingAutoConfiguration.class` now has zero references to `Filter` / `Servlet` / `FilterRegistrationBean`.
- `ServletMdcAutoConfiguration.class` owns those references and is gated by its class-level condition.

**Why it matters.** This is the canonical Spring Boot auto-configuration pattern. The framework itself does this everywhere — see how `DataSourceAutoConfiguration`, `JdbcTemplateAutoConfiguration`, and `HikariCheckpointRestoreLifecycle` are split into separate classes with class-level conditions. Lesson: if a bean method's return type references an optional dependency, that method belongs in its own auto-config class with a class-level `@ConditionalOnClass`.

---

## Chapter 12 — Kafka `createTopics` timeout

**Symptom.** Running in Docker:

```
org.apache.kafka.common.errors.TimeoutException: Timed out waiting for a node assignment. Call: createTopics
…
com.kirandev.kafka.admin.exception.KafkaClientException:
  Reached max number of retry for creating kafka topic(s)!
```

**Diagnosis — address mismatch.**

Kafka brokers were configured with two listeners:

```yaml
KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:9092,EXTERNAL://0.0.0.0:19092,CONTROLLER://kafka-1:9093
KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka-1:9092,EXTERNAL://localhost:19092
```

| Where the client runs | Use these bootstrap servers |
|---|---|
| Host machine | `localhost:19092,localhost:29092,localhost:39092` (EXTERNAL listener mapped to host ports) |
| Container on `kafka-net` | `kafka-1:9092,kafka-2:9092,kafka-3:9092` (PLAINTEXT internal listener) |

The config-server was serving `bootstrap-servers: localhost:19092,...` (the host values). Inside the container, `localhost` resolves to the container itself — no broker there. Client kept timing out and the retry loop eventually gave up.

A second red herring: `docker-compose.yaml` set `SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka-1:9092,...` — but the application **doesn't use Spring Boot's Kafka auto-config**. It uses a custom `KafkaConfigData` (`@ConfigurationProperties(prefix="kafka-config")`) wired into hand-rolled `KafkaProducerConfig` and `KafkaAdminConfig`. So `spring.kafka.*` properties were dead.

**Fix.**

1. `config-server-repository/twitter-to-kafka-service.yml` — converted the hard-coded addresses to env-var placeholders with the host-local defaults:
   ```yaml
   kafka-config:
     bootstrap-servers: ${KAFKA_CONFIG_BOOTSTRAP_SERVERS:localhost:19092,localhost:29092,localhost:39092}
     schema-registry-url: ${KAFKA_CONFIG_SCHEMA_REGISTRY_URL:http://localhost:8081}
   ```

2. `docker-compose.yaml` — replaced the dead `SPRING_KAFKA_*` env vars with ones that bind to the actual `kafka-config.*` prefix:
   ```yaml
   KAFKA_CONFIG_BOOTSTRAP_SERVERS: kafka-1:9092,kafka-2:9092,kafka-3:9092
   KAFKA_CONFIG_SCHEMA_REGISTRY_URL: http://schema-registry:8081
   ```

**Important nuance.** Placeholder resolution happens on the **client side** (in `twitter-to-kafka-service`), not on the config-server. Curling the config-server endpoint will show the literal `${KAFKA_CONFIG_BOOTSTRAP_SERVERS:...}` text — that's fine and correct. The client resolves the placeholder against its own `Environment`, which has the docker-side env var set.

**Why it matters.** Two reusable lessons:

- When a docker-compose env var is set but doesn't affect runtime behaviour, suspect a **property-prefix drift** — the env var name corresponds to a Spring Boot property path the application no longer reads.
- For multi-environment configuration in Spring Cloud Config, the `${ENV_VAR:default}` placeholder pattern in served YAML beats per-profile YAML files. The defaults document local intent; the env vars adapt per environment without proliferating profile files.

---

## Chapter 13 — Retry tuning for KRaft cold-start

**Situation.** The previous fix solved the address problem, but `depends_on.condition: service_healthy` only verifies the **per-broker** healthcheck (`kafka-cluster cluster-id`). That check can pass before the controller quorum is fully elected and the ISR (in-sync replica set) stable. With `replication-factor: 3`, topic creation cannot proceed until all three brokers agree on a leader and the quorum is settled — a window of several seconds on a cold start.

**Fix.** Tuned `retry-config.*` in `config-server-repository/twitter-to-kafka-service.yml`:

```yaml
retry-config:
  initial-interval: 1s
  multiplier: 2.0
  max-interval: 30s    # was 10s
  max-attempts: 6      # was 3
  sleep-time: 5s
```

Backoff schedule:

| Attempt | Per-attempt wait | Cumulative |
|---|---|---|
| 1→2 | 1 s | 1 s |
| 2→3 | 2 s | 3 s |
| 3→4 | 4 s | 7 s |
| 4→5 | 8 s | 15 s |
| 5→6 | 16 s | 31 s |
| 6→7 | 30 s (capped) | 61 s |

Worst-case wait before `KafkaClientException` is thrown: ~60–90s — enough for the KRaft quorum to settle even on a slow cold start.

**The `max-interval` cap is the subtle part.** Without it, attempt 7 would have been `1 × 2^6 = 64s` — a single retry longer than the entire previous schedule. Capping keeps the schedule shape predictable.

**Why it matters.** Container-orchestrator "service is healthy" is a per-container guarantee; cluster-level invariants (quorum, ISR, leader election) are emergent properties that need a separate retry surface to absorb. Always assume the cluster's first 30–60 seconds are unstable and design the client retry budget accordingly.

---

## Chapter 14 — `spring.factories` vs `AutoConfiguration.imports`

**Questions raised.** Is `META-INF/spring.factories` still relevant in Spring Boot 4? And what does "SPI" mean — it kept coming up in chapters 2 and 10.

**Evidence gathered from `~/.m2`.** Both files coexist in Boot-4-era artifacts:

```
spring-cloud-config-client-5.0.0.jar:
  META-INF/spring.factories                                              (1.2 KB — six SPIs registered)
  META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports  (70 B — one class)
```

This is the canonical Boot-4 layout: `.imports` for auto-config, `spring.factories` for every other SPI.

### What "SPI" means

**Service Provider Interface** — a plug-in mechanism. The framework declares an interface; a separate JAR provides an implementation; the runtime discovers them through a file in `META-INF/`. Neither side imports the other at compile time.

| | API | SPI |
|---|---|---|
| Who calls it? | You | The framework |
| Who implements it? | The framework | You (or a third party) |
| Dependency direction | Your code → framework | Framework → your code |
| Goal | **Use** the framework | **Extend** the framework |

Two discovery mechanisms exist on the JVM:

| Mechanism | File location | Used by |
|---|---|---|
| Java `ServiceLoader` | `META-INF/services/<interface-fqn>` | JDBC drivers, annotation processors, `Slf4j` bindings |
| Spring `SpringFactoriesLoader` | `META-INF/spring.factories` + `META-INF/spring/<interface-fqn>.imports` | Spring Boot / Spring Cloud |

Spring rolled its own because it needs ASM bytecode scanning for early-phase condition evaluation, supports multiple SPI types per file (legacy `.factories` form), and integrates with the Spring lifecycle.

The whole "add `spring-boot-starter-web` and Tomcat lights up" experience rides on this mechanism — every Spring Boot starter is SPI registrations all the way down.

### The Boot 2.7 → 3.0 → 4 role split

| Concern | Boot ≤ 2.6 | Boot 3 / 4 |
|---|---|---|
| Auto-configuration classes | `spring.factories` under `EnableAutoConfiguration` key | `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` |
| Everything else (SPIs) | `spring.factories` | **Still `spring.factories`** |

The `EnableAutoConfiguration` key was **deprecated in 2.7, silently dropped in 3.0**. Entries placed there compile but never load — no warning. This was the root cause of many "library upgraded but starter no longer fires" bugs during the Boot 3 migration.

### What `spring.factories` actively registers in Boot 4

From `spring-boot-autoconfigure-4.0.6.jar`, six SPIs are still read from `spring.factories`:

| SPI interface | Purpose |
|---|---|
| `AutoConfigurationImportFilter` | Filter applied to auto-config candidates before condition evaluation |
| `AutoConfigurationImportListener` | Notified when auto-configs are imported (audit hook) |
| `BackgroundPreinitializer` | Eager class-loading on a background thread during startup |
| `FailureAnalyzer` | Converts an exception into a user-friendly error report |
| `ApplicationContextInitializer` | Hooks into context refresh before any bean is created |
| `ApplicationListener` | Subscribes to application lifecycle events |

`spring-cloud-config-client-5.0.0.jar` adds four more (these are the ones we traced through in chapter 10):

| SPI interface | Plug-in purpose |
|---|---|
| `ConfigDataLocationResolver` | Adds a new `<scheme>:` prefix to `spring.config.import` (this is how `configserver:` works) |
| `ConfigDataLoader` | Performs the actual fetch for a resolved location |
| `BootstrapRegistryInitializer` | Boot-time registry callbacks (used to register the retry bootstrap) |
| `EnvironmentPostProcessor` | Mutates the `Environment` after preparation, before context creation |

### Decision matrix — where to put what

| If you're writing… | Boot version | File |
|---|---|---|
| Auto-config class | ≤ 2.6 | `spring.factories` (`EnableAutoConfiguration=...`) |
| Auto-config class | 2.7+ | Both supported — `.imports` preferred |
| Auto-config class | 3.x, 4.x | **Only** `META-INF/spring/...AutoConfiguration.imports` |
| `FailureAnalyzer`, `ApplicationListener`, `ApplicationContextInitializer` | any | `META-INF/spring.factories` |
| `ConfigDataLocationResolver`, `ConfigDataLoader` | 2.4+ | `META-INF/spring.factories` |
| `EnvironmentPostProcessor` | any | `META-INF/spring.factories` |
| Pure Java SPI provider | any | `META-INF/services/<interface-fqn>` |

### What this means for `shared-logging-lib`

The current layout is correct for Boot 4:

```
shared-logging-lib/src/main/resources/META-INF/
└── spring/
    └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
        ├── com.kirandev.sharedlogginglib.LoggingAutoConfiguration
        ├── com.kirandev.sharedlogginglib.ServletMdcAutoConfiguration
        └── com.kirandev.sharedlogginglib.KafkaLoggingAutoConfiguration
```

If we ever add (say) a `FailureAnalyzer` to surface a friendly message when Kafka MDC binding fails, it would live in a sibling `spring.factories`:

```
shared-logging-lib/src/main/resources/META-INF/
├── spring.factories                                     ← FailureAnalyzer goes here
└── spring/
    └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

with content:

```properties
org.springframework.boot.diagnostics.FailureAnalyzer=\
  com.kirandev.sharedlogginglib.MdcBindingFailureAnalyzer
```

That's the exact layout `spring-cloud-config-client-5.0.0.jar` uses today.

### Future direction (Boot 3.4+)

A per-SPI `.imports` location is supported: `META-INF/spring/<full-interface-fqn>.imports`. For example, `META-INF/spring/org.springframework.boot.diagnostics.FailureAnalyzer.imports` instead of an entry in `spring.factories`. The long-term direction is one line-oriented file per SPI, mirroring the `AutoConfiguration.imports` pattern. As of Boot 4.0 nothing about the current `spring.factories` usage is wrong or warns — but new libraries can start migrating.

**Why it matters.** Two distinct lessons fall out:

1. **"`spring.factories` was removed in Boot 3" is folklore.** What was removed is the `EnableAutoConfiguration` *key*, not the *file*. The file remains the official home for at least ten other SPI types and shows no sign of going away.
2. **SPI vs `@Bean` is a deliberate design choice about extensibility.** SPI when third parties need to plug in without modifying your code; `@Bean` when registration is internal wiring you control. Spring Boot starters are SPI registrations all the way down — that's why they work without explicit imports in the consuming app.

---

## Final state

### Module layout (logging)

```
shared-logging-lib/
├── pom.xml                                  ← logstash-logback-encoder; spring-kafka (optional)
└── src/main/
    ├── java/com/kirandev/sharedlogginglib/
    │   ├── LoggingMdc.java                  ← MDC key constants
    │   ├── LoggingProperties.java           ← @ConfigurationProperties kirandev.logging.*
    │   ├── LoggingAutoConfiguration.java    ← always-on beans (ServiceMdcInitializer, TaskDecorator)
    │   ├── ServletMdcAutoConfiguration.java ← web-only, @ConditionalOnClass(Filter.class)
    │   ├── KafkaLoggingAutoConfiguration.java ← Kafka-only, @ConditionalOnClass(DefaultKafkaProducerFactory)
    │   ├── MdcFilter.java                   ← servlet filter — W3C/B3 → MDC
    │   ├── MdcTaskDecorator.java            ← TaskExecutor MDC propagation
    │   ├── AsyncMdcConfiguration.java       ← BPP that attaches the decorator
    │   ├── MdcContext.java                  ← utility for non-web flows
    │   ├── ServiceMdcInitializer.java       ← seeds serviceId + deployment.environment
    │   └── kafka/
    │       ├── KafkaMdcProducerInterceptor.java
    │       └── KafkaMdcRecordInterceptor.java
    └── resources/
        ├── application-logging.yaml         ← library defaults imported via spring.config.import
        ├── logback-base.xml                 ← <included> fragment, profile-gated FILE appender
        ├── logback-spring.xml               ← fallback for services that don't ship their own
        └── META-INF/spring/
            └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
                ├── LoggingAutoConfiguration
                ├── ServletMdcAutoConfiguration
                └── KafkaLoggingAutoConfiguration
```

### How a new service onboards

1. Add dependency in its `pom.xml`:
   ```xml
   <dependency>
       <groupId>com.kirandev</groupId>
       <artifactId>shared-logging-lib</artifactId>
   </dependency>
   ```
2. (Optional) add a service-level `src/main/resources/logback-spring.xml`:
   ```xml
   <configuration scan="true">
       <include resource="logback-base.xml"/>
       <logger name="com.yourpackage" level="DEBUG"/>
   </configuration>
   ```
   Skip this entirely if the library defaults are fine.
3. Done. The service inherits MDC propagation, JSON or coloured console per profile, optional rolling file appender (`SPRING_PROFILES_ACTIVE=…,file-logging`), and — if Spring Kafka is on the classpath — automatic Kafka header propagation.

### Runtime matrix

| Profile | Console | File | Kafka MDC | HTTP MDC |
|---|---|---|---|---|
| no profile / default | JSON | – | if Kafka classpath | if servlet classpath |
| `local` | coloured | – | same | same |
| `docker` | JSON | – | same | same |
| `staging` / `prod` / `file-logging` | JSON | `${LOG_PATH}/${APP_NAME}.log` + gzip rotation | same | same |

---

## Reusable engineering lessons

1. **YAML duplicate keys are silent.** Run a YAML parser as a pre-commit check.

2. **Property paths that look framework-bound but aren't are dead code.** When a configuration source has no effect, suspect prefix drift first.

3. **Logback `RollingFileAppender` opens its file at construction.** Always gate IO-touching appenders behind a profile or class-level condition.

4. **`@ConditionalOnWebApplication` on a `@Bean` method cannot save you from sibling reflection traps.** If any bean method's return type references an optional dependency, isolate it into its own auto-config class with a class-level `@ConditionalOnClass`. The framework itself follows this pattern.

5. **`@ConditionalOnClass` is evaluated via ASM bytecode scanning, not the JVM classloader.** This is the only mechanism that lets you reference optional classes safely from an auto-config class.

6. **Container "service is healthy" ≠ cluster is ready.** Per-container healthchecks are a building block; cluster-level invariants (quorum, ISR, controller election) need a separate client-side retry budget.

7. **Two retry configs with similar names is a code-review smell.** This project has `spring.cloud.config.retry.*` (Config Client) and `retry-config.*` (application's domain `RetryTemplate`) — completely unrelated, both called "retry". Document the distinction prominently.

8. **`spring.config.import` with `optional:configserver:` + `spring.cloud.config.fail-fast: true` is the production-safe combo.** `optional:` keeps tests green; `fail-fast` makes production startup hard-fail when the config-server is unreachable.

9. **Library default properties belong in a YAML file at the JAR root, imported via `spring.config.import: "optional:classpath:application-X.yaml"`.** Other approaches (`@PropertySource`, renaming to `application.yaml`, `spring.factories` defaults) all have downsides in Boot 4.

10. **Non-root container users + bind-mounted log paths are a permission landmine.** Bake the writable directory into the image at build time with `chown` to the runtime user. Better still: stay stdout-only and let the orchestrator handle log collection.

11. **The `${ENV_VAR:default}` placeholder pattern in config-server YAML beats per-profile files** for multi-environment configuration. Placeholder resolution happens on the client side, so the same YAML serves localhost, docker, and k8s without forking.

12. **`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` is the contract for auto-configuration in Boot 4.** Always register one auto-config class per cross-cutting concern, each guarded by its own class-level conditions.

13. **`spring.factories` is alive in Boot 4 — but only for non-auto-config SPIs.** The `EnableAutoConfiguration` *key* was silently removed in 3.0; entries placed there now compile but never load. Other SPIs (`FailureAnalyzer`, `ApplicationListener`, `ApplicationContextInitializer`, `EnvironmentPostProcessor`, `ConfigDataLocationResolver`, `ConfigDataLoader`, …) still read from `spring.factories` and have no replacement file. "`spring.factories` was removed in Boot 3" is a common misreading — what was removed is one key in it, not the file itself.

14. **SPI vs `@Bean` is a deliberate design choice about extensibility.** SPI = third parties can plug in without modifying your code (registered via `META-INF/`). `@Bean` = registration tied to a specific configuration class. Choose SPI when the contract should survive without recompilation; choose `@Bean` for internal wiring. Spring Boot starters are SPI all the way down — that is why they activate without explicit imports.

15. **API points outward; SPI points inward.** Your application code calls the API; the framework calls the SPI implementations. Same `interface` keyword in Java; opposite architectural role. Conflating the two leads to libraries that are simultaneously hard to extend (because extension points weren't designed) and hard to use (because consumer-facing types leak provider concerns).

---

*Compiled from the iterative debugging session for `microservices-demo`. Filed for posterity.*
