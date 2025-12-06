package ru.cs.vsu.social_network.telegram_bot.utils.report.reportImpl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.cs.vsu.social_network.telegram_bot.dto.response.DailyStatsResponse;
import ru.cs.vsu.social_network.telegram_bot.utils.report.ReportFormatterService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Реализация сервиса для форматирования отчетов и статистики.
 * Обеспечивает преобразование данных в удобочитаемые форматы для Telegram и других каналов.
 */
@Slf4j
@Service
public class ReportFormatterServiceImpl implements ReportFormatterService {

    private static final String SERVICE_NAME = "ФОРМАТИРОВАТЕЛЬ_ОТЧЕТОВ";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    /**
     * {@inheritDoc}
     */
    @Override
    public String formatDailyTelegramReport(LocalDate date, List<String> visitorNames) {
        log.debug("{}_ФОРМАТИРОВАНИЕ_ЕЖЕДНЕВНОГО_ОТЧЕТА_НАЧАЛО: дата: {}, посетителей: {}",
                SERVICE_NAME, date, visitorNames.size());

        StringBuilder report = new StringBuilder();
        String formattedDate = formatDate(date);

        report.append("📊 *Журнал посещений тренажерного зала*\n");
        report.append("📅 *Дата:* ").append(formattedDate).append("\n");
        report.append("👥 *Посетители:* ").append(visitorNames.size()).append("\n\n");

        if (visitorNames.isEmpty()) {
            report.append("❌ *В этот день посетителей не было*");
        } else {
            report.append("*Список посетителей:*\n");
            report.append(formatVisitorList(visitorNames));
        }

        String result = report.toString();
        log.debug("{}_ФОРМАТИРОВАНИЕ_ЕЖЕДНЕВНОГО_ОТЧЕТА_УСПЕХ: отчет сформирован, длина: {}",
                SERVICE_NAME, result.length());

        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String formatPeriodTelegramReport(LocalDate startDate, LocalDate endDate,
                                             Map<LocalDate, DailyStatsResponse> dailyStats,
                                             long totalVisits, long uniqueVisitors,
                                             double averageDailyVisits) {
        log.debug("{}_ФОРМАТИРОВАНИЕ_ОТЧЕТА_ЗА_ПЕРИОД_НАЧАЛО: период {} - {}",
                SERVICE_NAME, startDate, endDate);

        StringBuilder report = new StringBuilder();

        String startDateStr = formatDate(startDate);
        String endDateStr = formatDate(endDate);

        report.append("📈 *Отчет о посещаемости тренажерного зала*\n");
        report.append("📅 *Период:* ").append(startDateStr).append(" - ").append(endDateStr).append("\n\n");

        report.append("*Общая статистика:*\n");
        report.append("• Всего посещений: ").append(totalVisits).append("\n");
        report.append("• Уникальных посетителей: ").append(uniqueVisitors).append("\n");
        report.append("• Среднее в день: ").append(String.format("%.1f", averageDailyVisits)).append("\n\n");

        report.append("*Ежедневная посещаемость:*\n");
        for (Map.Entry<LocalDate, DailyStatsResponse> entry : dailyStats.entrySet()) {
            LocalDate date = entry.getKey();
            DailyStatsResponse stat = entry.getValue();

            String dateStr = formatDate(date);
            report.append("• ").append(dateStr).append(": ")
                    .append(stat.getVisitorCount()).append(" чел.\n");
        }

        String result = report.toString();
        log.debug("{}_ФОРМАТИРОВАНИЕ_ОТЧЕТА_ЗА_ПЕРИОД_УСПЕХ: отчет сформирован, длина: {}",
                SERVICE_NAME, result.length());

        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String formatVisitorList(List<String> visitorNames) {
        log.debug("{}_ФОРМАТИРОВАНИЕ_СПИСКА_ПОСЕТИТЕЛЕЙ_НАЧАЛО: посетителей: {}",
                SERVICE_NAME, visitorNames.size());

        StringBuilder list = new StringBuilder();
        for (int i = 0; i < visitorNames.size(); i++) {
            list.append(i + 1).append(". ").append(visitorNames.get(i)).append("\n");
        }

        String result = list.toString();
        log.debug("{}_ФОРМАТИРОВАНИЕ_СПИСКА_ПОСЕТИТЕЛЕЙ_УСПЕХ: список сформирован",
                SERVICE_NAME);

        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String formatDate(LocalDate date) {
        return date.format(DATE_FORMATTER);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String formatDailyStat(DailyStatsResponse dailyStat) {
        String dateStr = formatDate(dailyStat.getDate());
        return String.format("%s: %d чел.", dateStr, dailyStat.getVisitorCount());
    }
}