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