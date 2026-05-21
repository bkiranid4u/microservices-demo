# =============================================================================
# config-server/Dockerfile — Option B (Self-contained)
# Usage: docker build -f config-server/Dockerfile -t com.kirandev/config-server:0.0.1-SNAPSHOT .
# Context: must be run from project ROOT so all module sources are available
# =============================================================================

# =============================================================================
# Stage 1: Build only config-server and its library dependencies
# =============================================================================
FROM maven:3.9-eclipse-temurin-25 AS builder
WORKDIR /build

# --- Parent POM ---
COPY pom.xml .

# --- Only the POMs for config-server and its library dependencies ---
# (app-config-data and common-config are direct dependencies of config-server)
COPY app-config-data/pom.xml    app-config-data/
COPY common-config/pom.xml      common-config/
COPY config-server/pom.xml      config-server/

# --- Download dependencies for these modules only ---
# -pl: only these modules  -am: also build dependency modules (--also-make)
RUN mvn dependency:go-offline -B --no-transfer-progress \
    -pl config-server -am

# --- Copy only the source needed ---
COPY app-config-data/src    app-config-data/src
COPY common-config/src      common-config/src
COPY config-server/src      config-server/src

# --- Build config-server and its libraries ---
# -pl config-server: target module
# -am: also compile app-config-data, common-config first
RUN mvn clean package -DskipTests -B --no-transfer-progress \
    -Dspring-boot.build-image.skip=true \
    -pl config-server -am


# =============================================================================
# Stage 2: Lean runtime image with baked-in config repository
# =============================================================================
FROM eclipse-temurin:25-jre-alpine

# ✅ 1. Install git (Required because Spring Cloud Config Server relies on git/JGit execution)
RUN apk add --no-cache git

# Create the dedicated non-root user matching your environment uid/gid profile
RUN addgroup -S -g 1000 appgroup && adduser -S -u 1000 -G appgroup appuser

WORKDIR /app

# ✅ 2. Pre-create the directory matching your exact target local absolute file URI path
RUN mkdir -p /home/appuser/devspace/microservices-demo/config-server-repository

# ✅ 3. Copy the configuration directory from your project context
# (Assumes config-server-repository folder is present at your project root workspace context)
COPY config-server-repository/ /home/appuser/devspace/microservices-demo/config-server-repository/

# Copy the built jar executable artifact from the builder stage
COPY --from=builder /build/config-server/target/*.jar app.jar

# ✅ 4. Assign complete ownership of the home space and app folder to appuser
RUN chown -R appuser:appgroup /home/appuser /app

USER appuser

EXPOSE 8888

# ✅ 5. Added system property to explicitly default the active git folder path matching the bake target
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dspring.cloud.config.server.git.uri=file:///home/appuser/devspace/microservices-demo/config-server-repository", \
    "-jar", "app.jar"]
