# Microservices Demo — Docker & Maven Multi-Module Setup Guide

## Table of Contents
1. [Project Structure](#1-project-structure)
2. [Parent POM Issues & Fixes](#2-parent-pom-issues--fixes)
3. [Docker Compose Evolution](#3-docker-compose-evolution)
4. [Buildpacks vs Dockerfile](#4-buildpacks-vs-dockerfile)
5. [Dockerfile Setup](#5-dockerfile-setup)
6. [How Library Modules Are Built](#6-how-library-modules-are-built)
7. [Two Build Approaches](#7-two-build-approaches)
8. [Makefile](#8-makefile)
9. [Docker Concepts Explained](#9-docker-concepts-explained)
10. [Image Size Optimization](#10-image-size-optimization)
11. [Common Errors & Fixes](#11-common-errors--fixes)

---

## 1. Project Structure

```
microservices-demo/
├── pom.xml                             ← parent (Spring Boot 4.0.6, Java 25)
├── app-config-data/                    ← library
├── common-config/                      ← library
├── config-server/                      ← executable (port 8888)
│   └── Dockerfile                      ← Option B: self-contained
├── twitter-to-kafka-service/           ← executable (port 8080)
│   └── Dockerfile                      ← Option B: self-contained
├── kafka/
│   ├── pom.xml                         ← aggregator (packaging: pom)
│   ├── kafka-model/                    ← library
│   ├── kafka-admin/                    ← library
│   └── kafka-producer/                 ← library
├── Dockerfile                          ← Option A: root, full reactor
├── docker-compose.yml                  ← Option A: local dev
├── docker-compose.prod.yml             ← Option B: CI/CD self-contained
└── Makefile
```

---

## 2. Parent POM Issues & Fixes

### Issues Found

| Issue | Impact | Fix |
|---|---|---|
| `config-server` missing from `<modules>` | Root build skips it — no image built | Add `<module>config-server</module>` |
| `spring-boot-maven-plugin` not in `<pluginManagement>` | Inconsistent versions across modules | Move to `<pluginManagement>` |
| `kafka` submodules listed at root AND in kafka/pom.xml | Fragile build order | Declare submodules only in `kafka/pom.xml` |
| `maven.install.skip=false` | Redundant — false is default | Remove or flip to `true` for libraries |
| `spring-cloud-config-server.version` unused property | Dead code | Use it or remove it |
| `java.version=25` | May not be available everywhere | Verify or use Java 21 (LTS) |

### Correct `<modules>` in Parent POM

```xml
<modules>
    <module>app-config-data</module>
    <module>common-config</module>
    <module>config-server</module>
    <module>twitter-to-kafka-service</module>
    <module>kafka</module>   <!-- kafka/pom.xml handles its children -->
</modules>
```

### Correct `kafka/pom.xml`

```xml
<artifactId>kafka</artifactId>
<packaging>pom</packaging>   <!-- required — prevents Maven treating it as a jar -->
<modules>
    <module>kafka-model</module>
    <module>kafka-admin</module>
    <module>kafka-producer</module>
</modules>
```

### `spring-boot-maven-plugin` in Parent — pluginManagement Only

```xml
<build>
    <pluginManagement>
        <!-- version/config management only — does NOT execute on any module -->
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </pluginManagement>

    <plugins>
        <!-- compiler applies to ALL modules — correct place -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
        </plugin>
    </plugins>
</build>
```

### Version Management

`<parent><version>` must be hardcoded — `${project.version}` cannot be used there (chicken-and-egg problem). Use Maven Versions plugin to change versions across all modules:

```bash
mvn versions:set -DnewVersion=1.0.0-SNAPSHOT
mvn versions:commit
```

---

## 3. Docker Compose Evolution

### Issues Fixed in docker-compose.yml

| # | Service | Problem | Fix |
|---|---|---|---|
| 1 | `config-server` | Missing `networks` — unreachable by other services | Added `kafka-net` |
| 2 | `schema-registry` | Wrong broker hostnames (`kafka-broker-*`) | Changed to `kafka-1/2/3` |
| 3 | `twitter-to-kafka-service` | Doesn't wait for schema-registry | Added `schema-registry` to `depends_on` |
| 4 | `version: '3.8'` | Obsolete in Compose v2, causes warnings | Removed |

### Healthcheck Pattern

```yaml
healthcheck:
  test: ["CMD", "curl", "-f", "http://localhost:8888/actuator/health"]
  interval: 10s
  timeout: 5s
  retries: 5
  start_period: 30s   # grace period before checks begin
```

### depends_on with Healthcheck (correct pattern)

```yaml
depends_on:
  config-server:
    condition: service_healthy   # waits for healthcheck, not just container start
```

> `depends_on` without `condition: service_healthy` only waits for the container to **start**, not for the app inside to be **ready**.

---

## 4. Buildpacks vs Dockerfile

### Buildpacks (spring-boot-maven-plugin `build-image`)

**Pros:**
- Zero Dockerfile maintenance
- Automatic best practices — layering, non-root user, memory tuning
- Automatic base image security updates on rebuild

**Cons:**
- Slow — 2-5 minutes per image
- Requires Docker daemon during `mvn install`
- Produces dangling `<none>:<none>` images
- Awkward with multi-module projects
- `layers.xml` schema issues across Spring Boot versions

### Dockerfile

**Pros:**
- Full control over base image and JVM flags
- Fast incremental builds with layer caching
- No Docker daemon needed during Maven build
- Works naturally in any CI/CD pipeline

**Cons:**
- You maintain the Dockerfile
- Easy to write insecure Dockerfiles if not careful

### When to Pick Each

| Situation | Pick |
|---|---|
| Simple single-module app, no Docker knowledge | Buildpacks |
| Multi-module project | Dockerfile |
| CI/CD pipeline | Dockerfile |
| Kubernetes / production | Dockerfile |
| Fast local iteration | Dockerfile |

### Cleaning Dangling Buildpack Images

```bash
docker image prune -f                        # remove all dangling images
docker images -f "dangling=true"             # list dangling images first
mvn clean install && docker image prune -f   # auto-prune after build
```

---

## 5. Dockerfile Setup

### Key Design Decisions

| Decision | Reason |
|---|---|
| `maven:3.9-eclipse-temurin-25` for builder | JDK + Maven bundled — `eclipse-temurin` alone has no `mvn` |
| `eclipse-temurin:25-jre-alpine` for runtime | ~90MB vs ~250MB for Ubuntu JRE |
| Non-root user (`appuser`) | Security — limits blast radius if app is exploited |
| `-XX:+UseContainerSupport` | JVM respects Docker memory limits, not host RAM |
| `-XX:MaxRAMPercentage=75.0` | Uses 75% of container RAM, leaves 25% for OS/GC |
| `-Djava.security.egd=file:/dev/./urandom` | Non-blocking random — faster Spring Boot startup |
| `-Dspring-boot.build-image.skip=true` | Prevents Buildpacks running inside Docker (no daemon available) |
| POMs copied before source | Docker layer cache — source changes don't re-download dependencies |
| `--no-transfer-progress` | Clean build logs in CI |

### Runtime Stage Template

```dockerfile
FROM eclipse-temurin:25-jre-alpine AS config-server

# Alpine user creation (different from Ubuntu)
RUN addgroup -S appgroup && adduser -S -G appgroup appuser

WORKDIR /app
COPY --from=builder /build/config-server/target/*.jar app.jar
RUN chown appuser:appgroup app.jar
USER appuser
EXPOSE 8888

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
```

---

## 6. How Library Modules Are Built

### Maven Reactor Build Order

When `mvn clean package` runs at root, Maven resolves the dependency graph and builds in order:

```
1. app-config-data      → compiled → installed to /root/.m2
2. common-config        → compiled → installed to /root/.m2
3. kafka/kafka-model    → compiled → installed to /root/.m2
4. kafka/kafka-admin    → compiled → installed to /root/.m2
5. kafka/kafka-producer → compiled → installed to /root/.m2
6. config-server        → finds libraries in .m2 ✅ → fat jar built
7. twitter-to-kafka-service → finds all libraries in .m2 ✅ → fat jar built
```

### What's Inside the Fat Jar

```
twitter-to-kafka-service.jar
├── BOOT-INF/
│   ├── classes/          ← your service's own compiled classes
│   └── lib/
│       ├── app-config-data-0.0.1-SNAPSHOT.jar    ← library module bundled
│       ├── kafka-admin-0.0.1-SNAPSHOT.jar         ← library module bundled
│       ├── kafka-producer-0.0.1-SNAPSHOT.jar      ← library module bundled
│       └── spring-boot-*.jar, kafka-*.jar ...     ← third party deps
├── META-INF/
└── org/springframework/boot/loader/
```

Only the fat jar crosses from builder to runtime stage — no source code, no Maven, no other modules.

---

## 7. Two Build Approaches

### Option A — Local Dev (Root Dockerfile + `.m2` cache mount)

**Flow:**
```bash
make install    # mvn install populates local .m2
make dev-up     # Docker build mounts .m2 cache → fast rebuild
```

**Key Dockerfile feature:**
```dockerfile
RUN --mount=type=cache,target=/root/.m2 \
    mvn clean package -DskipTests -B --no-transfer-progress \
    -Dspring-boot.build-image.skip=true
```

`--mount=type=cache` — BuildKit persists `/root/.m2` between builds without baking it into the image layer. Dependencies only download once.

> ⚠️ Requires `DOCKER_BUILDKIT=1` — without it the mount flag is silently ignored and Maven redownloads everything each build.

### Option B — Self-contained / CI-CD (Per-module Dockerfiles)

**Flow:**
```bash
make prod-up    # everything built inside Docker, zero prerequisites
```

**Key Maven flags:**
```dockerfile
RUN mvn clean package -DskipTests -B --no-transfer-progress \
    -Dspring-boot.build-image.skip=true \
    -pl config-server -am
#   ↑ target module only    ↑ also build its library dependencies
```

`-pl` = project list (only build this module)
`-am` = also make (build dependency modules first)

### Comparison

| | Option A (Local Dev) | Option B (Self-contained) |
|---|---|---|
| Prerequisites | `mvn install` first | None — Docker only |
| Build speed | Fast (cache mount) | Slower first build |
| CI/CD | Needs Maven on agent | Only Docker needed |
| Portability | Depends on local `.m2` | Fully reproducible |
| Risk | Stale `.m2` can cause bugs | Always clean build |
| Recommended for | Local dev iteration | CI/CD, production |

---

## 8. Makefile

### What is a Makefile?

A file that defines shortcut commands (targets) run with `make <target>`. A general-purpose task runner — not Maven or Docker specific.

### `.PHONY` Declaration

Tells `make` that targets are not actual files — prevents `make` skipping a command if a file/folder with the same name exists in the project.

```makefile
.PHONY: install dev-up dev-down prod-up prod-down clean
```

### Workflow Commands

```bash
# Option A — Local Dev
make install        # mvn install → populates .m2
make dev-up         # build images + start all services
make dev-down       # stop all services
make dev-restart    # rebuild + restart app services only

# Option B — CI/CD
make prod-up        # self-contained build + start all services
make prod-down      # stop all services
make prod-restart   # rebuild + restart app services only

# Utilities
make logs SERVICE=config-server   # tail specific service logs
make prune                        # remove dangling images
make clean                        # full reset including Kafka volumes
```

---

## 9. Docker Concepts Explained

### `--build` flag

```bash
docker compose up --build -d
```

Always rebuilds images before starting containers — even if image already exists. Without it, Docker uses the existing image and misses code changes. Docker layer cache still applies so unchanged layers are fast.

### `cache_from`

```yaml
cache_from:
  - com.kirandev/config-server:0.0.1-SNAPSHOT
```

Tells Docker where to look for cached layers before building. Most useful in CI/CD pipelines — pull previous build from registry as cache source so only changed layers rebuild. On local machine, BuildKit's own cache already handles this.

### `EXPOSE`

Documents which port the container listens on. Does NOT open the port — that's done by `ports:` in docker-compose. Metadata only.

### Multi-stage Build Summary

```
Stage 1 (builder):    maven:3.9-eclipse-temurin-25    ~1.2GB
                      All source, all modules, Maven
                      ↓ only fat jar crosses over
Stage 2 (runtime):    eclipse-temurin:25-jre-alpine   ~200-300MB
                      JRE + app.jar only
```

### `-Dspring-boot.build-image.skip=true`

Skips Buildpacks during `mvn package`. Required inside Dockerfile builds because there is no Docker daemon available inside a Docker build container.

---

## 10. Image Size Optimization

### Why ~800MB for a Simple App

```
eclipse-temurin:25-jre base    ~250MB
Spring Boot fat jar            ~150MB
Spring Cloud, Kafka libs       ~100MB
Avro, Confluent libs           ~100MB
OS layers (Ubuntu)             ~200MB
────────────────────────────────────
Total                          ~800MB
```

### Size Reduction Options

| Approach | Size | Effort |
|---|---|---|
| Ubuntu JRE (default) | ~800MB | none |
| Alpine JRE | ~600MB | 1 line change |
| Alpine + layered jar | ~550MB | small |
| Custom `jlink` JRE | ~200-300MB | medium |
| GraalVM native image | ~80-100MB | high |

### Alpine vs Ubuntu JRE

| | Ubuntu JRE | Alpine JRE |
|---|---|---|
| Size | ~250MB | ~90MB |
| C library | glibc | musl libc |
| Shell | bash | sh only |
| Package manager | apt | apk |
| User commands | `groupadd` / `useradd` | `addgroup` / `adduser` |
| Compatibility | Widest | Fine for pure Java |

> Alpine uses `musl libc` instead of `glibc`. Pure Java apps work fine. JNI native libraries may need testing.

---

## 11. Common Errors & Fixes

### `mvn: not found`

```
/bin/sh: 1: mvn: not found
```

**Cause:** Using `eclipse-temurin:25-jdk` as builder — only has JDK, no Maven.

**Fix:**
```dockerfile
# ❌
FROM eclipse-temurin:25-jdk AS builder

# ✅
FROM maven:3.9-eclipse-temurin-25 AS builder
```

---

### `groupadd: not found`

```
/bin/sh: groupadd: not found
```

**Cause:** Using Alpine image — `groupadd`/`useradd` are Ubuntu commands.

**Fix:**
```dockerfile
# ❌ Ubuntu syntax
RUN groupadd --system appgroup && useradd --system --gid appgroup appuser

# ✅ Alpine syntax
RUN addgroup -S appgroup && adduser -S -G appgroup appuser
```

---

### `layers.xml` Schema Error

```
Failed to process custom layers configuration: Invalid layers.xml configuration:
cvc-elt.1.a: Cannot find the declaration of element 'layers'
```

**Cause:** `layers.xml` schema changed in Spring Boot 4.x, or file not found at expected path.

**Fix:** Remove the `<layers>` configuration block entirely — Spring Boot's default layering is sufficient for most projects:

```xml
<!-- Remove this from spring-boot-maven-plugin config -->
<layers>
    <enabled>true</enabled>
    <configuration>...</configuration>
</layers>
```

---

### `build-image` Not Running at Root Level

**Cause:** Module missing from `<modules>` in parent POM, or `repackage` not running before `build-image`.

**Fix:**
```xml
<!-- Ensure module is in parent pom.xml <modules> -->
<module>config-server</module>

<!-- Ensure explicit execution order in module pom.xml -->
<execution>
    <id>repackage</id>
    <phase>package</phase>
    <goals><goal>repackage</goal></goals>
</execution>
<execution>
    <id>build-image</id>
    <phase>install</phase>
    <goals><goal>build-image</goal></goals>
</execution>
```

---

### `--mount=type=cache` Silently Ignored

**Cause:** BuildKit not enabled.

**Fix:**
```bash
# Prefix every build command
DOCKER_BUILDKIT=1 docker compose up --build -d

# Or enable permanently in ~/.docker/daemon.json
{
  "features": { "buildkit": true }
}
```

---

### `depends_on` Not Waiting for App Readiness

**Cause:** `depends_on` without `condition` only waits for container start, not app readiness.

**Fix:**
```yaml
depends_on:
  config-server:
    condition: service_healthy   # waits for healthcheck to pass
```

Requires `healthcheck` defined on the dependency service and `spring-boot-actuator` on the classpath.
