-- Initial seed data and schema extensions
CREATE UNIQUE INDEX IF NOT EXISTS uq_user_profiles_riot_id ON user_profiles (LOWER(game_name), LOWER(tag_line));

