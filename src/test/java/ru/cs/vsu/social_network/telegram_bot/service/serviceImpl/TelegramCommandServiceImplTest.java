package ru.cs.vsu.social_network.telegram_bot.service.serviceImpl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.cs.vsu.social_network.telegram_bot.dto.response.ReportResponse;
import ru.cs.vsu.social_network.telegram_bot.dto.response.UserInfoResponse;
import ru.cs.vsu.social_network.telegram_bot.dto.response.VisitResponse;
import ru.cs.vsu.social_network.telegram_bot.dto.response.VisitorLogResponse;
import ru.cs.vsu.social_network.telegram_bot.entity.enums.ROLE;
import ru.cs.vsu.social_network.telegram_bot.service.ReportService;
import ru.cs.vsu.social_network.telegram_bot.service.UserService;
import ru.cs.vsu.social_network.telegram_bot.service.VisitService;
import ru.cs.vsu.social_network.telegram_bot.testUtils.TestDataFactory;
import ru.cs.vsu.social_network.telegram_bot.utils.table.TableFormatterService;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TelegramCommandServiceImplTest {

    private static final Long TELEGRAM_ID = 123456789L;
    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private UserService userService;
    @Mock
    private VisitService visitService;
    @Mock
    private ReportService reportService;
    @Mock
    private TableFormatterService tableFormatterService;

    @InjectMocks
    private TelegramCommandServiceImpl telegramCommandService;

    @Test
    @DisplayName("Обработка команды /start - успешно")
    void handleStartCommand_whenNewUser_shouldReturnWelcomeMessage() {
        final String username = "testuser";
        final String firstName = "Иван";
        final String lastName = "Иванов";

        final UserInfoResponse userResponse = TestDataFactory.createUserInfoResponse(
                USER_ID, TELEGRAM_ID, username, "Иван", ROLE.USER);

        when(userService.registerUser(any())).thenReturn(userResponse);

        final String result = telegramCommandService.handleStartCommand(
                TELEGRAM_ID, username, firstName, lastName);

        assertNotNull(result);
        assertTrue(result.contains("Привет"));
        assertTrue(result.contains("Добро пожаловать"));
        verify(userService).registerUser(any());
    }

    @Test
    @DisplayName("Обработка команды 'Я в зале' - успешно")
    void handleInGymCommand_whenValidUser_shouldReturnSuccessMessage() {
        final UserInfoResponse userResponse = TestDataFactory.createUserInfoResponse(
                USER_ID, TELEGRAM_ID, "testuser", "Иван", ROLE.USER);
        final VisitResponse visitResponse = TestDataFactory.createVisitResponse(
                UUID.randomUUID(), USER_ID, LocalDate.now());

        when(userService.getUserByTelegramId(TELEGRAM_ID)).thenReturn(userResponse);
        when(visitService.createVisitByTelegramId(TELEGRAM_ID)).thenReturn(visitResponse);

        final String result = telegramCommandService.handleInGymCommand(TELEGRAM_ID);

        assertNotNull(result);
        assertTrue(result.contains("успешно отмечены"));
        assertTrue(result.contains("Иван"));
        verify(visitService).createVisitByTelegramId(TELEGRAM_ID);
    }

    @Test
    @DisplayName("Обработка команды 'Я в зале' - уже отмечен")
    void handleInGymCommand_whenAlreadyVisited_shouldReturnWarningMessage() {
        final String visitAlreadyFailureMessage = "Ошибка! Вы уже отметились сегодня";

        when(visitService.createVisitByTelegramId(TELEGRAM_ID))
                .thenThrow(new RuntimeException(visitAlreadyFailureMessage));

        final String result = telegramCommandService.handleInGymCommand(TELEGRAM_ID);

        assertNotNull(result);
        assertTrue(
                result.contains("уже отметились") ||
                        result.contains("Уже отметились") ||
                        result.contains("отметились сегодня"),
                "Результат должен содержать предупреждение о повторной отметке. Результат: " + result
        );
        verify(visitService).createVisitByTelegramId(TELEGRAM_ID);
    }

    @Test
    @DisplayName("Ввод отображаемого имени - успешно")
    void handleDisplayNameInput_whenValidName_shouldReturnSuccessMessage() {
        final String displayName = "Спортсмен";
        final UserInfoResponse userResponse = TestDataFactory.createUserInfoResponse(
                USER_ID, TELEGRAM_ID, "testuser", "СтароеИмя", ROLE.USER);

        when(userService.registerUser(any())).thenReturn(userResponse);
        telegramCommandService.handleStartCommand(TELEGRAM_ID, "testuser", "Иван", "Иванов");

        when(userService.getUserByTelegramId(TELEGRAM_ID)).thenReturn(userResponse);
        when(userService.updateDisplayName(USER_ID, displayName)).thenReturn(userResponse);

        final String result = telegramCommandService.handleDisplayNameInput(TELEGRAM_ID, displayName);

        assertNotNull(result);
        assertTrue(result.contains("Отлично"));
        assertTrue(result.contains(displayName));
        verify(userService).updateDisplayName(USER_ID, displayName);
    }

    @Test
    @DisplayName("Обработка команды отчета за день - успешно")
    void handleDailyReportCommand_whenAdmin_shouldReturnReport() {
        final UserInfoResponse userResponse = TestDataFactory.createUserInfoResponse(
                USER_ID, TELEGRAM_ID, "admin", "Админ", ROLE.ADMIN);
        final VisitorLogResponse reportResponse = TestDataFactory.createVisitorLogResponse(
                UUID.randomUUID(), LocalDate.now(), 5, 1);

        when(userService.getUserByTelegramId(TELEGRAM_ID)).thenReturn(userResponse);
        when(reportService.generateDailyReportForDate(USER_ID, LocalDate.now()))
                .thenReturn(reportResponse);

        final String result = telegramCommandService.handleDailyReportCommand(TELEGRAM_ID, null);

        assertNotNull(result);
        assertEquals(reportResponse.getFormattedReport(), result);
        verify(reportService).generateDailyReportForDate(USER_ID, LocalDate.now());
    }

    @Test
    @DisplayName("Обработка команды отчета за день - не админ")
    void handleDailyReportCommand_whenNotAdmin_shouldReturnAccessDenied() {
        final UserInfoResponse userResponse = TestDataFactory.createUserInfoResponse(
                USER_ID, TELEGRAM_ID, "user", "Пользователь", ROLE.USER);

        when(userService.getUserByTelegramId(TELEGRAM_ID)).thenReturn(userResponse);

        final String result = telegramCommandService.handleDailyReportCommand(TELEGRAM_ID, null);

        assertNotNull(result);
        assertTrue(result.contains("Доступ запрещен"));
        verify(reportService, never()).generateDailyReportForDate(any(), any());
    }

    @Test
    @DisplayName("Обработка команды отчета за период - успешно")
    void handlePeriodReportCommand_whenAdmin_shouldReturnReport() {
        final UserInfoResponse userResponse = TestDataFactory.createUserInfoResponse(
                USER_ID, TELEGRAM_ID, "admin", "Админ", ROLE.ADMIN);
        final ReportResponse reportResponse = TestDataFactory.createReportResponse(
                LocalDate.of(2025, 12, 1),
                LocalDate.of(2025, 12, 3),
                15, 10, 3);

        when(userService.getUserByTelegramId(TELEGRAM_ID)).thenReturn(userResponse);
        when(reportService.generatePeriodReport(eq(USER_ID), any(), any()))
                .thenReturn(reportResponse);

        final String result = telegramCommandService.handlePeriodReportCommand(
                TELEGRAM_ID, "01.12.2025", "03.12.2025");

        assertNotNull(result);
        assertEquals(reportResponse.getTelegramFormattedReport(), result);
        verify(reportService).generatePeriodReport(eq(USER_ID), any(), any());
    }

    @Test
    @DisplayName("Обработка команды таблицы - получение за сегодня")
    void handleTableCommand_whenNoDate_shouldReturnTodayTable() {
        final UserInfoResponse userResponse = TestDataFactory.createUserInfoResponse(
                USER_ID, TELEGRAM_ID, "admin", "Админ", ROLE.ADMIN);

        when(userService.getUserByTelegramId(TELEGRAM_ID)).thenReturn(userResponse);
        when(tableFormatterService.formatTableForToday(eq(USER_ID.toString()), any()))
                .thenReturn("Таблица за сегодня");

        final String result = telegramCommandService.handleTableCommand(TELEGRAM_ID, null);

        assertNotNull(result);
        assertEquals("Таблица за сегодня", result);
        verify(tableFormatterService).formatTableForToday(eq(USER_ID.toString()), any());
    }

    @Test
    @DisplayName("Обработка команды таблицы - получение за дату")
    void handleTableCommand_whenDateProvided_shouldReturnDateTable() {
        final UserInfoResponse userResponse = TestDataFactory.createUserInfoResponse(
                USER_ID, TELEGRAM_ID, "admin", "Админ", ROLE.ADMIN);

        when(userService.getUserByTelegramId(TELEGRAM_ID)).thenReturn(userResponse);
        when(tableFormatterService.formatTableForDate(eq(USER_ID.toString()), any(), any()))
                .thenReturn("Таблица за дату");

        final String result = telegramCommandService.handleTableCommand(TELEGRAM_ID, "06.12.2025");

        assertNotNull(result);
        assertEquals("Таблица за дату", result);
        verify(tableFormatterService).formatTableForDate(eq(USER_ID.toString()), any(), any());
    }

    @Test
    @DisplayName("Обработка неизвестной команды - для обычного пользователя")
    void handleUnknownCommand_whenRegularUser_shouldReturnHelp() {
        final UserInfoResponse userResponse = TestDataFactory.createUserInfoResponse(
                USER_ID, TELEGRAM_ID, "user", "Пользователь", ROLE.USER);

        when(userService.getUserByTelegramId(TELEGRAM_ID)).thenReturn(userResponse);

        final String result = telegramCommandService.handleUnknownCommand(TELEGRAM_ID);

        assertNotNull(result);
        assertTrue(result.contains("я не понял вашу команду"));
        assertTrue(result.contains("Основные команды"));
        assertFalse(result.contains("Команды администратора"));

        assertTrue(result.contains("/start - Начать работу с ботом"));
        assertTrue(result.contains("Я в зале - Отметиться в тренажерном зале"));
        assertTrue(result.contains("Сменить имя - Изменить имя для обращения"));
        assertTrue(result.contains("/help - Показать справку по командам"));
    }

    @Test
    @DisplayName("Обработка неизвестной команды - для администратора")
    void handleUnknownCommand_whenAdmin_shouldReturnAdminHelp() {
        final UserInfoResponse userResponse = TestDataFactory.createUserInfoResponse(
                USER_ID, TELEGRAM_ID, "admin", "Админ", ROLE.ADMIN);

        when(userService.getUserByTelegramId(TELEGRAM_ID)).thenReturn(userResponse);

        final String result = telegramCommandService.handleUnknownCommand(TELEGRAM_ID);

        assertNotNull(result);
        assertTrue(result.contains("я не понял вашу команду"));
        assertTrue(result.contains("Команды администратора"));
    }
}