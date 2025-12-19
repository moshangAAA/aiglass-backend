CREATE TABLE users
(
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    username       VARCHAR(255) NOT NULL UNIQUE,
    phone_number   VARCHAR(255) NOT NULL UNIQUE,
    role           VARCHAR(255) NOT NULL,
    usage_account  INT          NOT NULL DEFAULT 0,
    total_distance INT          NOT NULL DEFAULT 0,
    preference     JSON         NULL,
    password_hash  VARCHAR(255) NOT NULL,
    created_at     TIMESTAMP    NOT NULL,
    updated_at     TIMESTAMP    NULL,

    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT uq_users_phone_number UNIQUE (phone_number)
);

CREATE INDEX idx_user_username ON users (username);
CREATE INDEX idx_user_phone ON users (phone_number);