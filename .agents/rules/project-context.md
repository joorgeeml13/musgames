---
description: Product vision, core Mus mechanics, repository layout and source-of-truth documents.
always_on: true
---

# 00. Project Context & Product Domain

## 1. Mission & Value Proposition
Órdago (`ordago.gg`) is a high-stakes, real-time web platform for classic Spanish tavern table games, launching with competitive Mus and expanding into Tute, Guiñote, Pocha, and Dominoes. It blends the ranked rating rigor of Chess.com with the psychological tension and first-person bluffs of *Liar's Bar*.

## 2. Repository Layout (Monorepo)
- `apps/backend/`: Quarkus 3 / Java 21 service (Maven, base package `dev.ordago`).
- `apps/frontend/`: React 19 / TypeScript / Vite SPA (npm).
- `docker/`: Dev and prod Compose files.
- `docs/`: Product and technical source-of-truth documents (see §4).
- `.github/workflows/ci.yml`: The CI gate every change must pass.

## 3. Core Pillars & Non-Negotiable Rules
- **Web-First (Zero Friction):** Instant play in modern browsers (React SPA); no heavy downloads or native clients required.
- **Exclusive Visual Focus (1 Target at a Time):** Players can only aim visual focus at one entity simultaneously (`CARDS`, `PARTNER`, `RIVAL_LEFT`, `RIVAL_RIGHT`). Tunnel vision is a core tactical mechanic.
- **Active Signal Catching (Anti-Spam):** Facial signals take 1.2s to perform. Opponents looking at the sender receive no automated text banner: they must visually spot the avatar animation and actively press the catch button (`CATCH_SIGNAL`). False alarms trigger a 5-second cooldown penalty.
- **Cross-Game Card-by-Card Cosmetics:** Card skins belong to the standard 40-card Spanish deck; skins equipped on individual cards persist across all supported Spanish card games on the platform.
- **Official Mus Rule Set:** 40-card deck with 8 Kings and 8 Aces (3s count as Kings; 2s count as Aces). A match is best-of-3: the first team to win 2 games of 40 stones wins the match.

## 4. Documents of Record (Source of Truth)
- `docs/product/01-product-vision.md` (Product vision, TikTok GTM, monetization)
- `docs/product/02-game-design-mus.md` (Complete Mus rules, focus mechanics, timer budgets)
- `docs/product/03-brand-and-design-system.md` (Cyber-Tavern brand guidelines, typography, palette)
- `docs/technical/01-architecture.md` (System layout, RAM vs. DB boundary, anti-cheat)
- `docs/technical/02-mus-state-machine.md` (Deterministic FSM, betting transitions, Mano tiebreakers)
- `docs/technical/03-websocket-protocol.md` (JSON WebSocket protocol, camera focus, gesture events)
- `docs/technical/04-database-and-elo.md` (PostgreSQL DDL schema, Player Tag pattern, 2v2 Elo math)
