-- Indexes to accelerate housekeeping purge queries
CREATE INDEX IF NOT EXISTS idx_user_sessions_expires_at ON user_sessions(expires_at);
CREATE INDEX IF NOT EXISTS idx_user_sessions_revoked_cleanup ON user_sessions(revoked_at) WHERE is_revoked = TRUE;
