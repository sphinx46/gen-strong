package ru.cs.vsu.social_network.telegram_bot.utils.factory;

import ru.cs.vsu.social_network.telegram_bot.entity.User;
import ru.cs.vsu.social_network.telegram_bot.entity.Visit;
import ru.cs.vsu.social_network.telegram_bot.utils.factory.EntityFactory;

import java.util.UUID;

/**
 * Фабрика для создания сущностей Visit.
 * Определяет контракт для создания посещений.
 */
public interface VisitFactory extends EntityFactory<Visit, Void> {

    /**
     * Создает посещение для пользователя.
     *
     * @return созданное посещение
     */
    @Override
    Visit create(Void request);

    /**
     * Создает посещение для пользователя по его Telegram ID.
     *
     * @param telegramId Telegram ID пользователя
     * @return созданное посещение
     */
    Visit createFromTelegramId(Long telegramId);

    /**
     * Создает посещение для текущего времени (альтернативный метод).
     *
     * @param user пользователь
     * @return созданное посещение
     */
    Visit createForUser(User user);

    /**
     * Создает посещение пользователя с данными.
     *
     * @param userId идентификатор пользователя
     * @param request dto данные
     * @return созданное посещение
     */
    Visit buildEntityWithUserId(UUID userId, Void request);
}