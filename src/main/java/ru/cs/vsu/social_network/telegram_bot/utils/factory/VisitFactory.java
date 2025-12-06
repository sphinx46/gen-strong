package ru.cs.vsu.social_network.telegram_bot.utils.factory;

import ru.cs.vsu.social_network.telegram_bot.entity.Visit;
import ru.cs.vsu.social_network.telegram_bot.utils.factory.EntityFactory;

import java.util.UUID;

/**
 * Фабрика для создания сущностей Visit.
 * Определяет контракт для создания посещений.
 */
public interface VisitFactory extends EntityFactory<Visit, Void> {

    /**
     * Создает посещение для пользователя по его ID.
     *
     * @param userId идентификатор пользователя
     * @return созданное посещение
     */
    @Override
    Visit create(UUID userId, Void request);

    /**
     * Создает посещение для пользователя по его Telegram ID.
     *
     * @param telegramId Telegram ID пользователя
     * @return созданное посещение
     */
    Visit createFromTelegramId(Long telegramId);
}