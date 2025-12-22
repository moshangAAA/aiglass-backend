ALTER TABLE users
    ADD COLUMN otp_code VARCHAR(6),
    ADD COLUMN otp_generated_at TIMESTAMP,
    ADD COLUMN otp_verified BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN phone_verified BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN failed_login_attempts INT NOT NULL DEFAULT 0,
    ADD COLUMN lockout_time TIMESTAMP,
    ADD COLUMN locked BOOLEAN NOT NULL DEFAULT FALSE;

-- Add index for
CREATE INDEX idx_user_locked ON users(locked);
CREATE INDEX idx_user_lockout_time ON users(lockout_time);
CREATE INDEX idx_user_phone_verified ON users(phone_verified);