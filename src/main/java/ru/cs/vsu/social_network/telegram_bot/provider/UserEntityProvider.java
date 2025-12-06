package ru.cs.vsu.social_network.telegram_bot.provider;

import ru.cs.vsu.social_network.telegram_bot.entity.User;

import java.util.Optional;
import java.util.UUID;

/**
 * Специализированный провайдер для доступа к сущностям User.
 * Обеспечивает получение пользователей по идентификатору с обработкой ошибок.
 */
public interface UserEntityProvider extends EntityProvider<User> {

    /**
     * Находит пользователя по Telegram ID.
     *
     * @param telegramId идентификатор пользователя в Telegram
     * @return Optional с пользователем, если найден
     */
    Optional<User> findByTelegramId(Long telegramId);

    /**
     * Проверяет существование пользователя с указанным Telegram ID.
     *
     * @param telegramId идентификатор пользователя в Telegram
     * @return true если пользователь существует, false в противном случае
     */
    boolean existsByTelegramId(Long telegramId);
}