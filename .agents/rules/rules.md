---
description: Index of the modular agent rules for Órdago.
always_on: true
---

# Agent Directives & Instructions Index — Órdago (ordago.gg)

Before executing or designing any feature, bug fix, or refactor, read and follow the modular rules in `.agents/rules/` (paths relative to the repository root):

1. `project-context.md`: Vision, Mus core mechanics, repository layout, and source-of-truth documents.
2. `git-workflow.md`: Branching strategy, conventional commits, and the CI checks every change must pass.
3. `code-standards.md`: Java 21, TypeScript, general quality, and testing guidelines.
4. `backend-quarkus.md` (applies to `apps/backend/**`): Strict hexagonal architecture per domain, blocking execution model, in-memory game state, token rotation, and WebSocket patterns.
5. `frontend-react.md` (applies to `apps/frontend/**`): TanStack Router, Zustand, Cyber-Tavern design system, and exclusive camera focus UI.

Do not introduce architectural patterns, dependencies, or gameplay logic that conflict with these rules.
