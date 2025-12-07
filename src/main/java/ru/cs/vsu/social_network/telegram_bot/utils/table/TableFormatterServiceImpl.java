package ru.cs.vsu.social_network.telegram_bot.utils.table;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.cs.vsu.social_network.telegram_bot.dto.response.VisitorLogResponse;
import ru.cs.vsu.social_network.telegram_bot.service.ReportService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Реализация сервиса для форматирования таблиц посещений.
 * Обеспечивает создание структурированных таблиц для Telegram с логированием операций.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TableFormatterServiceImpl implements TableFormatterService {

    private static final String SERVICE_NAME = "ФОРМАТИРОВАТЕЛЬ_ТАБЛИЦ";
    private static final DateTimeFormatter INPUT_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter OUTPUT_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final ReportService reportService;

    /**
     * {@inheritDoc}
     */
    @Override
    public String formatTableForToday(String adminUserId, Optional<VisitorLogResponse> existingLog) {
        log.info("{}_ФОРМАТИРОВАНИЕ_ТАБЛИЦЫ_ЗА_ТЕКУЩИЙ_ДЕНЬ_НАЧАЛО: администратор {}",
                SERVICE_NAME, adminUserId);

        final LocalDate today = LocalDate.now();

        if (existingLog.isPresent()) {
            log.info("{}_ФОРМАТИРОВАНИЕ_ТАБЛИЦЫ_ЗА_ТЕКУЩИЙ_ДЕНЬ_УСПЕХ: журнал за сегодня найден",
                    SERVICE_NAME);
            return existingLog.get().getFormattedReport();
        } else {
            log.info("{}_ФОРМАТИРОВАНИЕ_ТАБЛИЦЫ_ЗА_ТЕКУЩИЙ_ДЕНЬ_ГЕНЕРАЦИЯ: журнал не найден, генерируем",
                    SERVICE_NAME);
            final VisitorLogResponse newLog = reportService.generateDailyReport(java.util.UUID.fromString(adminUserId));
            return newLog.getFormattedReport();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String formatTableForDate(String adminUserId, LocalDate date, Optional<VisitorLogResponse> existingLog) {
        log.info("{}_ФОРМАТИРОВАНИЕ_ТАБЛИЦЫ_ЗА_ДАТУ_НАЧАЛО: администратор {}, дата: {}",
                SERVICE_NAME, adminUserId, date);

        if (existingLog.isPresent()) {
            log.info("{}_ФОРМАТИРОВАНИЕ_ТАБЛИЦЫ_ЗА_ДАТУ_УСПЕХ: журнал за дату {} найден",
                    SERVICE_NAME, date);
            return existingLog.get().getFormattedReport();
        } else {
            log.info("{}_ФОРМАТИРОВАНИЕ_ТАБЛИЦЫ_ЗА_ДАТУ_ГЕНЕРАЦИЯ: журнал не найден, генерируем",
                    SERVICE_NAME);
            final VisitorLogResponse newLog = reportService.generateDailyReportForDate(
                    java.util.UUID.fromString(adminUserId), date);
            return newLog.getFormattedReport();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String formatTableForPeriod(LocalDate startDate, LocalDate endDate, List<VisitorLogResponse> logs) {
        log.info("{}_ФОРМАТИРОВАНИЕ_ТАБЛИЦЫ_ЗА_ПЕРИОД_НАЧАЛО: период {} - {}, журналов: {}",
                SERVICE_NAME, startDate, endDate, logs.size());

        if (logs.isEmpty()) {
            log.info("{}_ФОРМАТИРОВАНИЕ_ТАБЛИЦЫ_ЗА_ПЕРИОД_ПУСТО: возвращаем пустую таблицу",
                    SERVICE_NAME);
            return formatPeriodTableEmpty(startDate, endDate);
        }


            final StringBuilder table = new StringBuilder();

        final String startDateStr = formatDate(startDate);
        final String endDateStr = formatDate(endDate);

        table.append("📋 *Таблица посещений тренажерного зала*\n");
        table.append("📅 *Период:* ").append(startDateStr).append(" - ").append(endDateStr).append("\n");
        table.append("📊 *Всего дней:* ").append(logs.size()).append("\n\n");

        int totalVisitors = 0;
        int totalNewUsers = 0;

        for (final VisitorLogResponse logEntry : logs) {
            final String dateStr = formatDate(logEntry.getLogDate());
            table.append("• *").append(dateStr).append("*: ")
                    .append(logEntry.getVisitorCount()).append(" чел.");

            if (logEntry.getNewUsersCount() != null && logEntry.getNewUsersCount() > 0) {
                table.append(" (новых: ").append(logEntry.getNewUsersCount()).append(")");
                totalNewUsers += logEntry.getNewUsersCount();
            }

            if (logEntry.getVisitorCount() > 0) {
                table.append(" (");
                if (logEntry.getRawData() != null && !logEntry.getRawData().isEmpty()) {
                    final String[] visitors = logEntry.getRawData().split(", ");
                    for (int i = 0; i < Math.min(visitors.length, 3); i++) {
                        if (i > 0) table.append(", ");
                        table.append(visitors[i]);
                    }
                    if (visitors.length > 3) {
                        table.append("...");
                    }
                }
                table.append(")");
            }
            table.append("\n");

            totalVisitors += logEntry.getVisitorCount();
        }

        table.append("\n*Итоги:*\n");
        table.append("• Всего посетителей за период: ").append(totalVisitors).append("\n");
        table.append("• Количество новых участников: ").append(totalNewUsers).append("\n");
        if (logs.size() > 0) {
            table.append("• Среднее в день: ")
                    .append(String.format("%.1f", (double) totalVisitors / logs.size()))
                    .append("\n");
        }

        log.info("{}_ФОРМАТИРОВАНИЕ_ТАБЛИЦЫ_ЗА_ПЕРИОД_УСПЕХ: " +
                        "таблица сформирована, новых пользователей: {}",
                SERVICE_NAME, totalNewUsers);

        return table.toString();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String formatPeriodTableEmpty(LocalDate startDate, LocalDate endDate) {
        log.info("{}_ФОРМАТИРОВАНИЕ_ПУСТОЙ_ТАБЛИЦЫ_ЗА_ПЕРИОД: период {} - {}",
                SERVICE_NAME, startDate, endDate);

        final String startDateStr = formatDate(startDate);
        final String endDateStr = formatDate(endDate);

        return "📋 *Таблица посещений тренажерного зала*\n" +
                "📅 *Период:* " + startDateStr + " - " + endDateStr + "\n\n" +
                "❌ *Нет данных о посещениях за указанный период.*\n" +
                "Возможно, журналы не были сформированы или посещений не было.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTableUsageInstructions() {
        log.debug("{}_ПОЛУЧЕНИЕ_ИНСТРУКЦИЙ_ПО_ИСПОЛЬЗОВАНИЮ", SERVICE_NAME);

        return "📋 *Использование команды /table*\n\n" +
                "*Получить таблицу за сегодня:*\n" +
                "/table\n\n" +
                "*Получить таблицу за определенную дату:*\n" +
                "/table ДД.ММ.ГГГГ\n" +
                "Пример: /table 06.12.2025\n\n" +
                "*Получить таблицу за период:*\n" +
                "/table ДД.ММ.ГГГГ ДД.ММ.ГГГГ\n" +
                "Пример: /table 01.12.2025 06.12.2025";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String formatDate(LocalDate date) {
        return date.format(OUTPUT_DATE_FORMATTER);
    }
}