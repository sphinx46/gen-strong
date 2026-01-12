package ru.cs.vsu.social_network.telegram_bot.service.command.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.cs.vsu.social_network.telegram_bot.dto.response.UserInfoResponse;
import ru.cs.vsu.social_network.telegram_bot.service.UserService;
import ru.cs.vsu.social_network.telegram_bot.service.command.BaseTelegramCommand;
import ru.cs.vsu.social_network.telegram_bot.validation.UserValidator;

/**
 * Команда для обработки ввода имени пользователя.
 * Сохраняет имя, которое будет использоваться для обращения к пользователю.
 */
@Slf4j
@Component
public class DisplayNameInputCommand extends BaseTelegramCommand {

    /**
     * Конструктор команды DisplayNameInput.
     *
     * @param userService сервис пользователей
     * @param userValidator валидатор пользователей
     */
    public DisplayNameInputCommand(UserService userService, UserValidator userValidator) {
        super(userService, userValidator);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String execute(Long telegramId, String input) {
        checkAndInitStates();

        log.info("{}_DISPLAY_NAME_INPUT_BEGIN: обработка имени '{}' для Telegram ID: {}",
                SERVICE_NAME, input, telegramId);

        String userState = userStates.get(telegramId);

        if ("awaiting_display_name".equals(userState)) {
            return processInitialNameInput(telegramId, input);
        } else if ("awaiting_new_display_name".equals(userState)) {
            return processNameChange(telegramId, input);
        } else {
            log.warn("{}_DISPLAY_NAME_INPUT_UNEXPECTED: Telegram ID {} не ожидает ввода имени",
                    SERVICE_NAME, telegramId);
            return "Неожиданный запрос. Пожалуйста, используйте команды из меню.";
        }
    }

    /**
     * Обрабатывает первичный ввод имени при регистрации.
     *
     * @param telegramId идентификатор пользователя
     * @param displayName введенное имя
     * @return результат обработки
     */
    private String processInitialNameInput(Long telegramId, String displayName) {
        try {
            UserInfoResponse user = getUserInfo(telegramId);
            userService.updateDisplayName(user.getId(), displayName.trim());
            userStates.remove(telegramId);

            String response = String.format(
                    "Отлично, %s!\n\n" +
                            "Теперь я буду обращаться к вам так.\n\n" +
                            "Доступные команды:\n" +
                            "• Я в зале — Отметиться в зале\n" +
                            "• Сменить имя — Изменить имя для обращения\n" +
                            "• Составить программу тренировок — Создать индивидуальную программу\n" +
                            "• /help — Показать справку по командам",
                    displayName.trim()
            );

            log.info("{}_DISPLAY_NAME_INPUT_SUCCESS: имя пользователя {} обновлено на '{}'",
                    SERVICE_NAME, telegramId, displayName);

            return response;

        } catch (Exception e) {
            log.error("{}_DISPLAY_NAME_INPUT_ERROR: ошибка при сохранении имени для {}: {}",
                    SERVICE_NAME, telegramId, e.getMessage());

            return "Произошла ошибка при сохранении имени.\n\n" +
                    "Пожалуйста, попробуйте еще раз.";
        }
    }

    /**
     * Обрабатывает изменение имени.
     *
     * @param telegramId идентификатор пользователя
     * @param displayName введенное имя
     * @return результат обработки
     */
    private String processNameChange(Long telegramId, String displayName) {
        try {
            UserInfoResponse user = getUserInfo(telegramId);
            userService.updateDisplayName(user.getId(), displayName.trim());
            userStates.remove(telegramId);

            String response = String.format(
                    "Имя успешно изменено!\n\n" +
                            "Теперь я буду обращаться к вам как %s.",
                    displayName.trim()
            );

            log.info("{}_CHANGE_NAME_SUCCESS: имя пользователя {} изменено на '{}'",
                    SERVICE_NAME, telegramId, displayName);

            return response;

        } catch (Exception e) {
            log.error("{}_CHANGE_NAME_ERROR: ошибка при изменении имени для {}: {}",
                    SERVICE_NAME, telegramId, e.getMessage());

            return "Произошла ошибка при изменении имени.\n\n" +
                    "Пожалуйста, попробуйте еще раз.";
        }
    }
}