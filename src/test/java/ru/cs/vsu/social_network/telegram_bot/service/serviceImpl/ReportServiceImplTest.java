package ru.cs.vsu.social_network.telegram_bot.service.serviceImpl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import ru.cs.vsu.social_network.telegram_bot.dto.response.DailyStatsResponse;
import ru.cs.vsu.social_network.telegram_bot.dto.response.ReportResponse;
import ru.cs.vsu.social_network.telegram_bot.dto.response.VisitorLogResponse;
import ru.cs.vsu.social_network.telegram_bot.entity.Visit;
import ru.cs.vsu.social_network.telegram_bot.entity.VisitorLog;
import ru.cs.vsu.social_network.telegram_bot.mapping.EntityMapper;
import ru.cs.vsu.social_network.telegram_bot.provider.VisitEntityProvider;
import ru.cs.vsu.social_network.telegram_bot.provider.VisitorLogEntityProvider;
import ru.cs.vsu.social_network.telegram_bot.repository.VisitorLogRepository;
import ru.cs.vsu.social_network.telegram_bot.testUtils.TestDataFactory;
import ru.cs.vsu.social_network.telegram_bot.utils.factory.VisitorLogFactory;
import ru.cs.vsu.social_network.telegram_bot.utils.report.ReportFormatterService;
import ru.cs.vsu.social_network.telegram_bot.utils.report.ReportStatisticsService;
import ru.cs.vsu.social_network.telegram_bot.validation.VisitorLogValidator;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    private static final UUID ADMIN_ID = UUID.randomUUID();
    private static final UUID LOG_ID = UUID.randomUUID();

    @Mock
    private VisitEntityProvider visitEntityProvider;
    @Mock
    private VisitorLogEntityProvider visitorLogEntityProvider;
    @Mock
    private VisitorLogRepository visitorLogRepository;
    @Mock
    private VisitorLogFactory visitorLogFactory;
    @Mock
    private VisitorLogValidator visitorLogValidator;
    @Mock
    private EntityMapper entityMapper;
    @Mock
    private ReportStatisticsService reportStatisticsService;
    @Mock
    private ReportFormatterService reportFormatterService;

    @InjectMocks
    private ReportServiceImpl reportService;


    @Test
    @DisplayName("Получение журнала по дате - успешно")
    void getVisitorLogByDate_whenLogExists_shouldReturnLog() {
        final LocalDate date = LocalDate.of(2025, 12, 6);
        doNothing().when(visitorLogValidator).validateAdminAccessForLogs(ADMIN_ID);

        final VisitorLog visitorLog = new VisitorLog();
        visitorLog.setId(LOG_ID);
        visitorLog.setLogDate(date);

        when(visitorLogEntityProvider.findByLogDate(date)).thenReturn(Optional.of(visitorLog));

        final VisitorLogResponse expectedResponse = TestDataFactory.createVisitorLogResponse(
                LOG_ID, date, 5, 1);
        when(entityMapper.map(visitorLog, VisitorLogResponse.class)).thenReturn(expectedResponse);

        final Optional<VisitorLogResponse> result = reportService.getVisitorLogByDate(ADMIN_ID, date);

        assertTrue(result.isPresent());
        assertEquals(date, result.get().getLogDate());
        verify(visitorLogValidator).validateAdminAccessForLogs(ADMIN_ID);
    }

    @Test
    @DisplayName("Получение журнала по дате - не найден")
    void getVisitorLogByDate_whenLogNotExists_shouldReturnEmpty() {
        final LocalDate date = LocalDate.of(2025, 12, 6);
        doNothing().when(visitorLogValidator).validateAdminAccessForLogs(ADMIN_ID);

        when(visitorLogEntityProvider.findByLogDate(date)).thenReturn(Optional.empty());

        final Optional<VisitorLogResponse> result = reportService.getVisitorLogByDate(ADMIN_ID, date);

        assertFalse(result.isPresent());
        verify(visitorLogValidator).validateAdminAccessForLogs(ADMIN_ID);
    }

    @Test
    @DisplayName("Получение журналов за период - успешно")
    void getVisitorLogsByPeriod_whenLogsExist_shouldReturnList() {
        final LocalDate startDate = LocalDate.of(2025, 12, 1);
        final LocalDate endDate = LocalDate.of(2025, 12, 3);

        doNothing().when(visitorLogValidator).validateAdminAccessForLogs(ADMIN_ID);

        final List<VisitorLog> logs = new ArrayList<>();
        logs.add(new VisitorLog());
        logs.add(new VisitorLog());

        when(visitorLogEntityProvider.findByPeriod(startDate, endDate)).thenReturn(logs);

        final List<VisitorLogResponse> expectedResponses = List.of(
                TestDataFactory.createVisitorLogResponse(UUID.randomUUID(), startDate, 3, 0),
                TestDataFactory.createVisitorLogResponse(UUID.randomUUID(), startDate.plusDays(1), 5, 1)
        );
        when(entityMapper.mapList(logs, VisitorLogResponse.class)).thenReturn(expectedResponses);

        final List<VisitorLogResponse> result = reportService.getVisitorLogsByPeriod(ADMIN_ID, startDate, endDate);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(visitorLogValidator).validateAdminAccessForLogs(ADMIN_ID);
    }

    @Test
    @DisplayName("Форматирование отчета для Telegram - успешно")
    void formatTelegramReport_whenValidData_shouldReturnFormattedString() {
        final LocalDate date = LocalDate.of(2025, 12, 6);
        final List<String> visitorNames = List.of("Иван Иванов", "Петр Петров");
        final List<String> newUserNames = List.of("Сергей Сергеев");

        when(reportFormatterService.formatDailyTelegramReport(
                eq(date), eq(visitorNames), eq(newUserNames), eq(1)))
                .thenReturn("Отформатированный отчет");

        final String result = reportService.formatTelegramReport(date, visitorNames, newUserNames);

        assertNotNull(result);
        assertEquals("Отформатированный отчет", result);
        verify(reportFormatterService).formatDailyTelegramReport(date, visitorNames, newUserNames, 1);
    }

    @Test
    @DisplayName("Получение всех журналов с пагинацией - успешно")
    void getAllVisitorLogsPaginated_whenLogsExist_shouldReturnPage() {
        final int page = 0;
        final int size = 10;

        doNothing().when(visitorLogValidator).validateAdminAccessForLogs(ADMIN_ID);

        final List<VisitorLog> logs = new ArrayList<>();
        logs.add(new VisitorLog());
        logs.add(new VisitorLog());

        final Page<VisitorLog> logsPage = new PageImpl<>(logs);
        when(visitorLogRepository.findAll(any(Pageable.class))).thenReturn(logsPage);

        final List<VisitorLogResponse> expectedResponses = List.of(
                TestDataFactory.createVisitorLogResponse(UUID.randomUUID(), LocalDate.now(), 3, 0),
                TestDataFactory.createVisitorLogResponse(UUID.randomUUID(), LocalDate.now().minusDays(1), 5, 1)
        );
        when(entityMapper.mapList(logs, VisitorLogResponse.class)).thenReturn(expectedResponses);

        final List<VisitorLogResponse> result = reportService.getAllVisitorLogsPaginated(ADMIN_ID, page, size);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(visitorLogValidator).validateAdminAccessForLogs(ADMIN_ID);
    }

    @Test
    @DisplayName("Получение общего количества журналов - успешно")
    void getTotalVisitorLogsCount_shouldReturnCount() {
        doNothing().when(visitorLogValidator).validateAdminAccessForLogs(ADMIN_ID);

        when(visitorLogRepository.count()).thenReturn(25L);

        final long result = reportService.getTotalVisitorLogsCount(ADMIN_ID);

        assertEquals(25L, result);
        verify(visitorLogValidator).validateAdminAccessForLogs(ADMIN_ID);
    }

    @Test
    @DisplayName("Генерация ежедневного отчета - успешно")
    void generateDailyReport_whenValidAdmin_shouldReturnVisitorLog() {
        doNothing().when(visitorLogValidator).validateAdminAccessForLogs(ADMIN_ID);

        final LocalDate today = LocalDate.now();
        final VisitorLogResponse expectedResponse = TestDataFactory.createVisitorLogResponse(
                LOG_ID, today, 5, 1);

        final List<Visit> visits = TestDataFactory.createVisitsForDate(today, 3);
        when(visitEntityProvider.findAllWithUsersByDate(today)).thenReturn(visits);

        final List<Visit> newUsersVisits = List.of();
        when(visitEntityProvider.findNewUsersByDate(today)).thenReturn(newUsersVisits);

        final VisitorLog visitorLog = new VisitorLog();
        visitorLog.setId(LOG_ID);
        when(visitorLogEntityProvider.findByLogDate(today)).thenReturn(Optional.of(visitorLog));

        when(visitorLogRepository.save(any(VisitorLog.class))).thenReturn(visitorLog);

        when(entityMapper.map(visitorLog, VisitorLogResponse.class)).thenReturn(expectedResponse);

        when(reportFormatterService.formatDailyTelegramReport(any(), any(), any(), anyInt()))
                .thenReturn("Форматированный отчет");

        final VisitorLogResponse result = reportService.generateDailyReport(ADMIN_ID);

        assertNotNull(result);
        assertEquals(LOG_ID, result.getId());
        verify(visitorLogValidator).validateAdminAccessForLogs(ADMIN_ID);
    }

    @Test
    @DisplayName("Генерация отчета по дате - успешно")
    void generateDailyReportForDate_whenValidDate_shouldReturnVisitorLog() {
        final LocalDate date = LocalDate.of(2025, 12, 6);
        doNothing().when(visitorLogValidator).validateAdminAccessForLogs(ADMIN_ID);

        final VisitorLogResponse expectedResponse = TestDataFactory.createVisitorLogResponse(
                LOG_ID, date, 7, 2);

        final List<Visit> visits = TestDataFactory.createVisitsForDate(date, 5);
        when(visitEntityProvider.findAllWithUsersByDate(date)).thenReturn(visits);

        final List<Visit> newUsersVisits = List.of();
        when(visitEntityProvider.findNewUsersByDate(date)).thenReturn(newUsersVisits);

        final VisitorLog visitorLog = new VisitorLog();
        visitorLog.setId(LOG_ID);
        when(visitorLogEntityProvider.findByLogDate(date)).thenReturn(Optional.of(visitorLog));

        when(visitorLogRepository.save(any(VisitorLog.class))).thenReturn(visitorLog);

        when(entityMapper.map(visitorLog, VisitorLogResponse.class)).thenReturn(expectedResponse);

        when(reportFormatterService.formatDailyTelegramReport(any(), any(), any(), anyInt()))
                .thenReturn("Форматированный отчет");

        final VisitorLogResponse result = reportService.generateDailyReportForDate(ADMIN_ID, date);

        assertNotNull(result);
        assertEquals(date, result.getLogDate());
        verify(visitorLogValidator).validateAdminAccessForLogs(ADMIN_ID);
    }

    @Test
    @DisplayName("Генерация отчета за период - успешно")
    void generatePeriodReport_whenValidPeriod_shouldReturnReport() {
        final LocalDate startDate = LocalDate.of(2025, 12, 1);
        final LocalDate endDate = LocalDate.of(2025, 12, 3);

        doNothing().when(visitorLogValidator).validateAdminAccessForLogs(ADMIN_ID);

        final List<Visit> visits = TestDataFactory.createVisitsForDate(startDate, 3);
        when(visitEntityProvider.findAllByDateRange(startDate, endDate)).thenReturn(visits);

        final long[] stats = {15L, 10L};
        when(reportStatisticsService.calculateOverallStatistics(visits)).thenReturn(stats);

        final Map<LocalDate, List<Visit>> visitsByDate = new HashMap<>();
        visitsByDate.put(startDate, visits);
        when(reportStatisticsService.groupVisitsByDate(visits)).thenReturn(visitsByDate);

        when(visitEntityProvider.countNewUsersByDateRange(startDate, endDate)).thenReturn(2);

        final DailyStatsResponse dailyStat = TestDataFactory.createDailyStatsResponse(startDate, 5, 1);
        when(reportStatisticsService.createDailyStats(eq(startDate), anyList())).thenReturn(dailyStat);

        when(reportStatisticsService.calculateAverageDailyVisits(15, 1)).thenReturn(15.0);

        when(reportFormatterService.formatPeriodTelegramReport(
                any(LocalDate.class),
                any(LocalDate.class),
                anyMap(),
                anyLong(),
                anyLong(),
                anyLong(),
                anyDouble()))
                .thenReturn("Тестовый отчет");

        final ReportResponse result = reportService.generatePeriodReport(ADMIN_ID, startDate, endDate);

        assertNotNull(result);
        assertEquals(startDate, result.getStartDate());
        assertEquals(endDate, result.getEndDate());
        assertEquals(15, result.getTotalVisits());
        assertEquals(2, result.getTotalNewUsers());
        verify(visitorLogValidator).validateAdminAccessForLogs(ADMIN_ID);
    }
}