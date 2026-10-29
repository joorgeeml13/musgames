# 04. Technical Specification — Database Schema & 2v2 Elo Rating

**Módulo:** Persistencia y Competitivo (`/backend/.../domain` y `/backend/.../service/elo`)  
**Motor:** PostgreSQL 16+ con Hibernate ORM / Panache  
**Objetivo:** Persistir cuentas, perfiles (Riot ID), partidas cerradas, inventario unificado de skins por carta y calcular variaciones de ELO por parejas.

## 1. Esquema Relacional de Base de Datos (DDL)

```sql
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. Tabla de Cuentas (Auth & Seguridad)
CREATE TABLE accounts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(32) NOT NULL DEFAULT 'USER',
    is_system_admin BOOLEAN NOT NULL DEFAULT FALSE,
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    email_verification_token VARCHAR(255),
    email_verification_expires_at TIMESTAMP WITH TIME ZONE,
    password_reset_token VARCHAR(255),
    password_reset_expires_at TIMESTAMP WITH TIME ZONE,
    failed_login_attempts INT NOT NULL DEFAULT 0,
    locked_until TIMESTAMP WITH TIME ZONE,
    last_login_at TIMESTAMP WITH TIME ZONE,
    last_login_ip VARCHAR(45),
    two_factor_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    two_factor_secret VARCHAR(255),
    is_banned BOOLEAN NOT NULL DEFAULT FALSE,
    banned_until TIMESTAMP WITH TIME ZONE,
    ban_reason VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_accounts_email ON accounts (email);

-- 2. Tabla de Sesiones y Dispositivos (Rotación de Refresh Tokens)
CREATE TABLE user_sessions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    device_id VARCHAR(128) NOT NULL,
    device_name VARCHAR(128),
    user_agent TEXT,
    ip_address VARCHAR(45) NOT NULL,
    refresh_token_hash VARCHAR(255) NOT NULL UNIQUE,
    previous_refresh_token_hash VARCHAR(255),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    is_revoked BOOLEAN NOT NULL DEFAULT FALSE,
    revoked_at TIMESTAMP WITH TIME ZONE,
    revocation_reason VARCHAR(64),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    last_refreshed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sessions_account ON user_sessions (account_id);
CREATE INDEX idx_sessions_device ON user_sessions (device_id);
CREATE INDEX idx_sessions_token_hash ON user_sessions (refresh_token_hash);

-- 3. Tabla de Perfiles Públicos (Sistema Riot ID: game_name#tag_line)
CREATE TABLE user_profiles (
    id UUID PRIMARY KEY REFERENCES accounts(id) ON DELETE CASCADE,
    game_name VARCHAR(24) NOT NULL,
    tag_line VARCHAR(6) NOT NULL,
    display_name VARCHAR(48),
    avatar_url VARCHAR(255),
    avatar_frame_id VARCHAR(64),
    active_title VARCHAR(64) NOT NULL DEFAULT 'Novato del Chinchón',
    bio VARCHAR(250),
    elo_rating INT NOT NULL DEFAULT 1200,
    games_played INT NOT NULL DEFAULT 0,
    games_won INT NOT NULL DEFAULT 0,
    is_founder BOOLEAN NOT NULL DEFAULT FALSE,
    locale VARCHAR(8) NOT NULL DEFAULT 'es-ES',
    is_online BOOLEAN NOT NULL DEFAULT FALSE,
    last_seen_at TIMESTAMP WITH TIME ZONE,
    allow_direct_challenges BOOLEAN NOT NULL DEFAULT TRUE,
    preferred_card_back_id VARCHAR(64),
    force_classic_deck_view BOOLEAN NOT NULL DEFAULT FALSE,
    sound_effects_volume INT NOT NULL DEFAULT 80,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Unicidad estricta estilo Riot ID (ej: Jorge#MAD es único)
CREATE UNIQUE INDEX idx_profiles_riot_id ON user_profiles (LOWER(game_name), LOWER(tag_line));
CREATE INDEX idx_profiles_elo ON user_profiles (elo_rating DESC);

-- 4. Catálogo de Skins (Carta a Carta — Compartido entre todos los juegos de baraja española)
CREATE TABLE card_skins (
    id VARCHAR(64) PRIMARY KEY,      -- ej: 'skin_as_oros_founder'
    card_raw_value INT NOT NULL,     -- 1 al 12
    card_suit VARCHAR(16) NOT NULL,  -- 'OROS', 'COPAS', 'ESPADAS', 'BASTOS'
    name VARCHAR(64) NOT NULL,
    rarity VARCHAR(16) NOT NULL,     -- 'COMMON', 'RARE', 'EPIC', 'LEGENDARY'
    asset_path VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 5. Inventario y Equipamiento del Jugador
CREATE TABLE user_card_inventory (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES user_profiles(id) ON DELETE CASCADE,
    card_skin_id VARCHAR(64) NOT NULL REFERENCES card_skins(id),
    is_equipped BOOLEAN NOT NULL DEFAULT FALSE,
    acquired_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_user_card UNIQUE (user_id, card_skin_id)
);

-- 6. Historial de Partidas
CREATE TABLE matches (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    game_type VARCHAR(16) NOT NULL DEFAULT 'MUS', -- 'MUS', 'TUTE', 'GUINOTE'
    mode VARCHAR(16) NOT NULL,                    -- 'RANKED_2V2', 'CASUAL', 'PRIVATE'
    team_a_score INT NOT NULL,
    team_b_score INT NOT NULL,
    winner_team VARCHAR(1) NOT NULL,              -- 'A' | 'B'
    duration_seconds INT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 7. Participantes y Variación de ELO por Partida
CREATE TABLE match_participants (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    match_id UUID NOT NULL REFERENCES matches(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES user_profiles(id),
    team VARCHAR(1) NOT NULL,
    seat INT NOT NULL,
    elo_before INT NOT NULL,
    elo_after INT NOT NULL,
    elo_delta INT NOT NULL
);

CREATE INDEX idx_match_participants_user ON match_participants (user_id);
```

