-- ─────────────────────────────────────────────────────────────────────────────
-- Basalt — PostgreSQL Initialisation Script
-- Enables the pgvector extension required by Spring AI's PgVectorStore.
-- This script runs automatically on first container startup.
-- ─────────────────────────────────────────────────────────────────────────────

CREATE EXTENSION IF NOT EXISTS vector;

