package ru.cs.vsu.social_network.telegram_bot.validation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import ru.cs.vsu.social_network.telegram_bot.provider.EntityProvider;
import ru.cs.vsu.social_network.telegram_bot.utils.MessageConstants;

import java.util.UUID;

/**
 * Абстрактная реализация валидатора для проверки прав доступа.
 *
 * @param <T> тип сущности для валидации
 */
@Slf4j
public abstract class AbstractValidator<T> {

    protected final EntityProvider<T> entityProvider;
    protected final String entityName;

    protected AbstractValidator(EntityProvider<T> entityProvider,
                                String entityName) {
        this.entityProvider = entityProvider;
        this.entityName = entityName;
    }

    /**
     * Проверяет владение сущностью.
     *
     * @param userId идентификатор пользователя
     * @param entityId идентификатор сущности
     * @throws AccessDeniedException если пользователь не является владельцем
     */
    protected void validateOwnership(UUID userId, UUID entityId) {
        log.info("{}_ВАЛИДАТОР_ВЛАДЕНИЕ_НАЧАЛО: проверка владения пользователем {} сущности {}",
                entityName, userId, entityId);

        T entity = entityProvider.getById(entityId);
        UUID ownerId = extractOwnerId(entity);

        if (!ownerId.equals(userId)) {
            log.warn("{}_ВАЛИДАТОР_ВЛАДЕНИЕ_ОШИБКА: пользователь {} не является владельцем сущности {}",
                    entityName, userId, entityId);
            throw new AccessDeniedException(MessageConstants.ACCESS_DENIED);
        }

        log.info("{}_ВАЛИДАТОР_ВЛАДЕНИЕ_УСПЕХ: пользователь {} является владельцем",
                entityName, userId);
    }

    /**
     * Извлекает идентификатор владельца из сущности.
     *
     * @param entity сущность для извлечения владельца
     * @return идентификатор владельца сущности
     */
    protected abstract UUID extractOwnerId(T entity);
}