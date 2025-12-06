-- Создание таблиц, если они не существуют
CREATE TABLE IF NOT EXISTS app_user (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    telegram_id BIGINT UNIQUE NOT NULL,
    username VARCHAR(255),
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    display_name VARCHAR(255),
    role VARCHAR(20) DEFAULT 'USER' CHECK (role IN ('USER', 'ADMIN')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS visit (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    visit_date TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS visitor_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    visitor_count INTEGER NOT NULL DEFAULT 0,
    raw_data TEXT,
    log_date DATE UNIQUE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Создание индексов
CREATE INDEX IF NOT EXISTS idx_user_telegram_id ON app_user(telegram_id);
CREATE INDEX IF NOT EXISTS idx_visit_visit_date ON visit(visit_date);
CREATE UNIQUE INDEX IF NOT EXISTS idx_visit_user_date ON visit(user_id, visit_date);
CREATE INDEX IF NOT EXISTS idx_visitor_log_date ON visitor_log(log_date);

-- Создание тестового администратора
INSERT INTO app_user (telegram_id, username, first_name, last_name, display_name, role)
VALUES (
    2056410944,
    'sphinx46',
    'Ваше',
    'Имя',
    'Администратор',
    'ADMIN'
)
ON CONFLICT (telegram_id) DO UPDATE SET
    username = EXCLUDED.username,
    first_name = EXCLUDED.first_name,
    last_name = EXCLUDED.last_name,
    display_name = EXCLUDED.display_name,
    role = EXCLUDED.role,
    updated_at = CURRENT_TIMESTAMP;