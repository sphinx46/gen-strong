package ru.cs.vsu.social_network.telegram_bot.utils.table;

import ru.cs.vsu.social_network.telegram_bot.dto.response.VisitorLogResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Сервис для форматирования таблиц посещений тренажерного зала.
 * Обеспечивает создание читаемых таблиц для Telegram с различными типами данных.
 */
public interface TableFormatterService {

    /**
     * Форматирует таблицу посещений за текущий день.
     * Использует существующий журнал или генерирует новый.
     *
     * @param adminUserId идентификатор администратора
     * @param existingLog Optional с существующим журналом за сегодня
     * @return форматированная таблица за текущий день
     */
    String formatTableForToday(String adminUserId, Optional<VisitorLogResponse> existingLog);

    /**
     * Форматирует таблицу посещений за указанную дату.
     * Использует существующий журнал или генерирует новый.
     *
     * @param adminUserId идентификатор администратора
     * @param date дата для форматирования
     * @param existingLog Optional с существующим журналом за указанную дату
     * @return форматированная таблица за указанную дату
     */
    String formatTableForDate(String adminUserId, LocalDate date, Optional<VisitorLogResponse> existingLog);

    /**
     * Форматирует таблицу посещений за указанный период.
     * Создает сводную таблицу с детализацией по дням.
     *
     * @param startDate начальная дата периода
     * @param endDate конечная дата периода
     * @param logs список журналов посещений за период
     * @return форматированная таблица за период
     */
    String formatTableForPeriod(LocalDate startDate, LocalDate endDate, List<VisitorLogResponse> logs);

    /**
     * Форматирует сообщение об отсутствии данных за период.
     *
     * @param startDate начальная дата периода
     * @param endDate конечная дата периода
     * @return сообщение об отсутствии данных
     */
    String formatPeriodTableEmpty(LocalDate startDate, LocalDate endDate);

    /**
     * Возвращает инструкции по использованию команды /table.
     *
     * @return строка с инструкциями
     */
    String getTableUsageInstructions();

    /**
     * Форматирует дату в читаемый формат.
     *
     * @param date дата для форматирования
     * @return отформатированная строка даты
     */
    String formatDate(LocalDate date);
}