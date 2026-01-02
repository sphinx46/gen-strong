package ru.cs.vsu.social_network.telegram_bot.service;

import ru.cs.vsu.social_network.telegram_bot.dto.request.UserBenchPressRequest;
import ru.cs.vsu.social_network.telegram_bot.dto.response.UserTrainingResponse;

import java.util.Optional;
import java.util.UUID;

/**
 * Сервис для управления данными тренировок пользователей.
 * Обеспечивает операции сохранения, обновления и получения данных о тренировках.
 */
public interface UserTrainingService {

    /**
     * Сохраняет или обновляет максимальный жим лежа пользователя по идентификатору пользователя.
     * Если запись существует - обновляет значение, если нет - создает новую запись.
     *
     * @param userId идентификатор пользователя
     * @param benchPressRequest запрос с максимальным жимом лежа
     * @return DTO ответа с сохраненными данными тренировок
     */
    UserTrainingResponse saveOrUpdateMaxBenchPress(UUID userId,
                                                   UserBenchPressRequest benchPressRequest);

    /**
     * Сохраняет или обновляет максимальный жим лежа пользователя по Telegram ID.
     * Удобный метод для работы с Telegram ботом.
     *
     * @param telegramId Telegram ID пользователя
     * @param benchPressRequest запрос с максимальным жимом лежа
     * @return DTO ответа с сохраненными данными тренировок
     */
    UserTrainingResponse saveOrUpdateMaxBenchPressByTelegramId(Long telegramId,
                                                               UserBenchPressRequest benchPressRequest);

    /**
     * Получает полную информацию о тренировках пользователя по идентификатору.
     *
     * @param userId идентификатор пользователя
     * @return Optional с данными тренировок, если запись существует
     */
    Optional<UserTrainingResponse> getUserTrainingByUserId(UUID userId);

    /**
     * Получает полную информацию о тренировках пользователя по Telegram ID.
     *
     * @param telegramId Telegram ID пользователя
     * @return Optional с данными тренировок, если запись существует
     */
    Optional<UserTrainingResponse> getUserTrainingByTelegramId(Long telegramId);

}