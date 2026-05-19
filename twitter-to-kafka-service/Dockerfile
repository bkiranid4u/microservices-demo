## ─────────────────────────────────────────
## Stage 1: Build
## Spring Boot 4 + Java 25 ready
## ─────────────────────────────────────────
#FROM maven:3.9.16-eclipse-temurin-25-alpine AS builder
#
#WORKDIR /build
#
## Cache dependencies first (only re-runs if pom.xml changes)
#COPY pom.xml .
#RUN mvn dependency:go-offline -B
#
## Copy source and build
#COPY src ./src
#RUN mvn clean package -DskipTests -B
#
## ─────────────────────────────────────────
## Stage 2: Runtime
## eclipse-temurin:25-jre (Ubuntu Noble — no Alpine JRE for Java 25)
## ─────────────────────────────────────────
#FROM eclipse-temurin:25-jre
#
## Add a non-root user for security
#RUN groupadd -r appgroup && useradd -r -g appgroup appuser
#
#WORKDIR /app
#
## Copy the built jar from the builder stage
#COPY --from=builder /build/target/twitter-to-kafka-service-*.jar app.jar
#
## Set ownership
#RUN chown -R appuser:appgroup /app
#
#USER appuser
#
## Expose the application port
#EXPOSE 8080
#
## JVM tuning for containers
## Java 25: ZGC is production-ready and preferred for low-latency workloads
## Virtual threads (Project Loom) are stable — no extra flags needed
#ENV JAVA_OPTS="-XX:+UseContainerSupport \
#               -XX:MaxRAMPercentage=75.0 \
#               -XX:+UseZGC \
#               -XX:+ZGenerational \
#               -Djava.security.egd=file:/dev/./urandom"
#
#ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]