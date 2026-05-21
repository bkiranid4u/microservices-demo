# =============================================================================
# Makefile — microservices-demo
# =============================================================================
# Option A (dev):  make install → make dev-up
# Option B (prod): make prod-up  (no Maven needed)
# =============================================================================

.PHONY: install dev-build dev-up dev-down dev-restart \
        prod-build prod-up prod-down prod-restart \
        clean prune logs help

# ── Option A: Local Dev Workflow ─────────────────────────────────────────────

## Install all modules to local .m2 (prerequisite for dev-up)
install:
	mvn clean install -DskipTests -Dspring-boot.build-image.skip=true

## Build Docker images using root Dockerfile + local .m2 cache (Option A)
dev-build:
	DOCKER_BUILDKIT=1 docker compose build

## Start all services (Option A) — run 'make install' first
dev-up:
	DOCKER_BUILDKIT=1 docker compose up --build -d

## Stop all services (Option A)
dev-down:
	docker compose down

## Restart app services only — skips Kafka/Schema Registry restart
dev-restart:
	docker compose up --build -d config-server twitter-to-kafka-service


# ── Option B: Self-contained / CI-CD Workflow ────────────────────────────────

## Build Docker images using per-module Dockerfiles (Option B — no Maven needed)
prod-build:
	DOCKER_BUILDKIT=1 docker compose -f docker-compose.prod.yml build

## Start all services using self-contained builds (Option B)
prod-up:
	DOCKER_BUILDKIT=1 docker compose -f docker-compose.prod.yml up --build -d

## Stop all services (Option B)
prod-down:
	docker compose -f docker-compose.prod.yml down

## Restart app services only (Option B)
prod-restart:
	docker compose -f docker-compose.prod.yml up --build -d config-server twitter-to-kafka-service


# ── Utilities ────────────────────────────────────────────────────────────────

## Tail logs for all services (use SERVICE=config-server for a specific one)
logs:
	docker compose logs -f $(SERVICE)

## Remove stopped containers and dangling images
prune:
	docker compose down
	docker image prune -f

## Full reset — removes containers, volumes (wipes Kafka data), and images
clean:
	docker compose down -v --remove-orphans
	docker compose -f docker-compose.prod.yml down -v --remove-orphans
	docker image prune -f

## Show this help
help:
	@echo ""
	@echo "  LOCAL DEV (Option A):"
	@echo "    make install       Install all modules to local .m2"
	@echo "    make dev-up        Build images + start all services"
	@echo "    make dev-down      Stop all services"
	@echo "    make dev-restart   Rebuild + restart app services only"
	@echo ""
	@echo "  CI/CD (Option B):"
	@echo "    make prod-up       Build self-contained images + start all services"
	@echo "    make prod-down     Stop all services"
	@echo "    make prod-restart  Rebuild + restart app services only"
	@echo ""
	@echo "  UTILITIES:"
	@echo "    make logs          Tail all logs  (SERVICE=x for specific)"
	@echo "    make prune         Remove dangling images"
	@echo "    make clean         Full reset including volumes"
	@echo ""