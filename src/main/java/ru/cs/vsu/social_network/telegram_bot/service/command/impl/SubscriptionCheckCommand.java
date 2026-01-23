package ru.cs.vsu.social_network.telegram_bot.service.command.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.cs.vsu.social_network.telegram_bot.bot.GymTelegramBot;
import ru.cs.vsu.social_network.telegram_bot.service.UserService;
import ru.cs.vsu.social_network.telegram_bot.service.command.BaseTelegramCommand;
import ru.cs.vsu.social_network.telegram_bot.validation.UserValidator;

@Slf4j
@Component
public class SubscriptionCheckCommand extends BaseTelegramCommand {

    private static final String COMMAND_NAME = "SUBSCRIPTION_CHECK_COMMAND";
    private static final String CHANNEL_USERNAME = "@pokoleniesil";

    private final GymTelegramBot gymTelegramBot;

    public SubscriptionCheckCommand(UserService userService,
                                    UserValidator userValidator,
                                    @Lazy GymTelegramBot gymTelegramBot) {
        super(userService, userValidator);
        this.gymTelegramBot = gymTelegramBot;
    }

    @Override
    public String execute(Long telegramId, String input) {
        log.info("{}_ВЫПОЛНЕНИЕ_НАЧАЛО: проверка подписки для пользователя {}", COMMAND_NAME, telegramId);

        try {
            boolean isSubscribed = checkSubscription(telegramId);

            if (isSubscribed) {
                log.info("{}_ПОДПИСКА_ПРОВЕРЕНА: пользователь {} подписан на канал", COMMAND_NAME, telegramId);
                return "success";
            } else {
                log.warn("{}_ПОДПИСКА_ОТСУТСТВУЕТ: пользователь {} не подписан на канал", COMMAND_NAME, telegramId);
                return buildSubscriptionRequirementMessage();
            }

        } catch (TelegramApiException e) {
            log.error("{}_ОШИБКА_API_TELEGRAM: не удалось проверить подписку для пользователя {}: {}",
                    COMMAND_NAME, telegramId, e.getMessage());
            return buildErrorMessage();
        }
    }

    private boolean checkSubscription(Long telegramId) throws TelegramApiException {
        GetChatMember getChatMember = new GetChatMember();
        getChatMember.setChatId(CHANNEL_USERNAME);
        getChatMember.setUserId(telegramId);

        ChatMember chatMember = gymTelegramBot.execute(getChatMember);
        String status = chatMember.getStatus();

        return "creator".equals(status) ||
                "administrator".equals(status) ||
                "member".equals(status) ||
                "restricted".equals(status);
    }

    private String buildSubscriptionRequirementMessage() {
        return """    
               ❌ *Вы не подписаны на обязательный канал*
               
               Для использования бота необходимо подписаться на наш канал:
               📢 [Поколение Сильных](https://t.me/pokoleniesil)
               
               После подписки нажмите /start для продолжения
               """;
    }

    private String buildErrorMessage() {
        return """
               ⚠️ *ТЕХНИЧЕСКАЯ ОШИБКА*
               
               Не удалось проверить подписку на канал.
               
               Пожалуйста:
               1. Убедитесь что подписаны на @pokoleniesil
               2. Попробуйте выполнить команду /start через 5 минут
               
               📢 [ПЕРЕЙТИ В КАНАЛ](https://t.me/pokoleniesil)
               """;
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