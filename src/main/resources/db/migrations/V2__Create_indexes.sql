CREATE INDEX idx_user_telegram_id ON app_user(telegram_id);
CREATE INDEX idx_visit_visit_date ON visit(visit_date);
CREATE UNIQUE INDEX idx_visit_user_date ON visit(user_id, visit_date);
CREATE INDEX idx_visitor_log_date ON visitor_log(log_date);