package ru.cs.vsu.social_network.telegram_bot.service.command.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.cs.vsu.social_network.telegram_bot.dto.response.ReportResponse;
import ru.cs.vsu.social_network.telegram_bot.dto.response.UserInfoResponse;
import ru.cs.vsu.social_network.telegram_bot.service.UserService;
import ru.cs.vsu.social_network.telegram_bot.service.ReportService;
import ru.cs.vsu.social_network.telegram_bot.service.command.BaseTelegramCommand;
import ru.cs.vsu.social_network.telegram_bot.validation.UserValidator;
import ru.cs.vsu.social_network.telegram_bot.validation.VisitorLogValidator;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Команда для получения отчета за период администратором.
 */
@Slf4j
@Component
public class PeriodReportCommand extends BaseTelegramCommand {

    private static final DateTimeFormatter INPUT_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final ReportService reportService;
    private final VisitorLogValidator visitorLogValidator;

    /**
     * Конструктор команды PeriodReport.
     *
     * @param userService сервис пользователей
     * @param userValidator валидатор пользователей
     * @param reportService сервис отчетов
     * @param visitorLogValidator валидатор журналов посещений
     */
    public PeriodReportCommand(UserService userService, UserValidator userValidator,
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
        checkAndInitStates();

        log.info("{}_PERIOD_REPORT_COMMAND_BEGIN: администратор {}, ввод: {}",
                SERVICE_NAME, telegramId, input);

        try {
            UserInfoResponse user = getUserInfo(telegramId);

            if (!isAdmin(user.getId())) {
                return "Доступ запрещен!\n\nЭта команда доступна только администраторам.";
            }

            visitorLogValidator.validateAdminAccessForLogs(user.getId());

            String[] dates = input.split("\\s+");
            if (dates.length != 2) {
                return "Неверный формат!\n\n" +
                        "Используйте: дата_начала дата_окончания\n" +
                        "Пример: 01.12.2025 06.12.2025";
            }

            LocalDate startDate = parseDate(dates[0]);
            LocalDate endDate = parseDate(dates[1]);

            if (startDate.isAfter(endDate)) {
                return "Дата начала не может быть позже даты окончания!";
            }

            ReportResponse report = reportService.generatePeriodReport(user.getId(), startDate, endDate);

            adminStates.remove(telegramId);

            log.info("{}_PERIOD_REPORT_COMMAND_SUCCESS: отчет за период {} - {} сгенерирован",
                    SERVICE_NAME, startDate, endDate);

            return report.getTelegramFormattedReport();

        } catch (DateTimeParseException e) {
            return "Неверный формат даты!\n\n" +
                    "Используйте формат: ДД.ММ.ГГГГ ДД.ММ.ГГГГ\n" +
                    "Пример: 01.12.2025 06.12.2025";
        } catch (Exception e) {
            log.error("{}_PERIOD_REPORT_COMMAND_ERROR: ошибка для {}: {}",
                    SERVICE_NAME, telegramId, e.getMessage());

            return "Произошла ошибка при генерации отчета.\n\n" +
                    "Проверьте формат дат и попробуйте еще раз.";
        }
    }

    /**
     * Парсит строку с датой.
     *
     * @param dateStr строка с датой
     * @return объект LocalDate
     */
    private LocalDate parseDate(String dateStr) {
        return LocalDate.parse(dateStr.trim(), INPUT_DATE_FORMATTER);
    }
}