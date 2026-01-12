package ru.cs.vsu.social_network.telegram_bot.service.command.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.cs.vsu.social_network.telegram_bot.dto.response.UserInfoResponse;
import ru.cs.vsu.social_network.telegram_bot.service.UserService;
import ru.cs.vsu.social_network.telegram_bot.service.command.BaseTelegramCommand;
import ru.cs.vsu.social_network.telegram_bot.validation.UserValidator;

/**
 * Команда для обработки команд администраторского меню.
 * Управляет состояниями администратора для сбора данных для отчетов.
 */
@Slf4j
@Component
public class AdminMenuCommand extends BaseTelegramCommand {

    private final DailyReportCommand dailyReportCommand;

    /**
     * Конструктор команды AdminMenuCommand.
     *
     * @param userService сервис пользователей
     * @param userValidator валидатор пользователей
     * @param dailyReportCommand команда дневного отчета
     */
    public AdminMenuCommand(UserService userService, UserValidator userValidator,
                            DailyReportCommand dailyReportCommand) {
        super(userService, userValidator);
        this.dailyReportCommand = dailyReportCommand;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String execute(Long telegramId, String input) {
        checkAndInitStates();

        log.info("{}_ADMIN_MENU_COMMAND_BEGIN: администратор {}, команда меню: {}",
                SERVICE_NAME, telegramId, input);

        try {
            UserInfoResponse user = getUserInfo(telegramId);

            if (!isAdmin(user.getId())) {
                return "Доступ запрещен! Эта команда доступна только администраторам.";
            }

            if ("Получить журнал за сегодня".equals(input)) {
                return dailyReportCommand.execute(telegramId, null);
            } else if (input.startsWith("Получить журнал за день")) {
                String datePart = input.replace("Получить журнал за день", "").trim();
                if (datePart.isEmpty()) {
                    adminStates.put(telegramId, "awaiting_specific_date");
                    return "Выберите дату для отчета\n\n" +
                            "Введите дату в формате ДД.ММ.ГГГГ\n" +
                            "Пример: 06.12.2025\n\n" +
                            "Или используйте специальные значения:\n" +
                            "• сегодня\n" +
                            "• вчера";
                } else {
                    return dailyReportCommand.execute(telegramId, datePart);
                }
            } else if ("Получить журнал за период".equals(input)) {
                adminStates.put(telegramId, "awaiting_start_date");
                return "Выберите период для отчета\n\n" +
                        "Введите начальную дату в формате ДД.ММ.ГГГГ\n" +
                        "Пример: 01.12.2025";
            }

            return "Неизвестная команда меню. Пожалуйста, используйте кнопки меню.";

        } catch (Exception e) {
            log.error("{}_ADMIN_MENU_COMMAND_ERROR: ошибка для {}: {}",
                    SERVICE_NAME, telegramId, e.getMessage());
            return "Произошла ошибка при обработке команды меню.";
        }
    }
}