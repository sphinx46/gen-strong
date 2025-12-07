package ru.cs.vsu.social_network.telegram_bot.utils.table;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.cs.vsu.social_network.telegram_bot.dto.response.VisitorLogResponse;
import ru.cs.vsu.social_network.telegram_bot.service.ReportService;
import ru.cs.vsu.social_network.telegram_bot.testUtils.TestDataFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TableFormatterServiceImplTest {

    @Mock
    private ReportService reportService;

    @InjectMocks
    private TableFormatterServiceImpl tableFormatterService;

    @Test
    @DisplayName("Форматирование таблицы за сегодня - существующий журнал")
    void formatTableForToday_whenExistingLog_shouldReturnFormattedReport() {
        final String adminUserId = UUID.randomUUID().toString();
        final VisitorLogResponse existingLog = TestDataFactory.createVisitorLogResponse(
                UUID.randomUUID(), LocalDate.now(), 5, 1
        );

        final String result = tableFormatterService.formatTableForToday(
                adminUserId, Optional.of(existingLog)
        );

        assertNotNull(result);
        assertEquals("Тестовый отчет за " + LocalDate.now(), result);
        verify(reportService, never()).generateDailyReport(any());
    }

    @Test
    @DisplayName("Форматирование таблицы за сегодня - новый журнал")
    void formatTableForToday_whenNoExistingLog_shouldGenerateNew() {
        final String adminUserId = UUID.randomUUID().toString();
        final UUID adminUUID = UUID.fromString(adminUserId);
        final VisitorLogResponse newLog = TestDataFactory.createVisitorLogResponse(
                UUID.randomUUID(), LocalDate.now(), 3, 0
        );

        when(reportService.generateDailyReport(adminUUID)).thenReturn(newLog);

        final String result = tableFormatterService.formatTableForToday(
                adminUserId, Optional.empty()
        );

        assertNotNull(result);
        assertEquals("Тестовый отчет за " + LocalDate.now(), result);
        verify(reportService).generateDailyReport(adminUUID);
    }

    @Test
    @DisplayName("Форматирование таблицы за дату - существующий журнал")
    void formatTableForDate_whenExistingLog_shouldReturnFormattedReport() {
        final String adminUserId = UUID.randomUUID().toString();
        final LocalDate date = LocalDate.of(2025, 12, 6);
        final VisitorLogResponse existingLog = TestDataFactory.createVisitorLogResponse(
                UUID.randomUUID(), date, 7, 2
        );

        final String result = tableFormatterService.formatTableForDate(
                adminUserId, date, Optional.of(existingLog)
        );

        assertNotNull(result);
        assertEquals("Тестовый отчет за " + date, result);
        verify(reportService, never()).generateDailyReportForDate(any(), any());
    }

    @Test
    @DisplayName("Форматирование таблицы за период - успешно")
    void formatTableForPeriod_whenLogsProvided_shouldReturnFormattedTable() {
        final LocalDate startDate = LocalDate.of(2025, 12, 1);
        final LocalDate endDate = LocalDate.of(2025, 12, 3);
        final List<VisitorLogResponse> logs = TestDataFactory.createVisitorLogsForPeriod(startDate, endDate);

        final String result = tableFormatterService.formatTableForPeriod(startDate, endDate, logs);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.contains("Таблица посещений тренажерного зала"));
        assertTrue(result.contains("01.12.2025"));
        assertTrue(result.contains("03.12.2025"));
        assertTrue(result.contains("Всего посетителей за период:"));
        assertTrue(result.contains("Итоги:"));
    }

    @Test
    @DisplayName("Форматирование таблицы за период - пустой список")
    void formatTableForPeriod_whenEmptyList_shouldReturnEmptyTable() {
        final LocalDate startDate = LocalDate.of(2025, 12, 1);
        final LocalDate endDate = LocalDate.of(2025, 12, 3);
        final List<VisitorLogResponse> emptyList = List.of();

        final String result = tableFormatterService.formatTableForPeriod(startDate, endDate, emptyList);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.contains("Нет данных о посещениях"));
    }

    @Test
    @DisplayName("Форматирование пустой таблицы за период - успешно")
    void formatPeriodTableEmpty_shouldReturnFormattedString() {
        final LocalDate startDate = LocalDate.of(2025, 12, 1);
        final LocalDate endDate = LocalDate.of(2025, 12, 3);

        final String result = tableFormatterService.formatPeriodTableEmpty(startDate, endDate);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.contains("Таблица посещений тренажерного зала"));
        assertTrue(result.contains("01.12.2025"));
        assertTrue(result.contains("03.12.2025"));
        assertTrue(result.contains("Нет данных о посещениях"));
    }

    @Test
    @DisplayName("Получение инструкций по использованию таблицы - успешно")
    void getTableUsageInstructions_shouldReturnInstructions() {
        final String result = tableFormatterService.getTableUsageInstructions();

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.contains("Использование команды /table"));
        assertTrue(result.contains("/table ДД.ММ.ГГГГ"));
        assertTrue(result.contains("Пример: /table 06.12.2025"));
    }

    @Test
    @DisplayName("Форматирование даты - успешно")
    void formatDate_whenValidDate_shouldReturnFormattedString() {
        final LocalDate date = LocalDate.of(2025, 12, 6);

        final String result = tableFormatterService.formatDate(date);

        assertEquals("06.12.2025", result);
    }
}