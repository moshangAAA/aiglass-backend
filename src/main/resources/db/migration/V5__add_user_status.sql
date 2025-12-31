-- Add status column to users table
ALTER TABLE users
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE, INACTIVE, BANNED';

-- Add index for status queries
CREATE INDEX idx_user_status ON users(status);

