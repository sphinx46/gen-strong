package ru.cs.vsu.social_network.telegram_bot.service.command.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.cs.vsu.social_network.telegram_bot.dto.response.UserInfoResponse;
import ru.cs.vsu.social_network.telegram_bot.service.UserService;
import ru.cs.vsu.social_network.telegram_bot.service.command.BaseTelegramCommand;
import ru.cs.vsu.social_network.telegram_bot.validation.UserValidator;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Команда для обработки ввода даты администратором.
 */
@Slf4j
@Component
public class AdminDateInputCommand extends BaseTelegramCommand {

    private static final DateTimeFormatter INPUT_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final DailyReportCommand dailyReportCommand;
    private final PeriodReportCommand periodReportCommand;

    /**
     * Конструктор команды AdminDateInput.
     *
     * @param userService сервис пользователей
     * @param userValidator валидатор пользователей
     * @param dailyReportCommand команда дневного отчета
     * @param periodReportCommand команда отчетов за период
     */
    public AdminDateInputCommand(UserService userService, UserValidator userValidator,
                                 DailyReportCommand dailyReportCommand,
                                 PeriodReportCommand periodReportCommand) {
        super(userService, userValidator);
        this.dailyReportCommand = dailyReportCommand;
        this.periodReportCommand = periodReportCommand;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String execute(Long telegramId, String input) {
        checkAndInitStates();

        log.info("{}_ADMIN_DATE_INPUT_BEGIN: администратор {}, ввод: {}",
                SERVICE_NAME, telegramId, input);

        try {
            UserInfoResponse user = getUserInfo(telegramId);

            if (!isAdmin(user.getId())) {
                return "Доступ запрещен! Эта команда доступна только администраторам.";
            }

            String state = adminStates.get(telegramId);
            if (state == null) {
                return "Неожиданный запрос. Пожалуйста, используйте команды из меню.";
            }

            if ("awaiting_specific_date".equals(state)) {
                adminStates.remove(telegramId);
                return dailyReportCommand.execute(telegramId, input);
            } else if ("awaiting_start_date".equals(state)) {
                try {
                    LocalDate.parse(input.trim(), INPUT_DATE_FORMATTER);
                    adminStates.put(telegramId, "awaiting_end_date_" + input);
                    return "Теперь введите конечную дату\n\n" +
                            "Формат: ДД.ММ.ГГГГ\n" +
                            "Пример: 06.12.2025";
                } catch (DateTimeParseException e) {
                    return "Неверный формат даты!\n\n" +
                            "Используйте формат: ДД.ММ.ГГГГ\n" +
                            "Пример: 01.12.2025";
                }
            } else if (state.startsWith("awaiting_end_date_")) {
                String startDateStr = state.substring("awaiting_end_date_".length());
                adminStates.remove(telegramId);
                return periodReportCommand.execute(telegramId, startDateStr + " " + input);
            }

            return "Неожиданный запрос. Пожалуйста, используйте команды из меню.";

        } catch (Exception e) {
            log.error("{}_ADMIN_DATE_INPUT_ERROR: ошибка для {}: {}",
                    SERVICE_NAME, telegramId, e.getMessage());
            return "Произошла ошибка при обработке даты.\n\n" +
                    "Проверьте формат и попробуйте еще раз.";
        }
    }
}