package ru.cs.vsu.social_network.telegram_bot.validation.validationImpl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import ru.cs.vsu.social_network.telegram_bot.provider.UserEntityProvider;
import ru.cs.vsu.social_network.telegram_bot.utils.MessageConstants;
import ru.cs.vsu.social_network.telegram_bot.validation.VisitorLogValidator;

import java.util.UUID;

/**
 * Реализация валидатора для проверки прав доступа к журналам посещений.
 */
@Slf4j
@Component
public final class VisitorLogValidatorImpl implements VisitorLogValidator {

    private static final String ENTITY_NAME = "ЖУРНАЛ_ПОСЕЩЕНИЙ";
    private final UserEntityProvider userEntityProvider;

    public VisitorLogValidatorImpl(UserEntityProvider userEntityProvider) {
        this.userEntityProvider = userEntityProvider;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateAdminAccessForLogs(UUID userId) {
        log.info("{}_ВАЛИДАТОР_АДМИН_ДОСТУП_НАЧАЛО: " +
                        "проверка прав администратора для доступа к журналам, пользователь {}",
                ENTITY_NAME, userId);

        var user = userEntityProvider.getById(userId);

        if (user.getRole() != ru.cs.vsu.social_network.telegram_bot.entity.enums.ROLE.ADMIN) {
            log.warn("{}_ВАЛИДАТОР_АДМИН_ДОСТУП_ОШИБКА: пользователь {} не является администратором",
                    ENTITY_NAME, userId);
            throw new AccessDeniedException(MessageConstants.ADMIN_ACCESS_REQUIRED);
        }

        log.info("{}_ВАЛИДАТОР_АДМИН_ДОСТУП_УСПЕХ: пользователь {} имеет доступ к журналам",
                ENTITY_NAME, userId);
    }
}