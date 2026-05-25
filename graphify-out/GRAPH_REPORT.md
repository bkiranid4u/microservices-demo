# Graph Report - .  (2026-05-25)

## Corpus Check
- Corpus is ~16,553 words - fits in a single context window. You may not need a graph.

## Summary
- 312 nodes · 350 edges · 48 communities (16 shown, 32 thin omitted)
- Extraction: 82% EXTRACTED · 18% INFERRED · 0% AMBIGUOUS · INFERRED: 63 edges (avg confidence: 0.84)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- [[_COMMUNITY_Docker & Infra Config|Docker & Infra Config]]
- [[_COMMUNITY_Avro Model Decode|Avro Model Decode]]
- [[_COMMUNITY_Spring Auto-Config Concepts|Spring Auto-Config Concepts]]
- [[_COMMUNITY_Avro Model Builder|Avro Model Builder]]
- [[_COMMUNITY_Application MDC Init|Application MDC Init]]
- [[_COMMUNITY_Stream Initialization|Stream Initialization]]
- [[_COMMUNITY_Kafka Admin Client|Kafka Admin Client]]
- [[_COMMUNITY_Logging Properties|Logging Properties]]
- [[_COMMUNITY_Web Client Config|Web Client Config]]
- [[_COMMUNITY_Kafka MDC Consumer|Kafka MDC Consumer]]
- [[_COMMUNITY_Kafka Admin & Retry|Kafka Admin & Retry]]
- [[_COMMUNITY_Distributed Trace Context|Distributed Trace Context]]
- [[_COMMUNITY_Stream Runner Strategies|Stream Runner Strategies]]
- [[_COMMUNITY_Kafka Producer Interceptor|Kafka Producer Interceptor]]
- [[_COMMUNITY_Kafka Producer Config|Kafka Producer Config]]
- [[_COMMUNITY_Domain Exceptions|Domain Exceptions]]
- [[_COMMUNITY_HTTP MDC Filter|HTTP MDC Filter]]
- [[_COMMUNITY_Kafka Logging Auto-Config|Kafka Logging Auto-Config]]
- [[_COMMUNITY_Retry Configuration|Retry Configuration]]
- [[_COMMUNITY_Twitter Kafka Producer|Twitter Kafka Producer]]
- [[_COMMUNITY_Kafka Admin Config|Kafka Admin Config]]
- [[_COMMUNITY_Kafka Logging Config|Kafka Logging Config]]
- [[_COMMUNITY_Logging Auto-Config|Logging Auto-Config]]
- [[_COMMUNITY_Servlet MDC Config|Servlet MDC Config]]
- [[_COMMUNITY_MDC Task Decorator|MDC Task Decorator]]
- [[_COMMUNITY_Claude Code Settings|Claude Code Settings]]
- [[_COMMUNITY_Config Server App|Config Server App]]
- [[_COMMUNITY_Stream Initializer|Stream Initializer]]
- [[_COMMUNITY_Kafka Producer Contract|Kafka Producer Contract]]
- [[_COMMUNITY_Stream Runner Interface|Stream Runner Interface]]
- [[_COMMUNITY_Kafka Producer Interface|Kafka Producer Interface]]
- [[_COMMUNITY_Logging MDC Constants|Logging MDC Constants]]
- [[_COMMUNITY_Async MDC Config|Async MDC Config]]
- [[_COMMUNITY_Logging Tests|Logging Tests]]
- [[_COMMUNITY_Retry Config Data|Retry Config Data]]
- [[_COMMUNITY_Twitter Config Data|Twitter Config Data]]
- [[_COMMUNITY_Kafka Config Data|Kafka Config Data]]
- [[_COMMUNITY_Kafka Producer Config Data|Kafka Producer Config Data]]
- [[_COMMUNITY_Twitter Service Config|Twitter Service Config]]
- [[_COMMUNITY_Config Server|Config Server]]
- [[_COMMUNITY_Kafka Network|Kafka Network]]
- [[_COMMUNITY_Build Strategy|Build Strategy]]
- [[_COMMUNITY_App Bridge Network|App Bridge Network]]

