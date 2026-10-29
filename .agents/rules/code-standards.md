---
description: General code standards for Java 21 and TypeScript, plus testing requirements.
always_on: true
---

# 02. General Code Standards & Quality Assurance

## 1. General Principles
- **KISS & YAGNI:** Do not over-engineer. Build exactly what is specified in technical docs.
- **Fail-Fast:** Validate inputs, state transitions, and constraints at method entry points. Throw domain exceptions early.
- **Immutable by Default:** Value objects, WebSocket payloads, DTOs, and event states must be immutable.
- **No Magic Numbers:** Game constants (15s turn budget, 1.2s signal duration, 5s anti-spam penalty, 40-stone game target) must reside in typed enums or constant classes.

## 2. Java 21 & Backend Standards
- Leverage **Java Records** for immutable DTOs, value objects (`Card`, `HandEvaluation`), and command payloads.
- Use pattern matching for `switch` expressions exhaustively.
- Never use `System.out.println` or raw printStackTrace. Use Quarkus logging (`org.jboss.logging.Logger`).
- Null safety: Return empty collections (`List.of()`) or `Optional<T>` instead of raw `null`s for query methods.

## 3. TypeScript & Frontend Standards
- `strict: true` is enabled in `apps/frontend/tsconfig.app.json` and `apps/frontend/tsconfig.node.json`; never disable it. The `any` type is prohibited.
- Define explicit interfaces and types matching backend WebSocket and REST DTOs.
- Isolate WebSocket subscription and network side-effects inside dedicated Zustand stores or custom React hooks.

## 4. Testing Requirements
- **Backend:** JUnit 5 + AssertJ + `@QuarkusTest`. The Mus core engine (`Card`, `HandEvaluator`, FSM transitions) must maintain near 100% test coverage across all lances and Mano tiebreakers.
- **Frontend:** Vitest for utility logic, Zustand stores, and focus state handlers. Component tests use React Testing Library (not installed yet: add `@testing-library/react` as a devDependency in the first task that needs a component test).
- Skipped or focused tests are banned: no `@Disabled` (JUnit 5), no `it.skip` / `it.only` / `describe.skip` (Vitest).
