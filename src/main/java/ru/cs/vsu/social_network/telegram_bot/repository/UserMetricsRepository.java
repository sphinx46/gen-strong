package ru.cs.vsu.social_network.telegram_bot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.cs.vsu.social_network.telegram_bot.entity.UserMetrics;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserMetricsRepository extends JpaRepository<UserMetrics, UUID> {

    /**
     * Найти метрики пользователя по telegramId
     * @param telegramId идентификатор пользователя в Telegram
     * @return Optional с метриками пользователя
     */
    Optional<UserMetrics> findByTelegramId(Long telegramId);

    /**
     * Проверить существование метрик пользователя по telegramId
     * @param telegramId идентификатор пользователя в Telegram
     * @return true если метрики существуют
     */
    boolean existsByTelegramId(Long telegramId);

    /**
     * Удалить метрики пользователя по telegramId
     * @param telegramId идентификатор пользователя в Telegram
     */
    void deleteByTelegramId(Long telegramId);
}