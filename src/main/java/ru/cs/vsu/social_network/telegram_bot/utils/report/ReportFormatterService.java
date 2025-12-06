package ru.cs.vsu.social_network.telegram_bot.utils.report;

import ru.cs.vsu.social_network.telegram_bot.dto.response.DailyStatsResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Сервис для форматирования отчетов и статистики для различных форматов вывода.
 * Обеспечивает преобразование данных в удобочитаемые форматы для Telegram и других каналов.
 */
public interface ReportFormatterService {

    /**
     * Форматирует ежедневный отчет для Telegram.
     * Создает структурированное сообщение со списком посетителей за день.
     *
     * @param date дата отчета
     * @param visitorNames список имен посетителей
     * @return форматированная строка для отправки в Telegram
     */
    String formatDailyTelegramReport(LocalDate date, List<String> visitorNames);

    /**
     * Форматирует отчет за период для Telegram.
     * Создает сводную статистику за период с детализацией по дням.
     *
     * @param startDate начальная дата периода
     * @param endDate конечная дата периода
     * @param dailyStats ежедневная статистика
     * @param totalVisits общее количество посещений
     * @param uniqueVisitors количество уникальных посетителей
     * @param averageDailyVisits среднее количество посещений в день
     * @return форматированная строка отчета
     */
    String formatPeriodTelegramReport(LocalDate startDate, LocalDate endDate,
                                      Map<LocalDate, DailyStatsResponse> dailyStats,
                                      long totalVisits, long uniqueVisitors,
                                      double averageDailyVisits);

    /**
     * Форматирует список посетителей в читаемый вид.
     * Создает нумерованный список с именами посетителей.
     *
     * @param visitorNames список имен посетителей
     * @return форматированный список
     */
    String formatVisitorList(List<String> visitorNames);

    /**
     * Форматирует дату в стандартный для отчетов формат.
     *
     * @param date дата для форматирования
     * @return отформатированная строка даты
     */
    String formatDate(LocalDate date);

    /**
     * Форматирует статистику за день в текстовый вид.
     * Создает строку с датой и количеством посетителей.
     *
     * @param dailyStat статистика за день
     * @return форматированная строка
     */
    String formatDailyStat(DailyStatsResponse dailyStat);
}