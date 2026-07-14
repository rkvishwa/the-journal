USE blog_engine;

CREATE TABLE IF NOT EXISTS posts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(160) NOT NULL,
    slug VARCHAR(180) NOT NULL,
    excerpt VARCHAR(320) NOT NULL,
    content_html LONGTEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    published_at TIMESTAMP(6) NULL,
    scheduled_at TIMESTAMP(6) NULL,
    CONSTRAINT uk_posts_slug UNIQUE (slug)
);

CREATE TABLE IF NOT EXISTS keywords (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    slug VARCHAR(100) NOT NULL,
    CONSTRAINT uk_keywords_name UNIQUE (name),
    CONSTRAINT uk_keywords_slug UNIQUE (slug)
);

CREATE TABLE IF NOT EXISTS post_keywords (
    post_id BIGINT NOT NULL,
    keyword_id BIGINT NOT NULL,
    PRIMARY KEY (post_id, keyword_id),
    CONSTRAINT fk_post_keywords_post FOREIGN KEY (post_id) REFERENCES posts (id) ON DELETE CASCADE,
    CONSTRAINT fk_post_keywords_keyword FOREIGN KEY (keyword_id) REFERENCES keywords (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS author_profile (
    id BIGINT PRIMARY KEY,
    display_name VARCHAR(80) NOT NULL,
    avatar_url VARCHAR(255) NULL
);

CREATE TABLE IF NOT EXISTS admin_credentials (
    id BIGINT PRIMARY KEY,
    username VARCHAR(80) NOT NULL,
    password_hash VARCHAR(120) NOT NULL,
    CONSTRAINT uk_admin_credentials_username UNIQUE (username)
);

INSERT INTO admin_credentials (id, username, password_hash)
VALUES (1, 'lae', '{bcrypt}$2a$10$PmUthJYdNs0YzLipgFkYxefTfTlWCHPmJmTeAqSUtR1ZrLxkHDj7K')
ON DUPLICATE KEY UPDATE
    username = VALUES(username),
    password_hash = VALUES(password_hash);

INSERT INTO author_profile (id, display_name, avatar_url)
VALUES (1, 'lae', NULL)
ON DUPLICATE KEY UPDATE
    display_name = VALUES(display_name);
