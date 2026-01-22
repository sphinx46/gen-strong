package ru.cs.vsu.social_network.telegram_bot.service;

import ru.cs.vsu.social_network.telegram_bot.dto.request.UserMetricsRequest;
import ru.cs.vsu.social_network.telegram_bot.dto.response.UserMetricsResponse;

public interface UserMetricsService {

    /**
     * Сохранить или обновить метрики пользователя
     * @param request запрос с данными метрик
     * @return сохраненные метрики
     */
    UserMetricsResponse saveMetrics(UserMetricsRequest request);

    /**
     * Получить метрики пользователя по telegramId
     * @param telegramId идентификатор пользователя в Telegram
     * @return метрики пользователя или null если не найдены
     */
    UserMetricsResponse getMetricsByTelegramId(Long telegramId);

    /**
     * Проверить существование метрик пользователя
     * @param telegramId идентификатор пользователя в Telegram
     * @return true если метрики существуют
     */
    boolean existsByTelegramId(Long telegramId);

    /**
     * Удалить метрики пользователя
     * @param telegramId идентификатор пользователя в Telegram
     */
    void deleteByTelegramId(Long telegramId);
}