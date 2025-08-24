-- src/main/resources/schema.sql
CREATE TABLE IF NOT EXISTS repositories (
                                            repository_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                            github_repo_id BIGINT NOT NULL UNIQUE,
                                            name VARCHAR(255) NOT NULL,
    owner VARCHAR(255) NOT NULL,
    github_url VARCHAR(500) NOT NULL,
    primary_language VARCHAR(50),
    stars_count INT DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS issues (
                                      issue_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                      repository_id BIGINT NOT NULL,
                                      github_issue_id BIGINT NOT NULL UNIQUE,
                                      title VARCHAR(500) NOT NULL,
    estimated_time VARCHAR(30),
    github_url VARCHAR(500) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    difficulty_level VARCHAR(20),
    popularity_score INT DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_issue_repository FOREIGN KEY (repository_id)
    REFERENCES repositories(repository_id) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS labels (
                                      label_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                      issue_id BIGINT NOT NULL,
                                      label_name VARCHAR(100) NOT NULL,
    label_color VARCHAR(7),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_label_issue FOREIGN KEY (issue_id)
    REFERENCES issues(issue_id) ON DELETE CASCADE  -- labels가 아닌 issues 참조
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;