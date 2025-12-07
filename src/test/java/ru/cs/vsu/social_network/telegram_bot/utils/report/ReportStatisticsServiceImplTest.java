package ru.cs.vsu.social_network.telegram_bot.utils.report;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.cs.vsu.social_network.telegram_bot.dto.response.DailyStatsResponse;
import ru.cs.vsu.social_network.telegram_bot.entity.Visit;
import ru.cs.vsu.social_network.telegram_bot.testUtils.TestDataFactory;
import ru.cs.vsu.social_network.telegram_bot.utils.report.reportImpl.ReportStatisticsServiceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ReportStatisticsServiceImplTest {

    @InjectMocks
    private ReportStatisticsServiceImpl reportStatisticsService;

    @Test
    @DisplayName("Расчет общей статистики - успешно")
    void calculateOverallStatistics_whenVisitsProvided_shouldReturnCorrectCounts() {
        final List<Visit> visits = new ArrayList<>();
        final UUID userId1 = UUID.randomUUID();
        final UUID userId2 = UUID.randomUUID();

        for (int i = 0; i < 3; i++) {
            visits.add(TestDataFactory.createVisit(
                    UUID.randomUUID(),
                    TestDataFactory.createUser(userId1, "user1", "User 1"),
                    LocalDateTime.now().plusHours(i)
            ));
        }

        visits.add(TestDataFactory.createVisit(
                UUID.randomUUID(),
                TestDataFactory.createUser(userId2, "user2", "User 2"),
                LocalDateTime.now().plusHours(4)
        ));

        final long[] result = reportStatisticsService.calculateOverallStatistics(visits);

        assertNotNull(result);
        assertEquals(2, result.length);
        assertEquals(4, result[0]);
        assertEquals(2, result[1]);
    }

    @Test
    @DisplayName("Расчет общей статистики - пустой список")
    void calculateOverallStatistics_whenEmptyList_shouldReturnZeroes() {
        final List<Visit> emptyList = Collections.emptyList();

        final long[] result = reportStatisticsService.calculateOverallStatistics(emptyList);

        assertNotNull(result);
        assertEquals(2, result.length);
        assertEquals(0, result[0]);
        assertEquals(0, result[1]);
    }

    @Test
    @DisplayName("Группировка посещений по датам - успешно")
    void groupVisitsByDate_whenVisitsProvided_shouldReturnGroupedMap() {
        final List<Visit> visits = new ArrayList<>();
        final LocalDate today = LocalDate.now();
        final LocalDate yesterday = today.minusDays(1);

        visits.add(TestDataFactory.createVisit(
                UUID.randomUUID(),
                TestDataFactory.createUser(UUID.randomUUID(), "user1", "User 1"),
                yesterday.atTime(10, 0)
        ));

        visits.add(TestDataFactory.createVisit(
                UUID.randomUUID(),
                TestDataFactory.createUser(UUID.randomUUID(), "user2", "User 2"),
                today.atTime(11, 0)
        ));

        visits.add(TestDataFactory.createVisit(
                UUID.randomUUID(),
                TestDataFactory.createUser(UUID.randomUUID(), "user3", "User 3"),
                today.atTime(12, 0)
        ));

        final Map<LocalDate, List<Visit>> result = reportStatisticsService.groupVisitsByDate(visits);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.containsKey(yesterday));
        assertTrue(result.containsKey(today));
        assertEquals(1, result.get(yesterday).size());
        assertEquals(2, result.get(today).size());
    }

    @Test
    @DisplayName("Расчет среднего количества посещений в день - успешно")
    void calculateAverageDailyVisits_whenValidData_shouldReturnCorrectAverage() {
        final long totalVisits = 30;
        final int daysWithVisits = 10;

        final double result = reportStatisticsService.calculateAverageDailyVisits(totalVisits, daysWithVisits);

        assertEquals(3.0, result, 0.01);
    }

    @Test
    @DisplayName("Расчет среднего количества посещений в день - ноль дней")
    void calculateAverageDailyVisits_whenZeroDays_shouldReturnZero() {
        final long totalVisits = 0;
        final int daysWithVisits = 0;

        final double result = reportStatisticsService.calculateAverageDailyVisits(totalVisits, daysWithVisits);

        assertEquals(0.0, result, 0.01);
    }

    @Test
    @DisplayName("Генерация ежедневной статистики за период - успешно")
    void generateDailyStatsForPeriod_whenValidPeriod_shouldReturnStatsList() {
        final LocalDate startDate = LocalDate.of(2025, 12, 1);
        final LocalDate endDate = LocalDate.of(2025, 12, 3);

        final Map<LocalDate, List<Visit>> visitsByDate = new HashMap<>();

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            visitsByDate.put(date, TestDataFactory.createVisitsForDate(date, 2));
        }

        final List<DailyStatsResponse> result = reportStatisticsService
                .generateDailyStatsForPeriod(startDate, endDate, visitsByDate);

        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals(startDate, result.get(0).getDate());
        assertEquals(endDate, result.get(2).getDate());
    }

    @Test
    @DisplayName("Подсчет новых пользователей за день - успешно")
    void countNewUsersForDate_whenVisitsProvided_shouldReturnCorrectCount() {
        final LocalDate date = LocalDate.now();
        final List<Visit> visits = TestDataFactory.createVisitsForDate(date, 5);

        final int result = reportStatisticsService.countNewUsersForDate(date, visits);

        assertEquals(5, result);
    }

    @Test
    @DisplayName("Подсчет новых пользователей за день - пустой список")
    void countNewUsersForDate_whenEmptyList_shouldReturnZero() {
        final LocalDate date = LocalDate.now();
        final List<Visit> emptyList = Collections.emptyList();

        final int result = reportStatisticsService.countNewUsersForDate(date, emptyList);

        assertEquals(0, result);
    }

    @Test
    @DisplayName("Создание статистики за день - успешно")
    void createDailyStats_whenVisitsProvided_shouldReturnCorrectStats() {
        final LocalDate date = LocalDate.now();
        final List<Visit> visits = TestDataFactory.createVisitsForDate(date, 3);

        final DailyStatsResponse result = reportStatisticsService.createDailyStats(date, visits);

        assertNotNull(result);
        assertEquals(date, result.getDate());
        assertEquals(3, result.getVisitorCount());
        assertEquals(3, result.getNewUsersCount());
        assertEquals(3, result.getVisitorNames().size());
    }
}