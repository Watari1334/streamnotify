CREATE TABLE users (
                       user_id    BIGSERIAL PRIMARY KEY,
                       user_name  VARCHAR(255) NOT NULL,
                       created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE streamers (
                           streamer_id          BIGSERIAL PRIMARY KEY,
                           platform             VARCHAR(50) NOT NULL,
                           platform_channel_id  VARCHAR(255) NOT NULL,
                           channel_name         VARCHAR(255) NOT NULL,
                           created_at           TIMESTAMP NOT NULL DEFAULT now(),
                           UNIQUE (platform, platform_channel_id)
);

CREATE TABLE notification_destinations (
                                           destination_id      BIGSERIAL PRIMARY KEY,
                                           user_id             BIGINT NOT NULL REFERENCES users (user_id),
                                           notification_type   VARCHAR(50) NOT NULL DEFAULT 'DISCORD',
                                           discord_webhook_url VARCHAR(500) NOT NULL,
                                           created_at           TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE registrations (
                               user_id     BIGINT NOT NULL REFERENCES users (user_id),
                               streamer_id BIGINT NOT NULL REFERENCES streamers (streamer_id),
                               created_at  TIMESTAMP NOT NULL DEFAULT now(),
                               PRIMARY KEY (user_id, streamer_id)
);

CREATE TABLE stream_status_history (
                                       history_id             BIGSERIAL PRIMARY KEY,
                                       streamer_id            BIGINT NOT NULL REFERENCES streamers (streamer_id),
                                       stream_started_at      TIMESTAMP NOT NULL,
                                       notification_sent_flag BOOLEAN NOT NULL DEFAULT false,
                                       created_at              TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_notification_destinations_user_id ON notification_destinations (user_id);
CREATE INDEX idx_registrations_streamer_id ON registrations (streamer_id);
CREATE INDEX idx_stream_status_history_streamer_id ON stream_status_history (streamer_id);