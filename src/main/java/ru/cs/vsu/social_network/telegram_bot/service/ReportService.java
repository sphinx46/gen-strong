package ru.cs.vsu.social_network.telegram_bot.service;

import ru.cs.vsu.social_network.telegram_bot.dto.response.DailyStatsResponse;
import ru.cs.vsu.social_network.telegram_bot.dto.response.ReportResponse;
import ru.cs.vsu.social_network.telegram_bot.dto.response.VisitorLogResponse;

import java.time.LocalDate;
import java.util.List;
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
     * Форматирует текстовый отчет для Telegram на основе данных журнала посещений.
     * Создает таблицу в формате, удобном для отображения в Telegram.
     *
     * @param date дата отчета
     * @param visitorNames список имен посетителей
     * @return форматированная строка для отправки в Telegram
     */
    String formatTelegramReport(LocalDate date, List<String> visitorNames);

    /**
     * Получает журналы посещений за последние N дней.
     * Используется для быстрого доступа к недавней истории.
     *
     * @param adminUserId идентификатор администратора
     * @param days количество дней для выборки
     * @return список журналов посещений
     */
    List<VisitorLogResponse> getRecentVisitorLogs(UUID adminUserId, int days);
}