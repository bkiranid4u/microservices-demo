# =============================================================================
# ROOT Dockerfile — Option A (Local Dev)
# Usage: Run 'make install' first to populate .m2, then 'make dev-up'
# =============================================================================

# =============================================================================
# Stage 1: Build using cached local .m2
# =============================================================================
FROM maven:3.9-eclipse-temurin-25 AS builder
WORKDIR /build

# Parent POM
COPY pom.xml .

# All module POMs (layer cache: only invalidated if a POM changes)
COPY shared-logging-lib/pom.xml         shared-logging-lib/
COPY app-config-data/pom.xml            app-config-data/
COPY common-config/pom.xml              common-config/
COPY config-server/pom.xml              config-server/
COPY twitter-to-kafka-service/pom.xml   twitter-to-kafka-service/
COPY kafka/pom.xml                      kafka/
COPY kafka/kafka-model/pom.xml          kafka/kafka-model/
COPY kafka/kafka-admin/pom.xml          kafka/kafka-admin/
COPY kafka/kafka-producer/pom.xml       kafka/kafka-producer/

# Resolve dependencies — reuses mounted .m2 cache, no re-download
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -B --no-transfer-progress

# All source code
COPY shared-logging-lib/src         shared-logging-lib/src
COPY app-config-data/src            app-config-data/src
COPY common-config/src              common-config/src
COPY config-server/src              config-server/src
COPY twitter-to-kafka-service/src   twitter-to-kafka-service/src
COPY kafka/kafka-model/src          kafka/kafka-model/src
COPY kafka/kafka-admin/src          kafka/kafka-admin/src
COPY kafka/kafka-producer/src       kafka/kafka-producer/src

# Build entire reactor — .m2 cache reused from mount
RUN --mount=type=cache,target=/root/.m2 \
    mvn clean package -DskipTests -B --no-transfer-progress \
    -Dspring-boot.build-image.skip=true


# =============================================================================
# Stage 2: config-server runtime (With Baked-In Git Repo)
# =============================================================================
FROM eclipse-temurin:25-jre-alpine AS config-server

# ✅ 1. Install git (Required because Spring Cloud Config Server relies on local Git/JGit execution)
RUN apk add --no-cache git curl

# Create the dedicated non-root user matching your environment uid/gid profile
RUN addgroup -S -g 1000 appgroup && adduser -S -u 1000 -G appgroup appuser

WORKDIR /app

# ✅ 2. Pre-create the local directory structure matching your required file:/// URI
RUN mkdir -p /home/appuser/devspace/microservices-demo/config-server-repository

# ✅ 3. Copy the configuration directory from your project context
# (Assumes config-server-repository folder is present at your project root workspace context)
COPY config-server-repository/ /home/appuser/devspace/microservices-demo/config-server-repository/

# Copy the built jar executable artifact from the builder stage
COPY --from=builder /build/config-server/target/*.jar app.jar

# Pre-create a writable log directory for the file-logging Spring profile (no-op otherwise)
RUN mkdir -p /app/logs/archived

# ✅ 4. Assign complete ownership of the home space and app folder to appuser
RUN chown -R appuser:appgroup /home/appuser /app

USER appuser
EXPOSE 8888

# ✅ 5. Added system property to point Spring Cloud Config Server directly to the baked repository
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dspring.cloud.config.server.git.uri=file:///home/appuser/devspace/microservices-demo/config-server-repository", \
    "-jar", "app.jar"]


# =============================================================================
# Stage 3: twitter-to-kafka-service runtime
# =============================================================================
FROM eclipse-temurin:25-jre-alpine AS twitter-to-kafka-service

RUN addgroup -S appgroup && adduser -S -G appgroup appuser
WORKDIR /app
COPY --from=builder /build/twitter-to-kafka-service/target/*.jar app.jar
# Pre-create a writable log directory for the file-logging Spring profile (no-op otherwise)
RUN mkdir -p /app/logs/archived && chown -R appuser:appgroup /app
USER appuser
EXPOSE 8080

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
