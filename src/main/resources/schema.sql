CREATE TABLE IF NOT EXISTS spring_ai_chat_memory (
    id SERIAL PRIMARY KEY,
    conversation_id VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    type VARCHAR(20) NOT NULL CHECK (type IN ('USER', 'ASSISTANT', 'SYSTEM', 'TOOL')),
    "timestamp" TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS chat_memory_archive (
                id SERIAL PRIMARY KEY,
                conversation_id VARCHAR(255) NOT NULL,
                content TEXT NOT NULL,
                type VARCHAR(20) NOT NULL CHECK (type IN ('USER', 'ASSISTANT', 'SYSTEM', 'TOOL')),
                "timestamp" TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS spring_ai_chat_memory_conversation_id_timestamp_idx
ON spring_ai_chat_memory(conversation_id, "timestamp");


CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS vector;

-- 2. Build the Vector Store table
CREATE TABLE IF NOT EXISTS vector_store (
    id uuid DEFAULT uuid_generate_v4() PRIMARY KEY,
    content text,
    metadata jsonb,
    embedding vector(1536)
);

-- 3. Build the performance index (IF NOT EXISTS prevents crashes on restarts)
CREATE INDEX IF NOT EXISTS vector_store_hnsw_idx
    ON vector_store USING hnsw (embedding vector_cosine_ops);