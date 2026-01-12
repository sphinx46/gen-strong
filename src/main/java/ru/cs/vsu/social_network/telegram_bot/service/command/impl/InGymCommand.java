package ru.cs.vsu.social_network.telegram_bot.service.command.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.cs.vsu.social_network.telegram_bot.dto.response.UserInfoResponse;
import ru.cs.vsu.social_network.telegram_bot.service.UserService;
import ru.cs.vsu.social_network.telegram_bot.service.VisitService;
import ru.cs.vsu.social_network.telegram_bot.service.command.BaseTelegramCommand;
import ru.cs.vsu.social_network.telegram_bot.utils.MessageConstants;
import ru.cs.vsu.social_network.telegram_bot.validation.UserValidator;

/**
 * Команда для обработки "Я в зале".
 * Создает запись о посещении тренажерного зала.
 */
@Slf4j
@Component
public class InGymCommand extends BaseTelegramCommand {

    private final VisitService visitService;

    /**
     * Конструктор команды InGym.
     *
     * @param userService сервис пользователей
     * @param userValidator валидатор пользователей
     * @param visitService сервис посещений
     */
    public InGymCommand(UserService userService, UserValidator userValidator, VisitService visitService) {
        super(userService, userValidator);
        this.visitService = visitService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String execute(Long telegramId, String input) {
        checkAndInitStates();

        log.info("{}_IN_GYM_COMMAND_BEGIN: обработка команды 'Я в зале' для Telegram ID: {}",
                SERVICE_NAME, telegramId);

        try {
            visitService.createVisitByTelegramId(telegramId);
            UserInfoResponse user = getUserInfo(telegramId);

            String response = String.format(
                    "Успешно!\n\n" +
                            "%s, вы отмечены в зале!\n\n" +
                            "Журнал за сегодня будет сформирован администратором.",
                    user.getDisplayName() != null ? user.getDisplayName() : user.getFirstName()
            );

            log.info("{}_IN_GYM_COMMAND_SUCCESS: пользователь {} отмечен в зале",
                    SERVICE_NAME, telegramId);

            return response;

        } catch (Exception e) {
            log.error("{}_IN_GYM_COMMAND_ERROR: ошибка при отметке пользователя {}: {}",
                    SERVICE_NAME, telegramId, e.getMessage());

            if (e.getMessage().contains(MessageConstants.VISIT_ALREADY_FAILURE)) {
                return "Вы уже отметились сегодня в зале!\n\n" +
                        "Одна отметка в день — этого достаточно!";
            }

            return "Произошла ошибка при отметке в зале.\n\n" +
                    "Пожалуйста, попробуйте позже или обратитесь к администратору.";
        }
    }
}