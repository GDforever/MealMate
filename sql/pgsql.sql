CREATE
DATABASE mealmate
      WITH
      ENCODING = 'UTF8'
      LC_COLLATE = 'en_US.utf8'
      LC_CTYPE = 'en_US.utf8'
      TEMPLATE = template0
      CONNECTION LIMIT = -1;

CREATE TABLE users
(
    id                BIGSERIAL PRIMARY KEY,
    username          VARCHAR(50)  NOT NULL UNIQUE,
    password          VARCHAR(255) NOT NULL,
    email             VARCHAR(100) UNIQUE,
    taste_preferences VARCHAR(1000),
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE meal_records
(
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT       NOT NULL REFERENCES users (id),
    meal_type       VARCHAR(20)  NOT NULL, -- BREAKFAST, LUNCH, DINNER, SNACK
    food_name       VARCHAR(255) NOT NULL,
    restaurant_name VARCHAR(200),
    location        VARCHAR(500),
    latitude        DOUBLE PRECISION,
    longitude       DOUBLE PRECISION,
    recorded_at     TIMESTAMP    NOT NULL,
    user_rating     INTEGER,
    tags            VARCHAR(1000),
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_meal_record_user_id ON meal_records (user_id);

CREATE TABLE restaurants
(
    id           BIGSERIAL PRIMARY KEY,
    name         VARCHAR(255) NOT NULL,
    address      VARCHAR(500),
    latitude     DOUBLE PRECISION,
    longitude    DOUBLE PRECISION,
    cuisine_type VARCHAR(50),
    avg_price    DECIMAL(10, 2),
    rating       DOUBLE PRECISION,
    source       VARCHAR(20),
    external_id  VARCHAR(100),
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);


CREATE TABLE chat_sessions
(
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT    NOT NULL REFERENCES users (id),
    title      VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_chat_session_user_id ON chat_sessions (user_id);

CREATE TABLE chat_messages
(
    id         BIGSERIAL PRIMARY KEY,
    session_id BIGINT      NOT NULL REFERENCES chat_sessions (id) ON DELETE CASCADE,
    role       VARCHAR(20) NOT NULL, -- USER, ASSISTANT, SYSTEM
    content    TEXT        NOT NULL,
    created_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_chat_message_session_id_created_at ON chat_messages (session_id, created_at);