package ru.cs.vsu.social_network.telegram_bot.provider;

import ru.cs.vsu.social_network.telegram_bot.entity.UserMetrics;

import java.util.Optional;

public interface UserMetricsProvider {

    /**
     * Получает метрики пользователя по telegramId
     * @param telegramId идентификатор пользователя в Telegram
     * @return найденные метрики
     */
    UserMetrics getByTelegramId(Long telegramId);

    /**
     * Получает метрики пользователя по telegramId с опциональным результатом
     * @param telegramId идентификатор пользователя в Telegram
     * @return Optional с метриками пользователя
     */
    Optional<UserMetrics> findOptionalByTelegramId(Long telegramId);

    /**
     * Проверяет существование метрик пользователя по telegramId
     * @param telegramId идентификатор пользователя в Telegram
     * @return true если метрики существуют
     */
    boolean existsByTelegramId(Long telegramId);
}