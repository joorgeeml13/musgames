ALTER TABLE accounts RENAME COLUMN email_verification_token TO email_verification_token_hash;
ALTER TABLE accounts RENAME COLUMN password_reset_token TO password_reset_token_hash;

DROP INDEX IF EXISTS idx_accounts_email_verification_token;
DROP INDEX IF EXISTS idx_accounts_password_reset_token;

CREATE UNIQUE INDEX idx_accounts_email_verification_token_hash 
  ON accounts(email_verification_token_hash) 
  WHERE email_verification_token_hash IS NOT NULL;

CREATE UNIQUE INDEX idx_accounts_password_reset_token_hash 
  ON accounts(password_reset_token_hash) 
  WHERE password_reset_token_hash IS NOT NULL;

UPDATE accounts SET email_verification_token_hash = NULL, email_verification_expires_at = NULL WHERE email_verification_token_hash IS NOT NULL;
UPDATE accounts SET password_reset_token_hash = NULL, password_reset_expires_at = NULL WHERE password_reset_token_hash IS NOT NULL;
