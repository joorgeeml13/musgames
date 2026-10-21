.DEFAULT_GOAL := help

COMPOSE_DEV := docker compose -f docker/dev/compose.yaml --env-file .env
COMPOSE_PROD := docker compose -f docker/prod/compose.yaml --env-file .env

.PHONY: help dev prod down down-dev down-prod logs logs-dev logs-prod test test-backend test-frontend build clean ps

help: ## Show this help message
	@echo "Available commands:"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-16s\033[0m %s\n", $$1, $$2}'

dev: ## Start development environment with hot-reload (PostgreSQL, Quarkus Dev, Vite Dev)
	$(COMPOSE_DEV) up -d --build

prod: ## Start production environment (PostgreSQL, Quarkus Fast-Jar, Nginx SPA)
	$(COMPOSE_PROD) up -d --build

down: ## Stop and remove all containers, networks, and orphans (both dev and prod)
	$(COMPOSE_DEV) down --remove-orphans
	$(COMPOSE_PROD) down --remove-orphans

down-dev: ## Stop development environment
	$(COMPOSE_DEV) down --remove-orphans

down-prod: ## Stop production environment
	$(COMPOSE_PROD) down --remove-orphans

logs: ## Tail logs for development environment
	$(COMPOSE_DEV) logs -f

logs-prod: ## Tail logs for production environment
	$(COMPOSE_PROD) logs -f

build: ## Build both development and production container images
	$(COMPOSE_DEV) build
	$(COMPOSE_PROD) build

ps: ## List running services
	@echo "--- Development Environment ---"
	@$(COMPOSE_DEV) ps
	@echo "--- Production Environment ---"
	@$(COMPOSE_PROD) ps

test: test-backend test-frontend ## Run tests across backend and frontend

test-backend: ## Run Quarkus backend unit and integration tests
	@echo "Running backend tests..."
	@cd backend && (./mvnw test || mvn test)

test-frontend: ## Run frontend linter and unit tests
	@echo "Running frontend checks..."
	@cd frontend && npm run lint && npm test

clean: ## Stop containers, remove volumes, networks, and build caches
	$(COMPOSE_DEV) down -v --remove-orphans || true
	$(COMPOSE_PROD) down -v --remove-orphans || true
	rm -rf backend/target frontend/dist .quarkus
