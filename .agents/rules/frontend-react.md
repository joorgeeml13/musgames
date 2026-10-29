---
description: Frontend architecture, routing, table focus UI and Cyber-Tavern design system.
globs: ["frontend/**"]
---

# 04. Frontend Architecture, UI & Real-Time Client

Frontend code lives in `frontend/`. Package manager is **npm** (`package-lock.json`); never use pnpm, yarn or bun.

## 1. Architecture & Routing
- **Core:** React 19 + TypeScript (strict) + Vite.
- **Routing:** TanStack Router with strictly typed route definitions:
  - `/`: Landing page
  - `/login`, `/register`: Auth views
  - `/lobby`: Matchmaking, friends list, profile overview
  - `/room/$roomId`: Game table
- **State Management:** Zustand stores (installed) for real-time WebSocket state synchronization.
- **Three.js (planned, not installed):** The game table will lazy-load a Three.js canvas in Phase 1.0. Do not add `three` or `@react-three/*` until a task explicitly asks for the 3D table.

## 2. Table UI & Visual Focus System
- Manage 4 mutually exclusive camera focus targets: `CARDS`, `PARTNER`, `RIVAL_LEFT`, `RIVAL_RIGHT`.
- Apply depth-of-field blur to the table when focus is set to `CARDS` to enforce tunnel vision.
- Disable the "Catch Signal" button if current focus is not pointing to an opponent (`RIVAL_LEFT` or `RIVAL_RIGHT`).
- Run a 5-second cooldown UI timer when a player triggers a false alarm catch attempt.

## 3. "Cyber-Tavern" Design System
- Dark Mode is mandatory and permanently enabled.
- Color Tokens:
  - Backgrounds: Obsidian Black (`#0D0F12`), Deep Charcoal (`#13161C`).
  - High Alerts & Órdagos: Crimson (`#E53935`).
  - Rating & Wins: Victory Gold (`#FFB300`).
  - Typography: Parchment White (`#F5F5F7`), Slate Muted (`#9BA1A6`).
- Card Readability: Suit icons and rank numbers must remain crisp, high-contrast, and instantly recognizable in top corners regardless of applied cosmetic skins.
