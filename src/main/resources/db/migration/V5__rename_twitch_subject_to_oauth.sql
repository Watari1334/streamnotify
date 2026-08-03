ALTER TABLE users RENAME COLUMN twitch_subject TO oauth_subject;
ALTER TABLE users ADD COLUMN oauth_provider VARCHAR(50) NOT NULL DEFAULT 'twitch';
ALTER TABLE users ALTER COLUMN oauth_provider DROP DEFAULT;