package ru.cs.vsu.social_network.telegram_bot.service.command.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.cs.vsu.social_network.telegram_bot.dto.request.UserCreateRequest;
import ru.cs.vsu.social_network.telegram_bot.dto.response.UserInfoResponse;
import ru.cs.vsu.social_network.telegram_bot.service.UserService;
import ru.cs.vsu.social_network.telegram_bot.service.command.BaseTelegramCommand;
import ru.cs.vsu.social_network.telegram_bot.validation.UserValidator;

@Slf4j
@Component
public class StartCommand extends BaseTelegramCommand {

    private static final String COMMAND_NAME = "START_COMMAND";
    private final SubscriptionCheckCommand subscriptionCheckCommand;

    public StartCommand(UserService userService,
                        UserValidator userValidator,
                        @Lazy SubscriptionCheckCommand subscriptionCheckCommand) {
        super(userService, userValidator);
        this.subscriptionCheckCommand = subscriptionCheckCommand;
    }

    @Override
    public String execute(Long telegramId, String input) {
        log.info("{}_ВЫПОЛНЕНИЕ_НАЧАЛО: обработка /start для пользователя {}", COMMAND_NAME, telegramId);

        String subscriptionResult = subscriptionCheckCommand.execute(telegramId, null);

        if (!"success".equals(subscriptionResult)) {
            log.warn("{}_ДОСТУП_ОТКЛОНЕН: пользователь {} не прошел проверку подписки", COMMAND_NAME, telegramId);
            return subscriptionResult;
        }

        log.info("{}_ПРОВЕРКА_ПРОЙДЕНА: пользователь {} успешно прошел проверку подписки", COMMAND_NAME, telegramId);

        checkAndInitStates();

        log.info("{}_СТАРТ_КОМАНДА: обработка /start для Telegram ID: {}",
                COMMAND_NAME, telegramId);

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
                "✅ *Добро пожаловать в \"Поколение сильных!\"*\n\n" +
                        "Привет, %s!\n\n" +
                        "Как мне к вам обращаться?\n" +
                        "Введите ваше имя и фамилию\n" +
                        "Пример: Сергей Мордвинов",
                user.getFirstName() != null ? user.getFirstName() : "друг"
        );

        log.info("{}_ВЫПОЛНЕНИЕ_УСПЕХ: пользователь {} зарегистрирован", COMMAND_NAME, telegramId);

        return response;
    }

    @Override
    public void setUserStates(java.util.Map<Long, String> userStates) {
        super.setUserStates(userStates);
    }

    @Override
    public void setAdminStates(java.util.Map<Long, String> adminStates) {
        super.setAdminStates(adminStates);
    }

    @Override
    public void setPendingBenchPressValues(java.util.Map<Long, Double> pendingBenchPressValues) {
        super.setPendingBenchPressValues(pendingBenchPressValues);
    }

    @Override
    public void setPendingTrainingCycles(java.util.Map<Long, String> pendingTrainingCycles) {
        super.setPendingTrainingCycles(pendingTrainingCycles);
    }

    @Override
    public void setPendingFormatSelections(java.util.Map<Long, String> pendingFormatSelections) {
        super.setPendingFormatSelections(pendingFormatSelections);
    }
}