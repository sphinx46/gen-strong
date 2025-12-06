package ru.cs.vsu.social_network.telegram_bot.validation.validationImpl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.cs.vsu.social_network.telegram_bot.entity.Visit;
import ru.cs.vsu.social_network.telegram_bot.exception.VisitRepeatException;
import ru.cs.vsu.social_network.telegram_bot.provider.VisitEntityProvider;
import ru.cs.vsu.social_network.telegram_bot.utils.MessageConstants;
import ru.cs.vsu.social_network.telegram_bot.validation.AbstractValidator;
import ru.cs.vsu.social_network.telegram_bot.validation.VisitValidator;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Реализация валидатора для проверки прав доступа к посещениям.
 */
@Slf4j
@Component
public final class VisitValidatorImpl extends AbstractValidator<Visit>
        implements VisitValidator {

    private final VisitEntityProvider visitEntityProvider;

    private static final String ENTITY_NAME = "ПОСЕЩЕНИЕ";

    public VisitValidatorImpl(VisitEntityProvider visitEntityProvider, VisitEntityProvider visitEntityProvider1) {
        super(visitEntityProvider, ENTITY_NAME);
        this.visitEntityProvider = visitEntityProvider1;
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
    public void validateRepeatVisit(UUID userId) {
        LocalDate today = LocalDate.now();
        if (visitEntityProvider.existsByUserIdAndDate(userId, today)) {
            log.warn("{}_ВАЛИДАТОР_ОТМЕТКА_ПОСЕЩЕНИЯ_ОШИБКА: " +
                    "пользователь уже отметился сегодня, ID: {}", ENTITY_NAME, userId);
            throw new VisitRepeatException(MessageConstants.VISIT_ALREADY_FAILURE);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected UUID extractOwnerId(Visit entity) {
        return entity.getUser().getId();
    }
}
