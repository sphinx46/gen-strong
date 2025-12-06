package ru.cs.vsu.social_network.telegram_bot.provider.providerImpl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.cs.vsu.social_network.telegram_bot.entity.VisitorLog;
import ru.cs.vsu.social_network.telegram_bot.exception.VisitorLogNotFoundException;
import ru.cs.vsu.social_network.telegram_bot.provider.AbstractEntityProvider;
import ru.cs.vsu.social_network.telegram_bot.provider.VisitorLogEntityProvider;
import ru.cs.vsu.social_network.telegram_bot.repository.VisitorLogRepository;
import ru.cs.vsu.social_network.telegram_bot.utils.MessageConstants;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Реализация провайдера для получения сущности VisitorLog.
 * Обеспечивает доступ к данным журналов посетителей с обработкой исключительных ситуаций.
 */
@Slf4j
@Component
public final class VisitorLogEntityProviderImpl extends AbstractEntityProvider<VisitorLog>
        implements VisitorLogEntityProvider {
    private static final String ENTITY_NAME = "ЖУРНАЛ_ПОСЕЩЕНИЙ";
    private final VisitorLogRepository visitorLogRepository;

    public VisitorLogEntityProviderImpl(VisitorLogRepository visitorLogRepository) {
        super(visitorLogRepository, ENTITY_NAME, () ->
                new VisitorLogNotFoundException(MessageConstants.VISITOR_LOG_NOT_FOUND_FAILURE));
        this.visitorLogRepository = visitorLogRepository;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<VisitorLog> findByLogDate(LocalDate logDate) {
        log.info("{}_ПРОВАЙДЕР_ПОИСК_ПО_ДАТЕ_НАЧАЛО: " +
                "поиск журнала за дату: {}", ENTITY_NAME, logDate);

        final Optional<VisitorLog> visitorLog = visitorLogRepository.findByLogDate(logDate);

        log.info("{}_ПРОВАЙДЕР_ПОИСК_ПО_ДАТЕ_УСПЕХ: " +
                        "журнал {} за дату: {}",
                ENTITY_NAME, visitorLog.isPresent() ? "найден" : "не найден", logDate);

        return visitorLog;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean existsByLogDate(LocalDate logDate) {
        log.info("{}_ПРОВАЙДЕР_ПРОВЕРКА_СУЩЕСТВОВАНИЯ_ПО_ДАТЕ_НАЧАЛО: " +
                "проверка существования журнала за дату: {}", ENTITY_NAME, logDate);

        final boolean exists = visitorLogRepository.existsByLogDate(logDate);

        log.info("{}_ПРОВАЙДЕР_ПРОВЕРКА_СУЩЕСТВОВАНИЯ_ПО_ДАТЕ_УСПЕХ: " +
                        "журнал {} за дату: {}",
                ENTITY_NAME, exists ? "существует" : "не существует", logDate);

        return exists;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<VisitorLog> findAllByMonth(int year, int month) {
        log.info("{}_ПРОВАЙДЕР_ПОЛУЧЕНИЕ_ПО_МЕСЯЦУ_НАЧАЛО: " +
                "за {}/{}", ENTITY_NAME, month, year);

        final List<VisitorLog> logs = visitorLogRepository.findAllByMonth(year, month);

        log.info("{}_ПРОВАЙДЕР_ПОЛУЧЕНИЕ_ПО_МЕСЯЦУ_УСПЕХ: " +
                "найдено {} записей за {}/{}", logs.size(), month, year);

        return logs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<VisitorLog> findLatest(int limit) {
        log.info("{}_ПРОВАЙДЕР_ПОЛУЧЕНИЕ_ПОСЛЕДНИХ_НАЧАЛО: " +
                "запрос последних {} записей", limit);

        final int effectiveLimit = Math.max(1, Math.min(limit, 100));
        final List<VisitorLog> logs = visitorLogRepository.findLatest(effectiveLimit);

        log.info("{}_ПРОВАЙДЕР_ПОЛУЧЕНИЕ_ПОСЛЕДНИХ_УСПЕХ: " +
                "получено {} записей", logs.size());

        return logs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<VisitorLog> findByPeriod(LocalDate startDate, LocalDate endDate) {
        log.info("{}_ПРОВАЙДЕР_ПОЛУЧЕНИЕ_ЗА_ПЕРИОД_НАЧАЛО: " +
                "период с {} по {}", ENTITY_NAME, startDate, endDate);

        final List<VisitorLog> logs = visitorLogRepository.findAllByLogDateBetweenOrderByLogDateDesc(startDate, endDate);

        log.info("{}_ПРОВАЙДЕР_ПОЛУЧЕНИЕ_ЗА_ПЕРИОД_УСПЕХ: " +
                "получено {} записей за период {} - {}", logs.size(), startDate, endDate);

        return logs;
    }
}