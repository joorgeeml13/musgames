---
description: Backend architecture (strict hexagonal per domain), Quarkus stack, in-memory game state and WebSockets.
globs: ["apps/backend/**"]
---

# 03. Backend Architecture & Quarkus Technical Rules

Java sources live in `apps/backend/src/main/java/dev/ordago/`. Run every Maven command from `apps/backend/`.

## 1. Strict Hexagonal Architecture per Domain
Each business domain (`auth`, `profile`, `room`, `engine`, `elo`, ...) is a self-contained package with exactly this layout. Cross-cutting code lives in `dev.ordago.shared` with the same layout.

```
dev.ordago.<domain>/
  ├── domain/                 # Pure Java: no Quarkus, Jakarta, JPA or Jackson imports
  │     ├── model/            # Entities, records, value objects (e.g. Account, UserProfile)
  │     ├── port/
  │     │     ├── in/         # Use case interfaces (e.g. LoginUseCase)
  │     │     └── out/        # Driven ports (e.g. AccountRepositoryPort)
  │     └── exception/        # Domain exceptions (extend shared DomainException)
  ├── application/            # Use case implementations: implement port/in, depend only on port/out
  └── infrastructure/         # Adapters (the only layer allowed to use framework APIs)
        ├── persistence/      # JPA/Panache entities and repositories implementing port/out
        ├── security/         # SmallRye JWT, Argon2id hashing adapters
        └── api/              # REST resources, WebSocket endpoints, DTOs, exception mappers
```

Dependency rules (non-negotiable):
- `domain` depends on nothing outside `domain` (plus `shared.domain`).
- `application` depends only on `domain`.
- `infrastructure` depends on `application` and `domain`; nothing depends on `infrastructure`.
- A domain never imports another domain's `application` or `infrastructure`; cross-domain calls go through the other domain's `domain.port.in` interfaces.
- JPA entities and API DTOs never leak into `domain`; map explicitly in the adapter.
- **Tactical DDD & Value Objects**: Concepts with distinct validation rules, formatting, or domain constraints (e.g., `Email`, `PasswordHash`, `RiotId`, `PhoneNumber`) must be modeled as immutable Java `record`s under `domain/model/`. Enforce domain invariants in their constructors throwing subclasses of `DomainException`. Do not use raw primitives (*Primitive Obsession*) in domain entities.

## 2. Execution Model (Imperative / Blocking)
- The stack is **blocking**: `quarkus-rest` + Hibernate ORM with Panache. Do not introduce Mutiny `Uni`/`Multi`, Hibernate Reactive or reactive Panache.
- Never block the Vert.x event loop: database access and other blocking I/O must run on worker threads (default for blocking signatures; use `@Blocking` when a WebSocket or REST method would otherwise run on the event loop).

## 3. Auth & User Profile Domain
- **`Account`:** Security credentials hashed with Argon2id, role assignments (`USER`, `ADMIN`), brute-force counters (`failed_login_attempts`, `locked_until`).
- **`UserSession`:** Per-device session tracking (`device_id`). Store `refreshTokenHash` (SHA-256) and `previousRefreshTokenHash` with a 30s grace window to absorb network retry races while detecting token reuse/theft.
- **`UserProfile`:** Public player identity using the **Riot ID pattern** (`game_name` max 24 chars + `tag_line` max 6 chars, composite unique index on `LOWER(game_name), LOWER(tag_line)`).

## 4. In-Memory Hot State (Mus Engine)
- **Zero-Persistence During Gameplay:** Active rooms and hands live exclusively in JVM RAM managed by a concurrent `RoomManager` (`ConcurrentHashMap`). Never write to PostgreSQL on bets, mus declarations, or discards.
- **Cold Persistence on Match Over:** PostgreSQL writes occur only when a team wins the match (first team to win 2 games of 40 stones) to persist `matches`, `match_participants`, and calculate Elo.
- **Zero-Trust Network Projections:** The server NEVER sends rival cards over WebSockets. Every connection receives an individualized `PlayerViewDTO` containing only its own 4 cards.

## 5. Real-Time WebSockets (`quarkus-websockets-next`)
- Endpoints mapped at `@WebSocket(path = "/ws/rooms/{roomId}")`.
- Authenticate via JWT during handshake; reject invalid or expired tokens immediately with custom close code 4401.
