package ru.cs.vsu.social_network.telegram_bot.service.command.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.cs.vsu.social_network.telegram_bot.dto.response.UserInfoResponse;
import ru.cs.vsu.social_network.telegram_bot.service.UserService;
import ru.cs.vsu.social_network.telegram_bot.service.command.BaseTelegramCommand;
import ru.cs.vsu.social_network.telegram_bot.validation.UserValidator;

/**
 * Команда для обработки "Сменить имя".
 * Запрашивает новое имя для обращения к пользователю.
 */
@Slf4j
@Component
public class ChangeNameCommand extends BaseTelegramCommand {

    /**
     * Конструктор команды ChangeName.
     *
     * @param userService сервис пользователей
     * @param userValidator валидатор пользователей
     */
    public ChangeNameCommand(UserService userService, UserValidator userValidator) {
        super(userService, userValidator);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String execute(Long telegramId, String input) {
        checkAndInitStates();

        log.info("{}_CHANGE_NAME_COMMAND_BEGIN: пользователь {} хочет сменить имя",
                SERVICE_NAME, telegramId);

        try {
            UserInfoResponse user = getUserInfo(telegramId);
            userStates.put(telegramId, "awaiting_new_display_name");

            String response = String.format(
                    "%s, вы хотите изменить имя для обращения.\n\n" +
                            "Пожалуйста, введите новое имя и фамилию.\n" +
                            "Пример: Сергей Мордвинов",
                    user.getDisplayName() != null ? user.getDisplayName() : user.getFirstName()
            );

            log.info("{}_CHANGE_NAME_COMMAND_SUCCESS: пользователь {} ожидает ввода нового имени",
                    SERVICE_NAME, telegramId);

            return response;

        } catch (Exception e) {
            log.error("{}_CHANGE_NAME_COMMAND_ERROR: ошибка для {}: {}",
                    SERVICE_NAME, telegramId, e.getMessage());

            return "Произошла ошибка при запросе смены имени.\n\n" +
                    "Пожалуйста, попробуйте позже.";
        }
    }
}