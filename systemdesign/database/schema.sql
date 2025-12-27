CREATE TABLE `userinfo` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `username` varchar(100) UNIQUE NOT NULL,
  `email` varchar(100) UNIQUE NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `phone` varchar(20),
  `role` varchar(20) NOT NULL COMMENT 'USER, ADMIN',
  `status` varchar(20) NOT NULL COMMENT 'ACTIVE, INACTIVE, BANNED',
  `last_time_login` timestamp,
  `preference` json,
  `created_at` timestamp,
  `updated_at` timestamp
);

CREATE TABLE `devices` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `name` varchar(100) NOT NULL,
  `type` varchar(50) NOT NULL,
  `owner_id` bigint NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'OFFLINE' COMMENT 'ONLINE, OFFLINE, ERROR',
  `location_lat` double,
  `location_lng` double,
  `battery_level` int DEFAULT 0,
  `firmware_version` varchar(50),
  `last_heartbeat` timestamp,
  `created_at` timestamp,
  `updated_at` timestamp
);

CREATE TABLE `location_history` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `device_id` bigint NOT NULL,
  `latitude` double NOT NULL,
  `longitude` double NOT NULL,
  `accuracy` decimal(5,2),
  `altitude` decimal(8,2),
  `speed` decimal(5,2),
  `heading` decimal(5,2),
  `created_at` timestamp,
  `updated_at` timestamp
);

CREATE TABLE `video_sessions` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `device_id` bigint NOT NULL,
  `video_url` varchar(255),
  `video_path` varchar(255),
  `duration` int DEFAULT 0,
  `size` bigint DEFAULT 0,
  `fps` int DEFAULT 30,
  `status` varchar(20) NOT NULL DEFAULT 'STREAMING' COMMENT 'STREAMING, RECORDING, COMPLETED, ERROR',
  `last_heartbeat` timestamp,
  `created_at` timestamp,
  `updated_at` timestamp
);

CREATE TABLE `voice_commands` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `device_id` bigint NOT NULL,
  `command_text` varchar(500),
  `command_type` varchar(50),
  `recognized_text` varchar(500),
  `response_text` varchar(500),
  `accuracy` decimal(5,2) DEFAULT 0,
  `status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING, RECOGNIZED, EXECUTED, FAILED',
  `created_at` timestamp,
  `updated_at` timestamp
);

CREATE TABLE `navigation_route` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `device_id` bigint NOT NULL,
  `start_location` json NOT NULL,
  `end_location` json NOT NULL,
  `route_name` varchar(255),
  `distance` decimal(10,2) DEFAULT 0,
  `estimated_time` int DEFAULT 0,
  `status` varchar(20) NOT NULL DEFAULT 'PLANNED' COMMENT 'PLANNED, IN_PROGRESS, COMPLETED, CANCELLED',
  `created_at` timestamp,
  `updated_at` timestamp
);

CREATE TABLE `system_logs` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `user_id` bigint,
  `action` varchar(100) NOT NULL,
  `resource_type` varchar(50),
  `resource_id` bigint,
  `old_value` json,
  `new_value` json,
  `ip_address` varchar(50),
  `user_agent` varchar(255),
  `created_at` timestamp,
  `updated_at` timestamp
);

CREATE INDEX `devices_index_0` ON `devices` (`owner_id`);

CREATE INDEX `devices_index_1` ON `devices` (`status`);

CREATE UNIQUE INDEX `devices_index_2` ON `devices` (`name`, `owner_id`);

CREATE INDEX `location_history_index_3` ON `location_history` (`device_id`, `created_at`);

CREATE INDEX `location_history_index_4` ON `location_history` (`latitude`, `longitude`);

CREATE INDEX `video_sessions_index_5` ON `video_sessions` (`user_id`);

CREATE INDEX `video_sessions_index_6` ON `video_sessions` (`device_id`);

CREATE INDEX `video_sessions_index_7` ON `video_sessions` (`created_at`);

CREATE INDEX `voice_commands_index_8` ON `voice_commands` (`user_id`);

CREATE INDEX `voice_commands_index_9` ON `voice_commands` (`device_id`);

CREATE INDEX `voice_commands_index_10` ON `voice_commands` (`status`);

CREATE INDEX `voice_commands_index_11` ON `voice_commands` (`created_at`);

CREATE INDEX `navigation_route_index_12` ON `navigation_route` (`user_id`);

CREATE INDEX `navigation_route_index_13` ON `navigation_route` (`device_id`);

CREATE INDEX `navigation_route_index_14` ON `navigation_route` (`status`);

CREATE INDEX `system_logs_index_15` ON `system_logs` (`user_id`);

CREATE INDEX `system_logs_index_16` ON `system_logs` (`action`);

CREATE INDEX `system_logs_index_17` ON `system_logs` (`created_at`);

ALTER TABLE `devices` ADD FOREIGN KEY (`owner_id`) REFERENCES `userinfo` (`id`);

ALTER TABLE `location_history` ADD FOREIGN KEY (`device_id`) REFERENCES `devices` (`id`);

ALTER TABLE `video_sessions` ADD FOREIGN KEY (`user_id`) REFERENCES `userinfo` (`id`);

ALTER TABLE `video_sessions` ADD FOREIGN KEY (`device_id`) REFERENCES `devices` (`id`);

ALTER TABLE `voice_commands` ADD FOREIGN KEY (`user_id`) REFERENCES `userinfo` (`id`);

ALTER TABLE `voice_commands` ADD FOREIGN KEY (`device_id`) REFERENCES `devices` (`id`);

ALTER TABLE `navigation_route` ADD FOREIGN KEY (`user_id`) REFERENCES `userinfo` (`id`);

ALTER TABLE `navigation_route` ADD FOREIGN KEY (`device_id`) REFERENCES `devices` (`id`);

ALTER TABLE `system_logs` ADD FOREIGN KEY (`user_id`) REFERENCES `userinfo` (`id`);
