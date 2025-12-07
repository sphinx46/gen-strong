package ru.cs.vsu.social_network.telegram_bot.utils.report;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.cs.vsu.social_network.telegram_bot.dto.response.DailyStatsResponse;
import ru.cs.vsu.social_network.telegram_bot.testUtils.TestDataFactory;
import ru.cs.vsu.social_network.telegram_bot.utils.report.reportImpl.ReportFormatterServiceImpl;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ReportFormatterServiceImplTest {

    @InjectMocks
    private ReportFormatterServiceImpl reportFormatterService;

    @Test
    @DisplayName("Форматирование отчета за период для Telegram - успешно")
    void formatPeriodTelegramReport_whenValidData_shouldReturnFormattedString() {
        final LocalDate startDate = LocalDate.of(2025, 12, 1);
        final LocalDate endDate = LocalDate.of(2025, 12, 3);

        final Map<LocalDate, DailyStatsResponse> dailyStats =
                TestDataFactory.createDailyStatsMap(startDate, endDate);

        final long totalVisits = 20;
        final long uniqueVisitors = 15;
        final long totalNewUsers = 3;
        final double averageDailyVisits = 6.7;

        final String result = reportFormatterService.formatPeriodTelegramReport(
                startDate, endDate, dailyStats, totalVisits, uniqueVisitors,
                totalNewUsers, averageDailyVisits
        );

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.contains("Отчет о посещаемости"));
        assertTrue(result.contains("01.12.2025"));
        assertTrue(result.contains("Всего посещений: 20"));
        assertTrue(result.contains("Количество новых участников: 3"));
    }

    @Test
    @DisplayName("Форматирование даты - успешно")
    void formatDate_whenValidDate_shouldReturnFormattedString() {
        final LocalDate date = LocalDate.of(2025, 12, 6);

        final String result = reportFormatterService.formatDate(date);

        assertEquals("06.12.2025", result);
    }

    @Test
    @DisplayName("Форматирование ежедневной статистики - успешно")
    void formatDailyStat_whenValidStat_shouldReturnFormattedString() {
        final DailyStatsResponse dailyStat = TestDataFactory.createDailyStatsResponse(
                LocalDate.of(2025, 12, 6), 8, 2
        );

        final String result = reportFormatterService.formatDailyStat(dailyStat);

        assertEquals("06.12.2025: 8 чел.", result);
    }

    @Test
    @DisplayName("Форматирование ежедневного отчета для Telegram - без посетителей")
    void formatDailyTelegramReport_whenNoVisitors_shouldReturnFormattedString() {
        final LocalDate date = LocalDate.of(2025, 12, 6);
        final List<String> visitorNames = List.of();
        final List<String> newUserNames = List.of();
        final int newUsersCount = 0;

        final String result = reportFormatterService.formatDailyTelegramReport(
                date, visitorNames, newUserNames, newUsersCount
        );

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.contains("Журнал посещений"));
        assertTrue(result.contains("06.12.2025"));
        assertTrue(result.contains("В этот день посетителей не было"));
    }

    @Test
    @DisplayName("Форматирование списка посетителей - успешно")
    void formatVisitorList_whenValidNames_shouldReturnFormattedString() {
        final List<String> visitorNames = List.of("Иван Иванов", "Петр Петров", "Сергей Сергеев");

        final String result = reportFormatterService.formatVisitorList(visitorNames);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.contains("1. Иван Иванов"));
        assertTrue(result.contains("2. Петр Петров"));
        assertTrue(result.contains("3. Сергей Сергеев"));
    }

    @Test
    @DisplayName("Форматирование списка посетителей - пустой список")
    void formatVisitorList_whenEmptyList_shouldReturnEmptyString() {
        final List<String> emptyList = List.of();

        final String result = reportFormatterService.formatVisitorList(emptyList);

        assertNotNull(result);
        assertEquals("", result);
    }
}