package ru.cs.vsu.social_network.telegram_bot.utils.factory.factoryImpl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.cs.vsu.social_network.telegram_bot.dto.request.AddVisitorRequest;
import ru.cs.vsu.social_network.telegram_bot.entity.VisitorLog;
import ru.cs.vsu.social_network.telegram_bot.utils.factory.VisitorLogFactory;


import java.time.LocalDate;

/**
 * Реализация фабрики для создания сущностей VisitorLog.
 * Создает новые экземпляры журналов посещений.
 */
@Slf4j
@Component
public final class VisitorLogFactoryImpl implements VisitorLogFactory {

    private static final String ENTITY_NAME = "ЖУРНАЛ_ПОСЕЩЕНИЙ";

    /**
     * {@inheritDoc}
     */
    @Override
    public VisitorLog createFromAddVisitorRequest(AddVisitorRequest request) {
        log.info("{}_ФАБРИКА_ИЗ_ЗАПРОСА: создание журнала из запроса",
                ENTITY_NAME);

        if (request == null) {
            log.warn("{}_ФАБРИКА_ИЗ_ЗАПРОСА: запрос null", ENTITY_NAME);
            return createWithData(0, "", LocalDate.now());
        }

        LocalDate date = request.getVisitDate() != null
                ? request.getVisitDate()
                : LocalDate.now();

        String displayName = request.getDisplayName() != null
                && !request.getDisplayName().trim().isEmpty()
                ? request.getDisplayName().trim()
                : "Анонимный посетитель";

        return createWithData(1, displayName, date);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public VisitorLog createWithData(Integer visitorCount, String rawData, LocalDate logDate) {
        log.info("{}_ФАБРИКА_С_ДАННЫМИ: создание журнала, количество: {}, дата: {}",
                ENTITY_NAME, visitorCount, logDate);

        LocalDate date = logDate != null ? logDate : LocalDate.now();
        Integer count = visitorCount != null ? visitorCount : 0;
        String data = rawData != null ? rawData : "";

        VisitorLog visitorLog = VisitorLog.builder()
                .visitorCount(count)
                .rawData(data)
                .logDate(date)
                .build();

        log.info("{}_ФАБРИКА_С_ДАННЫМИ: журнал создан", ENTITY_NAME);

        return visitorLog;
    }
}