package ru.cs.vsu.social_network.telegram_bot.service.command.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.cs.vsu.social_network.telegram_bot.service.UserService;
import ru.cs.vsu.social_network.telegram_bot.service.command.BaseTelegramCommand;
import ru.cs.vsu.social_network.telegram_bot.validation.UserValidator;

@Slf4j
@Component
public class ContributionCommand extends BaseTelegramCommand {

    public ContributionCommand(UserService userService, UserValidator userValidator) {
        super(userService, userValidator);
    }

    @Override
    public String execute(Long telegramId, String input) {
        checkAndInitStates();

        log.info("{}_CONTRIBUTION_COMMAND_BEGIN: обработка команды 'Внести вклад в развитие' для Telegram ID: {}",
                SERVICE_NAME, telegramId);

        String response = "Наш клуб и бот живут только благодаря энтузиастам. " +
                "Мы не берем денег за доступ, потому что верим в идею. " +
                "Но все требует времени и ресурсов. " +
                "Если автоматические расчеты избавили вас от головной боли с Excel " +
                "— вы можете отблагодарить проект.\n\n" +
                "Ваша поддержка поможет:\n" +
                "• Добавить в бота циклы.\n" +
                "• Починить/купить новый спортинвентарь.\n" +
                "• Обеспечить стабильную работу сервера для бота.\n\n" +
                "Любая сумма важна! Спасибо, что вы с нами!\n" +
                "❤ Поддержать проект: 4276 3300 1994 1715";

        log.info("{}_CONTRIBUTION_COMMAND_SUCCESS: информация о поддержке отправлена пользователю {}",
                SERVICE_NAME, telegramId);

        return response;
    }
}