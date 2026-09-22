CREATE TABLE users (
    id         BIGSERIAL PRIMARY KEY,
    login_id   VARCHAR(50)  NOT NULL,
    password   VARCHAR(100) NOT NULL,
    name       VARCHAR(50)  NOT NULL,
    role       VARCHAR(20)  NOT NULL,
    created_at TIMESTAMP    NOT NULL,

    CONSTRAINT uk_users_login_id UNIQUE (login_id)
);
