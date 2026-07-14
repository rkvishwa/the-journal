-- Add pending_email column for admin email-change verification flow
-- Run:
--   ./sql/migrate.sh sql/004_add_pending_email.sql

USE blog_engine;

ALTER TABLE email_verifications
    ADD COLUMN IF NOT EXISTS pending_email VARCHAR(255) NULL AFTER used;
