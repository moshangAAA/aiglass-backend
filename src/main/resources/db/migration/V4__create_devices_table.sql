CREATE TABLE devices
(
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    name             VARCHAR(255) NOT NULL,
    serial_number    VARCHAR(255) NOT NULL UNIQUE,
    type             VARCHAR(255) NOT NULL,
    status           VARCHAR(50)  NOT NULL,
    location_lat     DOUBLE,
    location_lng     DOUBLE,
    battery_level    INT,
    firmware_version VARCHAR(255),
    last_heartbeat   TIMESTAMP,
    ip_address       VARCHAR(255),
    connect_time     TIMESTAMP,
    owner_id         BIGINT,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    NULL     DEFAULT NULL,
    CONSTRAINT fk_device_owner FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE SET NULL,
    INDEX            idx_device_serial(serial_number),
    INDEX            idx_device_status(status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;