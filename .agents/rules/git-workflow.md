---
description: Branching strategy, conventional commits and the exact CI checks a change must pass.
always_on: true
---

# 01. Git Workflow, Branching & Release Cycle

## 1. Branch Strategy (GitFlow-lite)
- `main`: Production release branch. Only deployable, thoroughly tested code reaches this branch.
- `develop`: Shared integration and staging branch. Feature branches start from and merge back into `develop`.
- `feat/<module>-<short-description>`: Feature work (e.g., `feat/auth-riot-id`, `feat/engine-pares-evaluator`).
- `fix/<module>-<short-description>`: Bug and exploit fixes.
- `refactor/<module>-<short-description>`: Architectural improvements without functional changes.

## 2. Conventional Commits Specification
All commits must follow the `<type>(<scope>): <subject>` convention in present imperative mood:

Allowed types:
- `feat`: New feature or user-facing capability.
- `fix`: Bug or defect fix.
- `refactor`: Code reorganization without functional changes.
- `test`: Unit, integration, or regression test additions/updates.
- `docs`: Documentation, diagram, or markdown updates.
- `chore`: Tooling, Maven/npm dependency updates, or Docker changes.
- `ci`: GitHub Actions workflow changes.

Standard scopes: `(auth)`, `(engine)`, `(ws)`, `(elo)`, `(room)`, `(ui)`, `(db)`, `(infra)`.

*Examples:*
- `feat(auth): add refresh token rotation with sha-256 hash validation`
- `fix(engine): resolve mano seat priority tiebreaker on identical duples`
- `test(engine): cover edge cases for 31 vs 32 juego hand evaluation`

## 3. Pull Request Requirements
- Direct pushes to `main` and `develop` are prohibited.
- Every PR must pass the same checks as `.github/workflows/ci.yml`. Run them locally before finishing:

```bash
# Backend (from repo root)
cd backend && mvn clean test

# Frontend (from repo root)
cd frontend && npm ci && npm run lint && npm test && npm run build

# Infrastructure (from repo root)
docker compose -f docker/dev/compose.yaml config
```

- Zero failing tests; skipped tests are banned (see `code-standards.md` §4).
