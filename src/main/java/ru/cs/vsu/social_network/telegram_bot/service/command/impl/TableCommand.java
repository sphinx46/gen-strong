package ru.cs.vsu.social_network.telegram_bot.service.command.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.cs.vsu.social_network.telegram_bot.dto.response.UserInfoResponse;
import ru.cs.vsu.social_network.telegram_bot.service.UserService;
import ru.cs.vsu.social_network.telegram_bot.service.ReportService;
import ru.cs.vsu.social_network.telegram_bot.service.command.BaseTelegramCommand;
import ru.cs.vsu.social_network.telegram_bot.utils.table.TableFormatterService;
import ru.cs.vsu.social_network.telegram_bot.validation.UserValidator;
import ru.cs.vsu.social_network.telegram_bot.validation.VisitorLogValidator;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.UUID;

/**
 * Команда для получения таблицы посещений администратором.
 */
@Slf4j
@Component
public class TableCommand extends BaseTelegramCommand {

    private static final DateTimeFormatter INPUT_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final ReportService reportService;
    private final TableFormatterService tableFormatterService;
    private final VisitorLogValidator visitorLogValidator;

    /**
     * Конструктор команды Table.
     *
     * @param userService сервис пользователей
     * @param userValidator валидатор пользователей
     * @param reportService сервис отчетов
     * @param tableFormatterService сервис форматирования таблиц
     * @param visitorLogValidator валидатор журналов посещений
     */
    public TableCommand(UserService userService, UserValidator userValidator,
                        ReportService reportService, TableFormatterService tableFormatterService,
                        VisitorLogValidator visitorLogValidator) {
        super(userService, userValidator);
        this.reportService = reportService;
        this.tableFormatterService = tableFormatterService;
        this.visitorLogValidator = visitorLogValidator;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String execute(Long telegramId, String input) {
        log.info("{}_TABLE_COMMAND_BEGIN: администратор {}, ввод: {}",
                SERVICE_NAME, telegramId, input);

        try {
            checkAndInitStates();
            UserInfoResponse user = getUserInfo(telegramId);

            if (!isAdmin(user.getId())) {
                log.warn("{}_TABLE_COMMAND_ACCESS_DENIED: пользователь {} не является администратором",
                        SERVICE_NAME, telegramId);
                return "Доступ запрещен! Эта команда доступна только администраторам.";
            }

            visitorLogValidator.validateAdminAccessForLogs(user.getId());

            if (input == null || input.trim().isEmpty()) {
                log.info("{}_TABLE_COMMAND_GET_TODAY: администратор {}", SERVICE_NAME, telegramId);
                return getTableForToday(user.getId());
            }

            String[] parts = input.trim().split("\\s+");

            if (parts.length == 1) {
                return getTableForDate(user.getId(), parts[0]);
            } else if (parts.length == 2) {
                return getTableForPeriod(user.getId(), parts[0], parts[1]);
            } else {
                log.warn("{}_TABLE_COMMAND_INVALID_FORMAT: неверное количество параметров: {}",
                        SERVICE_NAME, parts.length);
                return tableFormatterService.getTableUsageInstructions();
            }

        } catch (Exception e) {
            log.error("{}_TABLE_COMMAND_ERROR: ошибка при обработке команды для {}: {}",
                    SERVICE_NAME, telegramId, e.getMessage(), e);
            return "Произошла ошибка при получении таблицы.\n\n" +
                    "Проверьте формат даты и попробуйте еще раз.";
        }
    }

    /**
     * Получает таблицу посещений за текущий день.
     *
     * @param adminUserId идентификатор администратора
     * @return таблица посещений за сегодня
     */
    private String getTableForToday(UUID adminUserId) {
        log.info("{}_TABLE_FOR_TODAY_BEGIN: администратор {}", SERVICE_NAME, adminUserId);

        LocalDate today = LocalDate.now();
        Optional<ru.cs.vsu.social_network.telegram_bot.dto.response.VisitorLogResponse> existingLog =
                reportService.getVisitorLogByDate(adminUserId, today);

        return tableFormatterService.formatTableForToday(adminUserId.toString(), existingLog);
    }

    /**
     * Получает таблицу посещений за указанную дату.
     *
     * @param adminUserId идентификатор администратора
     * @param dateStr строка с датой
     * @return таблица посещений за указанную дату
     */
    private String getTableForDate(UUID adminUserId, String dateStr) {
        log.info("{}_TABLE_FOR_DATE_BEGIN: администратор {}, дата: {}",
                SERVICE_NAME, adminUserId, dateStr);

        try {
            LocalDate date = LocalDate.parse(dateStr.trim(), INPUT_DATE_FORMATTER);
            Optional<ru.cs.vsu.social_network.telegram_bot.dto.response.VisitorLogResponse> existingLog =
                    reportService.getVisitorLogByDate(adminUserId, date);

            return tableFormatterService.formatTableForDate(adminUserId.toString(), date, existingLog);
        } catch (DateTimeParseException e) {
            log.warn("{}_TABLE_FOR_DATE_INVALID_FORMAT: неверный формат даты: {}",
                    SERVICE_NAME, dateStr);
            return "Неверный формат даты!\n\n" +
                    "Используйте формат: ДД.ММ.ГГГГ\n" +
                    "Пример: /report 06.12.2025";
        }
    }

    /**
     * Получает таблицу посещений за указанный период.
     *
     * @param adminUserId идентификатор администратора
     * @param startDateStr начальная дата периода
     * @param endDateStr конечная дата периода
     * @return таблица посещений за период
     */
    private String getTableForPeriod(UUID adminUserId, String startDateStr, String endDateStr) {
        log.info("{}_TABLE_FOR_PERIOD_BEGIN: администратор {}, период: {} - {}",
                SERVICE_NAME, adminUserId, startDateStr, endDateStr);

        try {
            LocalDate startDate = LocalDate.parse(startDateStr.trim(), INPUT_DATE_FORMATTER);
            LocalDate endDate = LocalDate.parse(endDateStr.trim(), INPUT_DATE_FORMATTER);

            if (startDate.isAfter(endDate)) {
                log.warn("{}_TABLE_FOR_PERIOD_INVALID_DATES: дата начала {} позже даты окончания {}",
                        SERVICE_NAME, startDate, endDate);
                return "Дата начала не может быть позже даты окончания!";
            }

            var logs = reportService.getVisitorLogsByPeriod(adminUserId, startDate, endDate);

            if (logs.isEmpty()) {
                return tableFormatterService.formatPeriodTableEmpty(startDate, endDate);
            }

            return tableFormatterService.formatTableForPeriod(startDate, endDate, logs);

        } catch (DateTimeParseException e) {
            log.warn("{}_TABLE_FOR_PERIOD_INVALID_FORMAT: неверный формат дат: {} - {}",
                    SERVICE_NAME, startDateStr, endDateStr);
            return "Неверный формат даты!\n\n" +
                    "Используйте формат: ДД.ММ.ГГГГ ДД.ММ.ГГГГ\n" +
                    "Пример: /report period 01.12.2025 06.12.2025";
        }
    }
}