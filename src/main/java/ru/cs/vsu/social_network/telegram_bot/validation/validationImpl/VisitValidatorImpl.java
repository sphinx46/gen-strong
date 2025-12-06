package ru.cs.vsu.social_network.telegram_bot.validation.validationImpl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.cs.vsu.social_network.telegram_bot.entity.Visit;
import ru.cs.vsu.social_network.telegram_bot.provider.VisitEntityProvider;
import ru.cs.vsu.social_network.telegram_bot.validation.AbstractValidator;
import ru.cs.vsu.social_network.telegram_bot.validation.VisitValidator;

import java.util.UUID;

/**
 * Реализация валидатора для проверки прав доступа к посещениям.
 */
@Slf4j
@Component
public final class VisitValidatorImpl extends AbstractValidator<Visit>
        implements VisitValidator {

    private static final String ENTITY_NAME = "ПОСЕЩЕНИЕ";

    public VisitValidatorImpl(VisitEntityProvider visitEntityProvider) {
        super(visitEntityProvider, ENTITY_NAME);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateUserAccess(UUID userId, UUID visitId) {
        validateOwnership(userId, visitId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected UUID extractOwnerId(Visit entity) {
        return entity.getUser().getId();
    }
}
