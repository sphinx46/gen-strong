package ru.cs.vsu.social_network.telegram_bot.validation;

import java.util.UUID;

/**
 * Валидатор для проверки прав доступа к журналам посещений.
 */
public interface VisitorLogValidator {

    /**
     * Проверяет, что пользователь является администратором для доступа к журналам.
     *
     * @param userId идентификатор пользователя
     * @throws AccessDeniedException если пользователь не является администратором
     */
    void validateAdminAccessForLogs(UUID userId);
}