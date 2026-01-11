package ru.cs.vsu.social_network.telegram_bot.service.command.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.cs.vsu.social_network.telegram_bot.dto.response.UserInfoResponse;
import ru.cs.vsu.social_network.telegram_bot.provider.UserTrainingEntityProvider;
import ru.cs.vsu.social_network.telegram_bot.service.UserService;
import ru.cs.vsu.social_network.telegram_bot.service.command.BaseTelegramCommand;
import ru.cs.vsu.social_network.telegram_bot.validation.UserValidator;

import java.util.Optional;

/**
 * Команда для обработки создания программы тренировок.
 * Запрашивает максимальный жим лежа для расчета программы.
 */
@Slf4j
@Component
public class TrainingProgramCommand extends BaseTelegramCommand {

    private final UserTrainingEntityProvider userTrainingEntityProvider;

    /**
     * Конструктор команды TrainingProgram.
     *
     * @param userService сервис пользователей
     * @param userValidator валидатор пользователей
     * @param userTrainingEntityProvider провайдер данных тренировок
     */
    public TrainingProgramCommand(UserService userService, UserValidator userValidator,
                                  UserTrainingEntityProvider userTrainingEntityProvider) {
        super(userService, userValidator);
        this.userTrainingEntityProvider = userTrainingEntityProvider;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String execute(Long telegramId, String input) {
        log.info("{}_TRAINING_PROGRAM_COMMAND_BEGIN: пользователь {} запрашивает программу",
                SERVICE_NAME, telegramId);

        try {
            UserInfoResponse user = getUserInfo(telegramId);
            userStates.put(telegramId, "awaiting_bench_press");

            Optional<Double> existingBenchPress = userTrainingEntityProvider.getMaxBenchPressByTelegramId(telegramId);

            StringBuilder response = new StringBuilder();
            response.append(String.format(
                    "%s, составим индивидуальную программу тренировок!\n\n",
                    user.getDisplayName() != null ? user.getDisplayName() : user.getFirstName()));

            response.append("Для расчета рабочих весов мне нужно знать ваш максимальный жим лежа.\n\n");

            if (existingBenchPress.isPresent()) {
                response.append(String.format("Текущее значение: %.1f кг\n\n", existingBenchPress.get()));
                response.append("Введите новое значение или старое для перегенерации программы:\n");
            } else {
                response.append("Какой ваш максимальный жим лежа?\n");
            }

            response.append("Пример: 102,5 или 105\n");
            response.append("Введите число в килограммах (можно с десятичной точкой):");

            log.info("{}_TRAINING_PROGRAM_COMMAND_SUCCESS: пользователь {} ожидает ввода жима лежа",
                    SERVICE_NAME, telegramId);

            return response.toString();

        } catch (Exception e) {
            log.error("{}_TRAINING_PROGRAM_COMMAND_ERROR: ошибка для {}: {}",
                    SERVICE_NAME, telegramId, e.getMessage());

            return "Произошла ошибка при запросе программы тренировок.\n\n" +
                    "Пожалуйста, попробуйте позже.";
        }
    }
}