package ru.cs.vsu.social_network.telegram_bot.service;

import ru.cs.vsu.social_network.telegram_bot.dto.response.DailyStatsResponse;
import ru.cs.vsu.social_network.telegram_bot.dto.response.ReportResponse;
import ru.cs.vsu.social_network.telegram_bot.dto.response.VisitorLogResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Сервис для генерации отчетов и статистики посещений тренажерного зала.
 * Обеспечивает формирование журналов посещений, статистики и отчетов для администраторов.
 */
public interface ReportService {

    /**
     * Генерирует журнал посещений за текущий день.
     * Формирует текстовую таблицу со списком всех посетителей за сегодня.
     *
     * @param adminUserId идентификатор администратора, запрашивающего отчет
     * @return DTO журнала посещений за текущий день
     */
    VisitorLogResponse generateDailyReport(UUID adminUserId);

    /**
     * Генерирует журнал посещений за указанную дату.
     * Формирует текстовую таблицу со списком всех посетителей за указанный день.
     *
     * @param adminUserId идентификатор администратора, запрашивающего отчет
     * @param date дата для генерации отчета
     * @return DTO журнала посещений за указанную дату
     */
    VisitorLogResponse generateDailyReportForDate(UUID adminUserId, LocalDate date);

    /**
     * Генерирует сводный отчет за указанный период.
     * Включает общую статистику, ежедневную разбивку и форматированный отчет для Telegram.
     *
     * @param adminUserId идентификатор администратора, запрашивающего отчет
     * @param startDate начальная дата периода
     * @param endDate конечная дата периода
     * @return DTO отчета за период
     */
    ReportResponse generatePeriodReport(UUID adminUserId, LocalDate startDate, LocalDate endDate);

    /**
     * Генерирует список статистики по дням за указанный период.
     * Используется для детализированного анализа посещаемости.
     *
     * @param adminUserId идентификатор администратора, запрашивающего статистику
     * @param startDate начальная дата периода
     * @param endDate конечная дата периода
     * @return список ежедневной статистики
     */
    List<DailyStatsResponse> generateDailyStats(UUID adminUserId, LocalDate startDate, LocalDate endDate);

    /**
     * Форматирует отчет для Telegram за указанную дату.
     *
     * @param date дата отчета
     * @param visitorNames список имен всех посетителей
     * @param newUserNames список имен новых пользователей
     * @return форматированный отчет для Telegram
     */
    String formatTelegramReport(LocalDate date,
                                List<String> visitorNames,
                                List<String> newUserNames);

    /**
     * Получает журналы посещений за последние N дней.
     * Используется для быстрого доступа к недавней истории.
     *
     * @param adminUserId идентификатор администратора
     * @param days количество дней для выборки
     * @return список журналов посещений
     */
    List<VisitorLogResponse> getRecentVisitorLogs(UUID adminUserId, int days);

    /**
     * Получает журнал посещений за указанную дату.
     * Если журнал не существует, возвращает пустой Optional.
     *
     * @param adminUserId идентификатор администратора, запрашивающего журнал
     * @param date дата для получения журнала
     * @return Optional с журналом посещений, если существует
     */
    Optional<VisitorLogResponse> getVisitorLogByDate(UUID adminUserId, LocalDate date);

    /**
     * Получает журналы посещений за указанный период.
     * Возвращает все журналы в указанном диапазоне дат.
     *
     * @param adminUserId идентификатор администратора, запрашивающего журналы
     * @param startDate начальная дата периода
     * @param endDate конечная дата периода
     * @return список журналов посещений за период
     */
    List<VisitorLogResponse> getVisitorLogsByPeriod(UUID adminUserId, LocalDate startDate, LocalDate endDate);

    /**
     * Получает журнал посещений по его идентификатору.
     * Используется для получения конкретной записи журнала.
     *
     * @param adminUserId идентификатор администратора
     * @param logId идентификатор журнала посещений
     * @return DTO журнала посещений
     */
    VisitorLogResponse getVisitorLogById(UUID adminUserId, UUID logId);

    /**
     * Получает все журналы посещений с пагинацией.
     * Используется для административного интерфейса с большим количеством записей.
     *
     * @param adminUserId идентификатор администратора
     * @param page номер страницы (начиная с 0)
     * @param size количество записей на странице
     * @return список журналов посещений для указанной страницы
     */
    List<VisitorLogResponse> getAllVisitorLogsPaginated(UUID adminUserId, int page, int size);

    /**
     * Получает общее количество записей в журнале посещений.
     *
     * @param adminUserId идентификатор администратора
     * @return общее количество записей
     */
    long getTotalVisitorLogsCount(UUID adminUserId);
}