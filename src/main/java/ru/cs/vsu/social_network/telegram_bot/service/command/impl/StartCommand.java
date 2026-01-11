package ru.cs.vsu.social_network.telegram_bot.service.command.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.cs.vsu.social_network.telegram_bot.dto.request.UserCreateRequest;
import ru.cs.vsu.social_network.telegram_bot.dto.response.UserInfoResponse;
import ru.cs.vsu.social_network.telegram_bot.service.UserService;
import ru.cs.vsu.social_network.telegram_bot.service.command.BaseTelegramCommand;
import ru.cs.vsu.social_network.telegram_bot.validation.UserValidator;

/**
 * Команда для обработки /start.
 * Регистрирует нового пользователя или приветствует существующего.
 */
@Slf4j
@Component
public class StartCommand extends BaseTelegramCommand {

    /**
     * Конструктор команды Start.
     *
     * @param userService сервис пользователей
     * @param userValidator валидатор пользователей
     */
    public StartCommand(UserService userService, UserValidator userValidator) {
        super(userService, userValidator);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String execute(Long telegramId, String input) {
        log.info("{}_START_COMMAND_BEGIN: обработка команды /start для Telegram ID: {}",
                SERVICE_NAME, telegramId);

        String[] userData = input != null ? input.split("\\|") : new String[3];
        String username = userData.length > 0 ? userData[0] : null;
        String firstName = userData.length > 1 ? userData[1] : null;
        String lastName = userData.length > 2 ? userData[2] : null;

        UserCreateRequest createRequest = UserCreateRequest.builder()
                .telegramId(telegramId)
                .username(username)
                .firstName(firstName)
                .lastName(lastName)
                .displayName(firstName)
                .build();

        UserInfoResponse user = userService.registerUser(createRequest);

        userStates.put(telegramId, "awaiting_display_name");

        String response = String.format(
                "Добро пожаловать в \"Поколение сильных!\"\n\n" +
                        "Привет, %s!\n\n" +
                        "Как мне к вам обращаться?\n" +
                        "Введите ваше имя и фамилию\n" +
                        "Пример: Сергей Мордвинов",
                user.getFirstName() != null ? user.getFirstName() : "друг"
        );

        log.info("{}_START_COMMAND_SUCCESS: пользователь {} зарегистрирован/найден",
                SERVICE_NAME, telegramId);

        return response;
    }
}