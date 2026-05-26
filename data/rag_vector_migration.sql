ALTER TABLE rag_chunk
    ADD COLUMN vector_point_id VARCHAR(64) NULL AFTER metadata_json,
    ADD COLUMN embedding_model VARCHAR(128) NULL AFTER vector_point_id,
    ADD COLUMN embedding_dim INT NULL AFTER embedding_model,
    ADD COLUMN vector_status VARCHAR(32) NULL AFTER embedding_dim,
    ADD INDEX idx_rag_chunk_vector_status (vector_status);
