---- Applied automatically on startup before Hibernate validation.
--ALTER TABLE email_verifications
--    ADD COLUMN IF NOT EXISTS pending_email VARCHAR(255) NULL;

ALTER TABLE email_verifications
    ADD COLUMN pending_email VARCHAR(255) NULL;