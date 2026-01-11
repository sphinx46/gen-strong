package ru.cs.vsu.social_network.telegram_bot.service.command.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.cs.vsu.social_network.telegram_bot.dto.response.UserInfoResponse;
import ru.cs.vsu.social_network.telegram_bot.entity.enums.ROLE;
import ru.cs.vsu.social_network.telegram_bot.service.UserService;
import ru.cs.vsu.social_network.telegram_bot.service.command.BaseTelegramCommand;
import ru.cs.vsu.social_network.telegram_bot.validation.UserValidator;

/**
 * Команда для обработки /help.
 * Возвращает справку по доступным командам.
 */
@Slf4j
@Component
public class HelpCommand extends BaseTelegramCommand {

    /**
     * Конструктор команды Help.
     *
     * @param userService сервис пользователей
     * @param userValidator валидатор пользователей
     */
    public HelpCommand(UserService userService, UserValidator userValidator) {
        super(userService, userValidator);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String execute(Long telegramId, String input) {
        log.info("{}_HELP_COMMAND_BEGIN: обработка команды /help для Telegram ID: {}",
                SERVICE_NAME, telegramId);

        try {
            UserInfoResponse user = getUserInfo(telegramId);
            String displayName = user.getDisplayName() != null ?
                    user.getDisplayName() : user.getFirstName();

            StringBuilder response = new StringBuilder();
            response.append(String.format("Справка по командам, %s!\n\n", displayName));

            response.append("Основные команды:\n");
            response.append("• /start — Начать работу с ботом\n");
            response.append("• Я в зале — Отметиться в тренажерном зале\n");
            response.append("• Сменить имя — Изменить имя для обращения\n");
            response.append("• Составить программу тренировок — Создать индивидуальную программу\n");
            response.append("• /help — Показать эту справку\n");

            if (user.getRole() == ROLE.ADMIN) {
                response.append("\nКоманды администратора:\n");
                response.append("• /report — Отчет посещений за сегодня\n");
                response.append("• /report дата — Отчет за определенный день\n");
                response.append("  Пример: /report 06.12.2025\n");
                response.append("• /report period начало конец — Отчет за период\n");
                response.append("  Пример: /report period 01.12.2025 06.12.2025\n");
                response.append("• /table — Таблица посещений за сегодня\n");
                response.append("• /table дата — Таблица за определенный день\n");
                response.append("• /table дата-начало дата-конец — Таблица за период\n");

                response.append("\nКнопки меню администратора:\n");
                response.append("• Получить журнал за сегодня\n");
                response.append("• Получить журнал за день\n");
                response.append("• Получить журнал за период\n");
            }

            response.append("\nИспользуйте кнопки меню или введите команду вручную.");

            log.info("{}_HELP_COMMAND_SUCCESS: справка отправлена пользователю {}",
                    SERVICE_NAME, telegramId);

            return response.toString();

        } catch (Exception e) {
            log.error("{}_HELP_COMMAND_ERROR: ошибка при обработке команды /help для {}: {}",
                    SERVICE_NAME, telegramId, e.getMessage());

            return "Добро пожаловать в тренажерный зал!\n\n" +
                    "Для начала работы введите команду /start";
        }
    }
}