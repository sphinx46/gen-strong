package ru.cs.vsu.social_network.telegram_bot.provider;

import ru.cs.vsu.social_network.telegram_bot.entity.UserTraining;

import java.util.Optional;
import java.util.UUID;

/**
 * Специализированный провайдер для доступа к сущностям UserTraining.
 * Обеспечивает получение данных тренировок пользователя с обработкой ошибок.
 */
public interface UserTrainingEntityProvider extends EntityProvider<UserTraining> {

    /**
     * Находит запись тренировок пользователя по Telegram ID.
     *
     * @param telegramId идентификатор пользователя в Telegram
     * @return Optional с записью тренировок, если найдена
     */
    Optional<UserTraining> findByTelegramId(Long telegramId);

    /**
     * Находит запись тренировок пользователя по идентификатору пользователя.
     *
     * @param userId идентификатор пользователя
     * @return Optional с записью тренировок, если найдена
     */
    Optional<UserTraining> findByUserId(UUID userId);

    /**
     * Проверяет существование записи тренировок для пользователя.
     *
     * @param userId идентификатор пользователя
     * @return true если запись тренировок существует
     */
    boolean existsByUserId(UUID userId);

    /**
     * Проверяет существование записи тренировок для пользователя по Telegram ID.
     *
     * @param telegramId Telegram ID пользователя
     * @return true если запись тренировок существует
     */
    boolean existsByTelegramId(Long telegramId);

    /**
     * Получает максимальный жим лежа пользователя по идентификатору пользователя.
     *
     * @param userId идентификатор пользователя
     * @return Optional с максимальным жимом лежа, если запись существует
     */
    Optional<Double> getMaxBenchPressByUserId(UUID userId);

    /**
     * Получает максимальный жим лежа пользователя по Telegram ID.
     *
     * @param telegramId Telegram ID пользователя
     * @return Optional с максимальным жимом лежа, если запись существует
     */
    Optional<Double> getMaxBenchPressByTelegramId(Long telegramId);
}