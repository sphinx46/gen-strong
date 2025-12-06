package ru.cs.vsu.social_network.telegram_bot.validation;

import ru.cs.vsu.social_network.telegram_bot.exception.VisitRepeatException;

import java.util.UUID;

/**
 * Валидатор для проверки прав доступа к посещениям.
 */
public interface VisitValidator {

    /**
     * Проверяет, что пользователь имеет доступ к посещению.
     *
     * @param userId идентификатор пользователя
     * @param visitId идентификатор посещения
     * @throws AccessDeniedException если пользователь не имеет доступа
     */
    void validateUserAccess(UUID userId, UUID visitId);

    /**
     * Проверяет, отмечался ли пользователь уже сегодня.
     *
     * @param userId идентификатор пользователя
     * @throws VisitRepeatException если пользователь уже отмечался
     */
    void validateRepeatVisit(UUID userId);
}