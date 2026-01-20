package ru.cs.vsu.social_network.telegram_bot.service.command.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.cs.vsu.social_network.telegram_bot.dto.response.UserInfoResponse;
import ru.cs.vsu.social_network.telegram_bot.entity.enums.ROLE;
import ru.cs.vsu.social_network.telegram_bot.service.UserService;
import ru.cs.vsu.social_network.telegram_bot.service.command.BaseTelegramCommand;
import ru.cs.vsu.social_network.telegram_bot.validation.UserValidator;

@Slf4j
@Component
public class HelpCommand extends BaseTelegramCommand {

    public HelpCommand(UserService userService, UserValidator userValidator) {
        super(userService, userValidator);
    }

    @Override
    public String execute(Long telegramId, String input) {
        checkAndInitStates();

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
            response.append("• Составить программу по жиму — Создать индивидуальную программу тренировок по жиму лежа в Excel\n");
            response.append("• Внести вклад в развитие — Поддержать проект\n");
            response.append("• /help — Показать эту справку\n");

            if (user.getRole() == ROLE.ADMIN) {
                response.append("\nКоманды администратора доступны через кнопки меню:\n");
                response.append("• Получить журнал за сегодня\n");
                response.append("• Получить журнал за день\n");
                response.append("• Получить журнал за период\n");
            }

            response.append("\nОбщая информация о клубе:\n");
            response.append("Текст...\n");
            response.append("Ссылка на группу в телеграм: https://t.me/pokoleniesil\n");
            response.append("Ссылка на чат в телеграм: @chatpokolenie\n");



            response.append("\nТехническая информация:\n");
            response.append("Наш проект open-source. Страница на github: https://github.com/sphinx46/gen-strong\n");
            response.append("Наша команда открыта к предложениям!\n");
            response.append("Если вы заметили ошибку или хотите предложить улучшение, просьба написать в личные сообщения разработчику.\n");
            response.append("Также можно обратиться за разработкой приложения/телеграм-бота под ваши индивидуальные цели на заказ.\n");
            response.append("Разработчик в Telegram: @BGsphinx\n");


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