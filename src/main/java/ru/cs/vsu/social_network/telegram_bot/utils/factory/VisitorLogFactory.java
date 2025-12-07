package ru.cs.vsu.social_network.telegram_bot.utils.factory;

import ru.cs.vsu.social_network.telegram_bot.dto.request.AddVisitorRequest;
import ru.cs.vsu.social_network.telegram_bot.entity.VisitorLog;

import java.time.LocalDate;

/**
 * Фабрика для создания сущностей VisitorLog.
 * Инкапсулирует логику создания новых записей в журнале посещений.
 */
public interface VisitorLogFactory {

    /**
     * Создает журнал посещений из запроса на добавление посетителя.
     *
     * @param request запрос на добавление посетителя
     * @return новый экземпляр VisitorLog
     */
    VisitorLog createFromAddVisitorRequest(AddVisitorRequest request);

    /**
     * Создает журнал посещений с указанными данными.
     *
     * @param visitorCount количество посетителей
     * @param rawData сырые данные о посетителях
     * @param logDate дата журнала
     * @return новый экземпляр VisitorLog
     */
    VisitorLog createWithData(Integer visitorCount,
                              String rawData,
                              LocalDate logDate);

    /**
     * Создает журнал посещений с указанными данными, включая количество новых пользователей.
     *
     * @param visitorCount количество посетителей
     * @param rawData сырые данные о посетителях
     * @param logDate дата журнала
     * @param newUsersCount количество новых пользователей
     * @return новый экземпляр VisitorLog
     */
    VisitorLog createWithData(Integer visitorCount,
                              String rawData,
                              LocalDate logDate,
                              int newUsersCount);
}