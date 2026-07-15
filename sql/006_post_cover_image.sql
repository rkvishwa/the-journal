-- Add cover image support for blog posts
USE blog_engine;

ALTER TABLE posts
    ADD COLUMN IF NOT EXISTS cover_image_url VARCHAR(255) NULL AFTER content_html;
