package ru.cs.vsu.social_network.telegram_bot.utils.factory;

import ru.cs.vsu.social_network.telegram_bot.dto.request.AddVisitorRequest;
import ru.cs.vsu.social_network.telegram_bot.entity.VisitorLog;

import java.time.LocalDate;

/**
 * Фабрика для создания сущностей VisitorLog.
 * Определяет контракт для создания журналов посещений.
 */
public interface VisitorLogFactory {

    /**
     * Создает журнал посещений из запроса AddVisitorRequest.
     *
     * @param request запрос на добавление посетителя
     * @return созданный журнал посещений
     */
    VisitorLog createFromAddVisitorRequest(AddVisitorRequest request);

    /**
     * Создает журнал посещений с указанным количеством посетителей.
     *
     * @param visitorCount количество посетителей
     * @param rawData сырые данные (список имен)
     * @param logDate дата журнала
     * @return созданный журнал посещений
     */
    VisitorLog createWithData(Integer visitorCount,
                              String rawData,
                              LocalDate logDate);
}