## God Nodes (most connected - your core abstractions)
1. `TwitterAvroModel` - 28 edges
2. `Builder` - 19 edges
3. `KafkaAdminClient` - 12 edges
4. `MdcContext` - 10 edges
5. `LoggingProperties` - 9 edges
6. `KafkaMdcRecordInterceptor` - 8 edges
7. `twitter-to-kafka-service Docker Service` - 8 edges
8. `KafkaMdcProducerInterceptor` - 7 edges
9. `kafka-1 Broker Service` - 7 edges
10. `KafkaProducerConfig` - 6 edges

## Surprising Connections (you probably didn't know these)
- `TwitterToKafkaServiceException` --conceptually_related_to--> `twitter-to-kafka-service Docker Service`  [INFERRED]
  twitter-to-kafka-service/src/main/java/com/kirandev/exception/TwitterToKafkaServiceException.java → docker-compose.yaml
- `kafka-1 Broker Service` --semantically_similar_to--> `kafka-broker-1 (kafka_cluster.yml)`  [INFERRED] [semantically similar]
  docker-compose.yaml → docker-compose/kafka_cluster.yml
- `config-server Docker Service` --semantically_similar_to--> `config-server Prod Docker Service`  [INFERRED] [semantically similar]
  docker-compose.yaml → docker-compose.prod.yml
- `application-logging.yaml (Library Defaults)` --semantically_similar_to--> `config-server-repository/application.yml (Global Defaults)`  [INFERRED] [semantically similar]
  shared-logging-lib/src/main/resources/application-logging.yaml → config-server-repository/application.yml
- `Exponential Backoff Retry Pattern` --rationale_for--> `KafkaAdminClient`  [INFERRED]
  common-config/src/main/java/com/kirandev/common/config/RetryConfig.java → kafka/kafka-admin/src/main/java/com/kirandev/kafka/admin/clients/KafkaAdminClient.java

## Hyperedges (group relationships)
- **Kafka Config Data shared across Admin, Producer, and Client** — configdata_kafkaconfigdata, config_kafkaadminconfig, clients_kafkaadminclient, config_kafkaproducerconfig [EXTRACTED 1.00]
- **MDC Propagation Layer: Filter, TaskDecorator, MdcContext, KafkaAutoConfig** — sharedlogginglib_mdcfilter, sharedlogginglib_mdctaskdecorator, sharedlogginglib_mdccontext, sharedlogginglib_kafkaloggingautoconfiguration [INFERRED 0.95]
- **Retry Config Data consumed by RetryConfig bean and KafkaAdminClient** — config_retryconfigdata, config_retryconfig, clients_kafkaadminclient [EXTRACTED 1.00]
- **Kafka MDC Trace Propagation Pipeline** — kafka_kafkamdcproducerinterceptor, kafka_kafkamdcrecordinterceptor, sharedlogginglib_loggingmdc [EXTRACTED 0.95]
- **Stream Runner Strategy Pattern** — runner_streamrunner, impl_mockkafkastreamrunner, impl_twitterkafkastreamrunner, impl_blueskykafkastreamrunner [EXTRACTED 0.95]
- **Twitter-to-Kafka Ingestion Pipeline** — impl_mockkafkastreamrunner, listner_twitterkafkastatuslistener, transformer_twittermsgtoavroschematransformer [EXTRACTED 0.95]
- **KRaft Kafka Cluster Formation (3 Nodes + Schema Registry + Control Center)** — docker_compose_kafka_1, docker_compose_kafka_2, docker_compose_kafka_3, docker_compose_schema_registry, docker_compose_control_center [EXTRACTED 1.00]
- **Spring Cloud Config Bootstrap Flow** — twitter_resources_application_yaml, docs_journey_configdata_spi, config_repo_application_yml, docker_compose_config_server_service [INFERRED 0.85]
- **Shared Logging Lib Auto-Config Trio** — docs_journey_chapter1_logging_pattern, docs_journey_chapter11_noclassdeffounderror, concept_asm_conditional_on_class [EXTRACTED 1.00]

## Communities (48 total, 32 thin omitted)

### Community 0 - "Docker & Infra Config"
Cohesion: 0.12
Nodes (25): kafka-config.* ConfigurationProperties Prefix, config-server Docker Service, Confluent Control Center Service, kafka-1 Broker Service, kafka-2 Broker Service, kafka-3 Broker Service, KRaft Mode (No Zookeeper), config-server Prod Docker Service (+17 more)

