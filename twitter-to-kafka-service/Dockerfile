# =============================================================================
# twitter-to-kafka-service/Dockerfile — Option B (Self-contained)
# Usage: docker build -f twitter-to-kafka-service/Dockerfile -t com.kirandev/twitter.to.kafka.service:0.0.1-SNAPSHOT .
# Context: must be run from project ROOT so all module sources are available
# =============================================================================

# =============================================================================
# Stage 1: Build only twitter-to-kafka-service and its library dependencies
# =============================================================================
FROM maven:3.9-eclipse-temurin-25 AS builder
WORKDIR /build

# --- Parent POM ---
COPY pom.xml .

# --- Only the POMs for twitter-to-kafka-service and its library dependencies ---
# Direct library deps: app-config-data, kafka-admin, kafka-producer
# kafka-admin and kafka-producer live under kafka/ aggregator
COPY app-config-data/pom.xml                    app-config-data/
COPY common-config/pom.xml                      common-config/
COPY config-server/pom.xml                      config-server/
COPY kafka/pom.xml                              kafka/
COPY kafka/kafka-model/pom.xml                  kafka/kafka-model/
COPY kafka/kafka-admin/pom.xml                  kafka/kafka-admin/
COPY kafka/kafka-producer/pom.xml               kafka/kafka-producer/
COPY twitter-to-kafka-service/pom.xml           twitter-to-kafka-service/

# --- Download dependencies for these modules only ---
RUN mvn dependency:go-offline -B --no-transfer-progress \
    -pl twitter-to-kafka-service -am

# --- Copy only the source needed ---
COPY app-config-data/src                app-config-data/src
COPY common-config/src                  common-config/src
COPY kafka/kafka-model/src              kafka/kafka-model/src
COPY kafka/kafka-admin/src              kafka/kafka-admin/src
COPY kafka/kafka-producer/src           kafka/kafka-producer/src
COPY twitter-to-kafka-service/src       twitter-to-kafka-service/src

# --- Build twitter-to-kafka-service and its libraries ---
# -pl twitter-to-kafka-service: target module
# -am: also compile all library dependencies first
RUN mvn clean package -DskipTests -B --no-transfer-progress \
    -Dspring-boot.build-image.skip=true \
    -pl twitter-to-kafka-service -am


# =============================================================================
# Stage 2: Lean runtime image
# =============================================================================
FROM eclipse-temurin:25-jre-alpine

RUN addgroup -S appgroup && adduser -S -G appgroup appuser

WORKDIR /app

COPY --from=builder /build/twitter-to-kafka-service/target/*.jar app.jar

RUN mkdir -p /app/twitter-to-kafka-service/logs \
    && chown -R appuser:appgroup /app

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]