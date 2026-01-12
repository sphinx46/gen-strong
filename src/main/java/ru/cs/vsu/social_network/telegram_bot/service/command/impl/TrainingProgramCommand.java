package ru.cs.vsu.social_network.telegram_bot.service.command.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.cs.vsu.social_network.telegram_bot.dto.response.UserInfoResponse;
import ru.cs.vsu.social_network.telegram_bot.service.UserService;
import ru.cs.vsu.social_network.telegram_bot.service.command.BaseTelegramCommand;
import ru.cs.vsu.social_network.telegram_bot.validation.UserValidator;

/**
 * Команда для обработки создания программы тренировок.
 * Запрашивает максимальный жим лежа для расчета программы.
 */
@Slf4j
@Component
public class TrainingProgramCommand extends BaseTelegramCommand {

    /**
     * Конструктор команды TrainingProgram.
     *
     * @param userService сервис пользователей
     * @param userValidator валидатор пользователей
     */
    public TrainingProgramCommand(UserService userService, UserValidator userValidator) {
        super(userService, userValidator);
    }

    /** {@inheritDoc} */
    @Override
    public String execute(Long telegramId, String input) {
        log.info("{}_TRAINING_PROGRAM_COMMAND_BEGIN: пользователь {} запрашивает программу",
                SERVICE_NAME, telegramId);

        try {
            checkAndInitStates();
            UserInfoResponse user = getUserInfo(telegramId);
            setUserState(telegramId, "awaiting_training_cycle");

            StringBuilder response = new StringBuilder();
            response.append(String.format(
                    "%s, составим индивидуальную программу тренировок!\n\n",
                    user.getDisplayName() != null ? user.getDisplayName() : user.getFirstName()));

            response.append("Выберите тренировочный цикл:\n\n");
            response.append("1. Гусеница - базовый цикл для развития силовых показателей\n");
            response.append("2. Жим номер один - специализированный цикл для жима лежа\n");
            response.append("3. СТО-2ж - цикл для подготовки к силовым нормативам\n\n");
            response.append("Все циклы разработаны заслуженным тренером России А.Е. Суровецким\n\n");
            response.append("Введите номер цикла (1, 2, 3):");

            log.info("{}_TRAINING_PROGRAM_COMMAND_SUCCESS: пользователь {} ожидает выбора цикла",
                    SERVICE_NAME, telegramId);

            return response.toString();

        } catch (IllegalStateException e) {
            log.error("{}_TRAINING_PROGRAM_COMMAND_STATE_ERROR: состояния не инициализированы для пользователя {}",
                    SERVICE_NAME, telegramId);
            return "Произошла внутренняя ошибка при инициализации состояний.\n\n" +
                    "Пожалуйста, попробуйте позже или обратитесь к администратору.";
        } catch (Exception e) {
            log.error("{}_TRAINING_PROGRAM_COMMAND_ERROR: ошибка для {}: {}",
                    SERVICE_NAME, telegramId, e.getMessage(), e);
            return "Произошла ошибка при запросе программы тренировок.\n\n" +
                    "Пожалуйста, попробуйте позже.";
        }
    }
}