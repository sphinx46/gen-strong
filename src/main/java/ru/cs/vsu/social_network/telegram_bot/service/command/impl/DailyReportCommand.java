package ru.cs.vsu.social_network.telegram_bot.service.command.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.cs.vsu.social_network.telegram_bot.dto.response.UserInfoResponse;
import ru.cs.vsu.social_network.telegram_bot.dto.response.VisitorLogResponse;
import ru.cs.vsu.social_network.telegram_bot.service.UserService;
import ru.cs.vsu.social_network.telegram_bot.service.ReportService;
import ru.cs.vsu.social_network.telegram_bot.service.command.BaseTelegramCommand;
import ru.cs.vsu.social_network.telegram_bot.validation.UserValidator;
import ru.cs.vsu.social_network.telegram_bot.validation.VisitorLogValidator;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Команда для получения дневного отчета администратором.
 */
@Slf4j
@Component
public class DailyReportCommand extends BaseTelegramCommand {

    private static final DateTimeFormatter INPUT_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final ReportService reportService;
    private final VisitorLogValidator visitorLogValidator;

    /**
     * Конструктор команды DailyReport.
     *
     * @param userService сервис пользователей
     * @param userValidator валидатор пользователей
     * @param reportService сервис отчетов
     * @param visitorLogValidator валидатор журналов посещений
     */
    public DailyReportCommand(UserService userService, UserValidator userValidator,
                              ReportService reportService, VisitorLogValidator visitorLogValidator) {
        super(userService, userValidator);
        this.reportService = reportService;
        this.visitorLogValidator = visitorLogValidator;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String execute(Long telegramId, String input) {
        log.info("{}_DAILY_REPORT_COMMAND_BEGIN: администратор {}, дата: {}",
                SERVICE_NAME, telegramId, input);

        try {
            UserInfoResponse user = getUserInfo(telegramId);

            if (!isAdmin(user.getId())) {
                return "Доступ запрещен!\n\nЭта команда доступна только администраторам.";
            }

            visitorLogValidator.validateAdminAccessForLogs(user.getId());

            LocalDate date = parseDate(input);

            VisitorLogResponse report = reportService.generateDailyReportForDate(user.getId(), date);

            adminStates.remove(telegramId);

            log.info("{}_DAILY_REPORT_COMMAND_SUCCESS: отчет за {} сгенерирован для администратора {}",
                    SERVICE_NAME, date, telegramId);

            return "Отчет посещений за " + date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) + "\n\n" +
                    report.getFormattedReport();

        } catch (Exception e) {
            log.error("{}_DAILY_REPORT_COMMAND_ERROR: ошибка для {}: {}",
                    SERVICE_NAME, telegramId, e.getMessage());

            return "Произошла ошибка при генерации отчета.\n\n" +
                    "Проверьте формат даты и попробуйте еще раз.";
        }
    }

    /**
     * Парсит строку с датой.
     *
     * @param dateStr строка с датой
     * @return объект LocalDate
     */
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return LocalDate.now();
        }

        String trimmedDate = dateStr.trim();
        if ("сегодня".equalsIgnoreCase(trimmedDate)) {
            return LocalDate.now();
        } else if ("вчера".equalsIgnoreCase(trimmedDate)) {
            return LocalDate.now().minusDays(1);
        } else {
            try {
                return LocalDate.parse(trimmedDate, INPUT_DATE_FORMATTER);
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Неверный формат даты!");
            }
        }
    }
}