### Community 2 - "Spring Auto-Config Concepts"
Cohesion: 0.11
Nodes (21): ASM Bytecode Scanning for @ConditionalOnClass, file-logging Spring Profile, optional:classpath: Config Import Directive, config-server-repository/application.yml (Global Defaults), config-server-repository/config-server.yml, config-server-repository/logging-common.yml, config-server application.yaml, NoClassDefFoundError jakarta.servlet.Filter Auto-config Bug (+13 more)

### Community 4 - "Application MDC Init"
Cohesion: 0.17
Nodes (3): ApplicationRunner, MdcContext, ServiceMdcInitializer

### Community 5 - "Stream Initialization"
Cohesion: 0.17
Nodes (6): CommandLineRunner, KafkaStreamInitializer, MockKafkaStreamRunner, TwitterToKafkaServiceApplication, StreamInitializer, StreamRunner

### Community 8 - "Web Client Config"
Cohesion: 0.17
Nodes (4): WebClientConfig, TwitterKafkaStatusListener, StatusAdapter, TwitterMsgtoAvroSchemaTransformer

### Community 10 - "Kafka Admin & Retry"
Cohesion: 0.27
Nodes (10): KafkaAdminClient, Exponential Backoff Retry Pattern, KafkaAdminConfig, KafkaProducerConfig, RetryConfig, RetryConfigData, WebClientConfig, KafkaConfigData (+2 more)

### Community 11 - "Distributed Trace Context"
Cohesion: 0.24
Nodes (10): MDC Propagation Pattern, W3C/B3 Trace Context Propagation over Kafka, KafkaMdcProducerInterceptor, KafkaMdcRecordInterceptor, AsyncMdcConfiguration, LoggingAutoConfiguration, LoggingMdc, LoggingProperties (+2 more)

### Community 12 - "Stream Runner Strategies"
Cohesion: 0.36
Nodes (9): BlueskyKafkaStreamRunner (commented out), KafkaStreamInitializer, MockKafkaStreamRunner, TwitterKafkaStreamRunner (commented out), StreamInitializer, TwitterToKafkaServiceApplication, TwitterKafkaStatusListener, StreamRunner (+1 more)

### Community 15 - "Domain Exceptions"
Cohesion: 0.29
Nodes (3): KafkaClientException, TwitterToKafkaServiceException, RuntimeException

### Community 17 - "Kafka Logging Auto-Config"
Cohesion: 0.47
Nodes (6): MDC Correlation Context Propagation, KafkaLoggingAutoConfiguration, LoggingAutoConfigurationTest, MdcContext, MdcFilter, MdcTaskDecorator

### Community 28 - "Kafka Producer Contract"
Cohesion: 0.67
Nodes (3): TwitterAvroModel, KafkaProducer (interface), TwitterKafkaProducer

## Knowledge Gaps
- **24 isolated node(s):** `RetryConfigData`, `KafkaProducerConfigData`, `TwitterToKafkaServiceConfigData`, `KafkaConfigData`, `PreToolUse` (+19 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **32 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `TwitterAvroModel` connect `Avro Model Decode` to `Web Client Config`, `Kafka MDC Consumer`, `Avro Model Builder`, `Kafka Admin Client`?**
  _High betweenness centrality (0.094) - this node is a cross-community bridge._
- **Why does `Builder` connect `Avro Model Builder` to `Web Client Config`?**
  _High betweenness centrality (0.045) - this node is a cross-community bridge._
- **What connects `RetryConfigData`, `KafkaProducerConfigData`, `TwitterToKafkaServiceConfigData` to the rest of the system?**
  _31 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Docker & Infra Config` be split into smaller, more focused modules?**
  _Cohesion score 0.11666666666666667 - nodes in this community are weakly interconnected._
- **Should `Avro Model Decode` be split into smaller, more focused modules?**
  _Cohesion score 0.08666666666666667 - nodes in this community are weakly interconnected._
- **Should `Spring Auto-Config Concepts` be split into smaller, more focused modules?**
  _Cohesion score 0.11428571428571428 - nodes in this community are weakly interconnected._
- **Should `Avro Model Builder` be split into smaller, more focused modules?**
  _Cohesion score 0.10526315789473684 - nodes in this community are weakly interconnected._