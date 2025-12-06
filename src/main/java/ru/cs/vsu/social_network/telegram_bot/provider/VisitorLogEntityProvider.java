package ru.cs.vsu.social_network.telegram_bot.provider;

import ru.cs.vsu.social_network.telegram_bot.entity.VisitorLog;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Специализированный провайдер для доступа к сущностям VisitorLog.
 * Обеспечивает получение журналов посетителей с обработкой ошибок.
 */
public interface VisitorLogEntityProvider extends EntityProvider<VisitorLog> {

    /**
     * Находит запись журнала по дате.
     *
     * @param logDate дата журнала
     * @return Optional с записью журнала, если найдена
     */
    Optional<VisitorLog> findByLogDate(LocalDate logDate);

    /**
     * Проверяет существование записи журнала на указанную дату.
     *
     * @param logDate дата для проверки
     * @return true если запись существует, false в противном случае
     */
    boolean existsByLogDate(LocalDate logDate);

    /**
     * Находит все записи журнала за указанный месяц.
     *
     * @param year год
     * @param month месяц (1-12)
     * @return список записей журнала за месяц
     */
    List<VisitorLog> findAllByMonth(int year, int month);

    /**
     * Находит последние N записей журнала.
     *
     * @param limit количество записей
     * @return список последних записей журнала
     */
    List<VisitorLog> findLatest(int limit);
}