## 2. Algoritmo de ELO para Parejas (2v2)

- **ELO Promedio de Equipo:**
  - $R_A = \frac{\text{ELO}(Jugador_{A1}) + \text{ELO}(Jugador_{A2})}{2}$
  - $R_B = \frac{\text{ELO}(Jugador_{B1}) + \text{ELO}(Jugador_{B2})}{2}$

- **Probabilidad Esperada de Victoria (E):**
  - $E_A = \frac{1}{1 + 10^{(R_B - R_A) / 400}}$
  - $E_B = 1 - E_A$

- **Factor K Dinámico:**
  - $K = 40$ para jugadores en calibración (< 20 partidas).
  - $K = 24$ para jugadores estándar ($\text{ELO} < 2000$).
  - $K = 16$ para jugadores de élite ($\text{ELO} \ge 2000$).

- **Cálculo de Variación (Delta):**
  - $\Delta_i = \text{round}(K_i \cdot (\text{Resultado} - E_{\text{equipo}}))$ (donde $\text{Resultado}$ es 1 para victoria y 0 para derrota).

## 3. Escalera de Rangos Competitivos

| Rango | Rango de ELO | Título Distintivo |
|---|---|---|
| **Hierro** | 0 – 999 | Novato del Chinchón |
| **Bronce** | 1000 – 1199 | Aprendiz de Barra |
| **Plata** | 1200 – 1399 | Parroquiano Frecuente |
| **Oro** | 1400 – 1599 | Tahúr de Pueblo |
| **Platino** | 1600 – 1799 | As de Bastos |
| **Diamante** | 1800 – 1999 | Maestro de la Seña |
| **Gran Maestro** | 2000+ | El Rey del Órdago |
