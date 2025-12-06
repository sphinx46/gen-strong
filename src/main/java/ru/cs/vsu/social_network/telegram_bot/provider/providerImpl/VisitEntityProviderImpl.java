package ru.cs.vsu.social_network.telegram_bot.provider.providerImpl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.cs.vsu.social_network.telegram_bot.entity.User;
import ru.cs.vsu.social_network.telegram_bot.entity.Visit;
import ru.cs.vsu.social_network.telegram_bot.exception.VisitNotFoundException;
import ru.cs.vsu.social_network.telegram_bot.provider.AbstractEntityProvider;
import ru.cs.vsu.social_network.telegram_bot.provider.VisitEntityProvider;
import ru.cs.vsu.social_network.telegram_bot.repository.VisitRepository;
import ru.cs.vsu.social_network.telegram_bot.utils.MessageConstants;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Реализация провайдера для получения сущности Visit.
 * Обеспечивает доступ к данным посещений с обработкой исключительных ситуаций.
 */
@Slf4j
@Component
public final class VisitEntityProviderImpl extends AbstractEntityProvider<Visit>
        implements VisitEntityProvider {
    private static final String ENTITY_NAME = "ПОСЕЩЕНИЕ";
    private final VisitRepository visitRepository;

    public VisitEntityProviderImpl(VisitRepository visitRepository) {
        super(visitRepository, ENTITY_NAME, () ->
                new VisitNotFoundException(MessageConstants.VISIT_NOT_FOUND_FAILURE));
        this.visitRepository = visitRepository;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean existsByUserIdAndDate(UUID userId, LocalDate date) {
        log.info("{}_ПРОВАЙДЕР_ПРОВЕРКА_ПОСЕЩЕНИЯ_НАЧАЛО: " +
                        "проверка посещения для пользователя: {} на дату: {}",
                ENTITY_NAME, userId, date);

        final boolean exists = visitRepository.existsByUserIdAndDate(userId, date);

        log.info("{}_ПРОВАЙДЕР_ПРОВЕРКА_ПОСЕЩЕНИЯ_УСПЕХ: " +
                        "посещение {} для пользователя: {} на дату: {}",
                ENTITY_NAME, exists ? "существует" : "не существует", userId, date);

        return exists;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Visit> findAllWithUsersByDate(LocalDate date) {
        log.info("{}_ПРОВАЙДЕР_ПОЛУЧЕНИЕ_ПО_ДАТЕ_С_ПОЛЬЗОВАТЕЛЯМИ_НАЧАЛО: " +
                "для даты: {}", ENTITY_NAME, date);

        final List<Visit> visits = visitRepository.findAllWithUsersByDate(date);

        log.info("{}_ПРОВАЙДЕР_ПОЛУЧЕНИЕ_ПО_ДАТЕ_С_ПОЛЬЗОВАТЕЛЯМИ_УСПЕХ: " +
                "найдено {} посещений для даты: {}", visits.size(), date);

        return visits;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long countByDate(LocalDate date) {
        log.info("{}_ПРОВАЙДЕР_ПОДСЧЕТ_ПО_ДАТЕ_НАЧАЛО: " +
                "для даты: {}", ENTITY_NAME, date);

        final long count = visitRepository.countByDate(date);

        log.info("{}_ПРОВАЙДЕР_ПОДСЧЕТ_ПО_ДАТЕ_УСПЕХ: " +
                "найдено {} посещений для даты: {}", count, date);

        return count;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Visit> findAllByDateRange(LocalDate startDate, LocalDate endDate) {
        log.info("{}_ПРОВАЙДЕР_ПОЛУЧЕНИЕ_ПО_ДИАПАЗОНУ_ДАТ_НАЧАЛО: " +
                "с {} по {}", ENTITY_NAME, startDate, endDate);

        final List<Visit> visits = visitRepository.findAllByDateRange(startDate, endDate);

        log.info("{}_ПРОВАЙДЕР_ПОЛУЧЕНИЕ_ПО_ДИАПАЗОНУ_ДАТ_УСПЕХ: " +
                "найдено {} посещений", visits.size());

        return visits;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Visit> findByUserAndVisitDateBetween(User user, LocalDateTime start, LocalDateTime end) {
        log.info("{}_ПРОВАЙДЕР_ПОИСК_ПО_ПОЛЬЗОВАТЕЛЮ_И_ДИАПАЗОНУ_НАЧАЛО: " +
                        "для пользователя: {} с {} по {}",
                ENTITY_NAME, user.getId(), start, end);

        final Optional<Visit> visit = visitRepository.findByUserAndVisitDateBetween(user, start, end);

        log.info("{}_ПРОВАЙДЕР_ПОИСК_ПО_ПОЛЬЗОВАТЕЛЮ_И_ДИАПАЗОНУ_УСПЕХ: " +
                        "посещение {} для пользователя: {}",
                ENTITY_NAME, visit.isPresent() ? "найдено" : "не найдено", user.getId());

        return visit;
    }
}