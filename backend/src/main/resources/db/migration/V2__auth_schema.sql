-- =====================================================================
-- V2: Autentikasi & otorisasi (user, role, permission, token)
-- =====================================================================

CREATE TABLE users (
    id                      BIGINT       NOT NULL AUTO_INCREMENT,
    company_id              BIGINT       NOT NULL,
    username                VARCHAR(50)  NOT NULL,
    email                   VARCHAR(150) NOT NULL,
    full_name               VARCHAR(150) NOT NULL,
    password_hash           VARCHAR(100) NOT NULL,
    status                  VARCHAR(20)  NOT NULL,
    failed_login_attempts   INT          NOT NULL DEFAULT 0,
    locked_until            DATETIME(6),
    last_login_at           DATETIME(6),
    password_changed_at     DATETIME(6),
    created_at              DATETIME(6)  NOT NULL,
    created_by              VARCHAR(100),
    updated_at              DATETIME(6),
    updated_by              VARCHAR(100),
    version                 BIGINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_username (username),
    UNIQUE KEY uk_users_email (email),
    KEY ix_users_status (status),
    CONSTRAINT fk_users_company FOREIGN KEY (company_id) REFERENCES companies (id),
    CONSTRAINT ck_users_status CHECK (status IN ('PENDING', 'ACTIVE', 'INACTIVE'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE roles (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    code            VARCHAR(30)  NOT NULL,
    name            VARCHAR(100) NOT NULL,
    description     VARCHAR(255),
    system_role     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      DATETIME(6)  NOT NULL,
    created_by      VARCHAR(100),
    updated_at      DATETIME(6),
    updated_by      VARCHAR(100),
    version         BIGINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_roles_code (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE permissions (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    code            VARCHAR(60)  NOT NULL,
    module          VARCHAR(30)  NOT NULL,
    action          VARCHAR(20)  NOT NULL,
    description     VARCHAR(255),
    PRIMARY KEY (id),
    UNIQUE KEY uk_permissions_code (code),
    KEY ix_permissions_module (module)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE user_roles (
    user_id     BIGINT NOT NULL,
    role_id     BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE role_permissions (
    role_id         BIGINT NOT NULL,
    permission_id   BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_perm_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE,
    CONSTRAINT fk_role_perm_perm FOREIGN KEY (permission_id) REFERENCES permissions (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Refresh token disimpan sebagai hash SHA-256 (token asli hanya ada di cookie user)
CREATE TABLE refresh_tokens (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    user_id             BIGINT       NOT NULL,
    token_hash          CHAR(64)     NOT NULL,
    expires_at          DATETIME(6)  NOT NULL,
    revoked_at          DATETIME(6),
    replaced_by_hash    CHAR(64),
    remember_me         BOOLEAN      NOT NULL DEFAULT FALSE,
    ip_address          VARCHAR(64),
    user_agent          VARCHAR(255),
    created_at          DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_refresh_hash (token_hash),
    KEY ix_refresh_user (user_id),
    CONSTRAINT fk_refresh_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE password_reset_tokens (
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    user_id         BIGINT      NOT NULL,
    token_hash      CHAR(64)    NOT NULL,
    expires_at      DATETIME(6) NOT NULL,
    used_at         DATETIME(6),
    created_at      DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_reset_hash (token_hash),
    KEY ix_reset_user (user_id),
    CONSTRAINT fk_reset_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
