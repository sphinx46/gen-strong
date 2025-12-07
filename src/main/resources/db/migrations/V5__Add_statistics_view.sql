CREATE OR REPLACE VIEW daily_statistics AS
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