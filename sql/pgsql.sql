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

-- Enable PGVector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- Knowledge Bases
CREATE TABLE knowledge_bases
(
    id          BIGSERIAL PRIMARY KEY,
    title       VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    user_id     BIGINT       NOT NULL REFERENCES users (id),
    is_public   BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_knowledge_base_user_id ON knowledge_bases (user_id);

-- Knowledge Documents
CREATE TABLE knowledge_documents
(
    id                BIGSERIAL PRIMARY KEY,
    knowledge_base_id BIGINT      NOT NULL REFERENCES knowledge_bases (id) ON DELETE CASCADE,
    title             VARCHAR(255) NOT NULL,
    content_type      VARCHAR(10) NOT NULL,
    file_path         VARCHAR(500),
    created_at        TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_knowledge_document_kb_id ON knowledge_documents (knowledge_base_id);

-- Knowledge Chunks (with vector embeddings)
CREATE TABLE knowledge_chunks
(
    id                BIGSERIAL PRIMARY KEY,
    document_id       BIGINT    NOT NULL REFERENCES knowledge_documents (id) ON DELETE CASCADE,
    knowledge_base_id BIGINT    NOT NULL REFERENCES knowledge_bases (id) ON DELETE CASCADE,
    chunk_index       INTEGER   NOT NULL,
    content           TEXT      NOT NULL,
    embedding         vector(1024),
    created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_knowledge_chunk_document_id ON knowledge_chunks (document_id);
CREATE INDEX idx_knowledge_chunk_kb_id ON knowledge_chunks (knowledge_base_id);