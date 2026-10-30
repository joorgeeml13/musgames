-- Initial schema for Mus backend: accounts, user_sessions, and user_profiles

CREATE TABLE accounts (
    id UUID PRIMARY KEY,
    version BIGINT,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    is_system_admin BOOLEAN NOT NULL DEFAULT FALSE,
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    email_verification_token VARCHAR(255),
    email_verification_expires_at TIMESTAMPTZ,
    password_reset_token VARCHAR(255),
    password_reset_expires_at TIMESTAMPTZ,
    failed_login_attempts INT NOT NULL DEFAULT 0,
    locked_until TIMESTAMPTZ,
    last_login_at TIMESTAMPTZ,
    last_login_ip VARCHAR(45),
    two_factor_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    two_factor_secret VARCHAR(255),
    is_banned BOOLEAN NOT NULL DEFAULT FALSE,
    banned_until TIMESTAMPTZ,
    ban_reason VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT chk_accounts_failed_login_attempts CHECK (failed_login_attempts >= 0)
);

CREATE TABLE user_sessions (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    device_id VARCHAR(128) NOT NULL,
    device_name VARCHAR(128),
    refresh_token_hash VARCHAR(255) NOT NULL,
    previous_refresh_token_hash VARCHAR(255),
    expires_at TIMESTAMPTZ NOT NULL,
    is_revoked BOOLEAN NOT NULL DEFAULT FALSE,
    revoked_at TIMESTAMPTZ,
    revocation_reason VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_refreshed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_profiles (
    id UUID PRIMARY KEY REFERENCES accounts(id) ON DELETE CASCADE,
    game_name VARCHAR(24) NOT NULL,
    tag_line VARCHAR(6) NOT NULL,
    display_name VARCHAR(48),
    elo_rating INT NOT NULL DEFAULT 1200,
    games_played INT NOT NULL DEFAULT 0,
    games_won INT NOT NULL DEFAULT 0,
    active_title VARCHAR(64) DEFAULT '',
    is_founder BOOLEAN NOT NULL DEFAULT FALSE,
    avatar_url VARCHAR(255),
    avatar_frame_id VARCHAR(64),
    bio VARCHAR(250),
    preferred_card_back_id VARCHAR(64),
    force_classic_deck_view BOOLEAN NOT NULL DEFAULT FALSE,
    sound_effects_volume INT NOT NULL DEFAULT 80,
    locale VARCHAR(8) NOT NULL DEFAULT 'es-ES',
    is_online BOOLEAN NOT NULL DEFAULT FALSE,
    last_seen_at TIMESTAMPTZ,
    allow_direct_challenges BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_profiles_sound_volume CHECK (sound_effects_volume BETWEEN 0 AND 100),
    CONSTRAINT chk_profiles_elo CHECK (elo_rating >= 0),
    CONSTRAINT chk_profiles_games CHECK (games_won >= 0 AND games_played >= games_won)
);

CREATE INDEX idx_accounts_email ON accounts(email);
CREATE UNIQUE INDEX uq_accounts_email ON accounts(LOWER(email));
CREATE INDEX idx_accounts_password_reset_token ON accounts(password_reset_token) WHERE password_reset_token IS NOT NULL;
CREATE INDEX idx_accounts_email_verification_token ON accounts(email_verification_token) WHERE email_verification_token IS NOT NULL;

CREATE INDEX idx_user_sessions_account_id ON user_sessions(account_id);
CREATE UNIQUE INDEX idx_user_sessions_refresh_hash ON user_sessions(refresh_token_hash);
CREATE INDEX idx_user_sessions_prev_refresh_hash ON user_sessions(previous_refresh_token_hash);

CREATE INDEX idx_user_profiles_player_tag ON user_profiles(game_name, tag_line);
CREATE UNIQUE INDEX uq_user_profiles_player_tag ON user_profiles(LOWER(game_name), LOWER(tag_line));
CREATE INDEX idx_user_profiles_elo ON user_profiles (elo_rating DESC);
