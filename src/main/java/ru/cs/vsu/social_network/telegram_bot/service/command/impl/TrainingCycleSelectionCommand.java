package ru.cs.vsu.social_network.telegram_bot.service.command.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.cs.vsu.social_network.telegram_bot.dto.response.TrainingCycleInfo;
import ru.cs.vsu.social_network.telegram_bot.service.ExcelTrainingService;
import ru.cs.vsu.social_network.telegram_bot.service.UserService;
import ru.cs.vsu.social_network.telegram_bot.service.command.BaseTelegramCommand;
import ru.cs.vsu.social_network.telegram_bot.utils.MessageConstants;
import ru.cs.vsu.social_network.telegram_bot.validation.UserValidator;

/**
 * Команда для обработки выбора тренировочного цикла
 */
@Slf4j
@Component
public class TrainingCycleSelectionCommand extends BaseTelegramCommand {

    private final ExcelTrainingService excelTrainingService;

    /**
     * Конструктор команды выбора тренировочного цикла
     *
     * @param userService сервис пользователей
     * @param userValidator валидатор пользователей
     * @param excelTrainingService сервис тренировочных планов
     */
    public TrainingCycleSelectionCommand(UserService userService,
                                         UserValidator userValidator,
                                         ExcelTrainingService excelTrainingService) {
        super(userService, userValidator);
        this.excelTrainingService = excelTrainingService;
    }

    /** {@inheritDoc} */
    @Override
    public String execute(Long telegramId, String input) {
        log.info("{}_TRAINING_CYCLE_SELECTION_BEGIN: пользователь {}, выбор '{}'",
                SERVICE_NAME, telegramId, input);

        try {
            checkAndInitStates();

            String userState = getUserState(telegramId);

            if (!"awaiting_training_cycle".equals(userState)) {
                log.warn("{}_TRAINING_CYCLE_UNEXPECTED: пользователь {} не ожидает выбора цикла. Текущий статус: {}",
                        SERVICE_NAME, telegramId, userState);
                return "Неожиданный запрос. Пожалуйста, используйте команды из меню.";
            }

            java.util.List<TrainingCycleInfo> cycles = excelTrainingService.getAvailableTrainingCycles();
            int selection;

            try {
                selection = Integer.parseInt(input.trim());
                if (selection < 1 || selection > cycles.size()) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException e) {
                log.warn("{}_TRAINING_CYCLE_INVALID_INPUT: пользователь {}, ввод '{}'",
                        SERVICE_NAME, telegramId, input);
                return MessageConstants.TRAINING_CYCLE_INVALID_INPUT +
                        "\n\nПожалуйста, введите число от 1 до " + cycles.size() + ":";
            }

            TrainingCycleInfo selectedCycle = cycles.get(selection - 1);
            pendingTrainingCycles.put(telegramId, selectedCycle.getId());
            setUserState(telegramId, "awaiting_bench_press");
            StringBuilder response = new StringBuilder();
            response.append(String.format("Выбран цикл: %s\n\n", selectedCycle.getDisplayName()));
            response.append("Для расчета рабочих весов мне нужно знать ваш максимальный жим лежа.\n\n");
            response.append("Какой ваш максимальный жим лежа?\n");
            response.append("Пример: 102,5 или 105\n");
            response.append("Введите число в килограммах (можно с десятичной точкой):");

            log.info("{}_TRAINING_CYCLE_SELECTION_SUCCESS: пользователь {} выбрал цикл '{}'",
                    SERVICE_NAME, telegramId, selectedCycle.getId());

            return response.toString();

        } catch (Exception e) {
            log.error("{}_TRAINING_CYCLE_SELECTION_ERROR: ошибка для {}: {}",
                    SERVICE_NAME, telegramId, e.getMessage(), e);
            return MessageConstants.TRAINING_CYCLE_SELECTION_FAILURE +
                    "\n\nПожалуйста, попробуйте снова.";
        }
    }
}