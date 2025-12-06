package ru.cs.vsu.social_network.telegram_bot.validation.validationImpl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import ru.cs.vsu.social_network.telegram_bot.entity.User;
import ru.cs.vsu.social_network.telegram_bot.entity.enums.ROLE;
import ru.cs.vsu.social_network.telegram_bot.provider.UserEntityProvider;
import ru.cs.vsu.social_network.telegram_bot.utils.MessageConstants;
import ru.cs.vsu.social_network.telegram_bot.validation.UserValidator;

import java.util.UUID;

/**
 * Реализация валидатора для проверки прав доступа пользователей.
 */
@Slf4j
@Component
public final class UserValidatorImpl implements UserValidator {

    private static final String ENTITY_NAME = "ПОЛЬЗОВАТЕЛЬ";
    private final UserEntityProvider userEntityProvider;

    public UserValidatorImpl(UserEntityProvider userEntityProvider) {
        this.userEntityProvider = userEntityProvider;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateAdminAccess(User user) {
        log.info("{}_ВАЛИДАТОР_АДМИН_ПРОВЕРКА_НАЧАЛО: " +
                        "проверка прав администратора для пользователя {}",
                ENTITY_NAME, user.getId());

        if (user.getRole() != ROLE.ADMIN) {
            log.warn("{}_ВАЛИДАТОР_АДМИН_ПРОВЕРКА_ОШИБКА: пользователь {} не является администратором",
                    ENTITY_NAME, user.getId());
            throw new AccessDeniedException(MessageConstants.ADMIN_ACCESS_REQUIRED);
        }

        log.info("{}_ВАЛИДАТОР_АДМИН_ПРОВЕРКА_УСПЕХ: пользователь {} является администратором",
                ENTITY_NAME, user.getId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateAdminAccessById(UUID userId) {
        log.info("{}_ВАЛИДАТОР_АДМИН_ПО_ID_НАЧАЛО: проверка прав администратора для пользователя {}",
                ENTITY_NAME, userId);

        User user = userEntityProvider.getById(userId);

        if (user.getRole() != ROLE.ADMIN) {
            log.warn("{}_ВАЛИДАТОР_АДМИН_ПО_ID_ОШИБКА: пользователь {} не является администратором",
                    ENTITY_NAME, userId);
            throw new AccessDeniedException(MessageConstants.ADMIN_ACCESS_REQUIRED);
        }

        log.info("{}_ВАЛИДАТОР_АДМИН_ПО_ID_УСПЕХ: пользователь {} является администратором",
                ENTITY_NAME, userId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateUserRole(User user, ROLE requiredRole) {
        log.info("{}_ВАЛИДАТОР_РОЛЬ_НАЧАЛО: проверка роли {} для пользователя {}",
                ENTITY_NAME, requiredRole, user.getId());

        if (user.getRole() != requiredRole) {
            log.warn("{}_ВАЛИДАТОР_РОЛЬ_ОШИБКА: у пользователя {} роль {}, требуется {}",
                    ENTITY_NAME, user.getId(), user.getRole(), requiredRole);
            throw new AccessDeniedException(
                    String.format(MessageConstants.ROLE_ACCESS_REQUIRED, requiredRole));
        }

        log.info("{}_ВАЛИДАТОР_РОЛЬ_УСПЕХ: пользователь {} имеет требуемую роль {}",
                ENTITY_NAME, user.getId(), requiredRole);
    }
}