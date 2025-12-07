ALTER TABLE app_user
DROP CONSTRAINT IF EXISTS app_user_role_check;

ALTER TABLE app_user
ADD CONSTRAINT app_user_role_check
CHECK (role IN ('USER', 'ADMIN'));

ALTER TABLE app_user
ALTER COLUMN role SET DEFAULT 'USER';

CREATE INDEX IF NOT EXISTS idx_visit_created_at ON visit(created_at);
CREATE INDEX IF NOT EXISTS idx_app_user_created_at ON app_user(created_at);
CREATE INDEX IF NOT EXISTS idx_visitor_log_created_at ON visitor_log(created_at);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_views WHERE viewname = 'daily_statistics') THEN
        CREATE VIEW daily_statistics AS
        SELECT
            DATE(v.visit_date) AS visit_day,
            COUNT(DISTINCT v.user_id) AS unique_visitors,
            COUNT(v.id) AS total_visits,
            COUNT(DISTINCT u.id) FILTER (WHERE DATE(u.created_at) = DATE(v.visit_date)) AS new_users,
            vl.visitor_count AS logged_visitors,
            vl.new_users_count AS logged_new_users
        FROM visit v
        JOIN app_user u ON v.user_id = u.id
        LEFT JOIN visitor_log vl ON DATE(v.visit_date) = vl.log_date
        GROUP BY DATE(v.visit_date), vl.visitor_count, vl.new_users_count;
    END IF;
END $$;

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