package ru.cs.vsu.social_network.telegram_bot.validation;

import ru.cs.vsu.social_network.telegram_bot.entity.User;
import ru.cs.vsu.social_network.telegram_bot.entity.enums.ROLE;

import java.util.UUID;

/**
 * Валидатор для проверки прав доступа и валидации пользователей.
 */
public interface UserValidator {

    /**
     * Проверяет, является ли пользователь администратором.
     *
     * @param user пользователь для проверки
     * @throws AccessDeniedException если пользователь не является администратором
     */
    void validateAdminAccess(User user);

    /**
     * Проверяет, является ли пользователь администратором по его ID.
     *
     * @param userId идентификатор пользователя
     * @throws AccessDeniedException если пользователь не является администратором
     */
    void validateAdminAccessById(UUID userId);

    /**
     * Проверяет, что пользователь имеет указанную роль.
     *
     * @param user пользователь для проверки
     * @param requiredRole требуемая роль
     * @throws AccessDeniedException если пользователь не имеет требуемой роли
     */
    void validateUserRole(User user, ROLE requiredRole);
}