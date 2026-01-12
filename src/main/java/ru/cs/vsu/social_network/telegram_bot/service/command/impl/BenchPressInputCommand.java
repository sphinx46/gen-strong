package ru.cs.vsu.social_network.telegram_bot.service.command.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.cs.vsu.social_network.telegram_bot.service.UserService;
import ru.cs.vsu.social_network.telegram_bot.service.command.BaseTelegramCommand;
import ru.cs.vsu.social_network.telegram_bot.validation.UserValidator;

import java.util.regex.Pattern;

/**
 * Команда для обработки ввода максимального жима лежа.
 * Валидирует ввод и переводит пользователя к выбору формата программы.
 */
@Slf4j
@Component
public class BenchPressInputCommand extends BaseTelegramCommand {

    private static final Pattern BENCH_PRESS_PATTERN = Pattern.compile("^\\d+(?:\\.\\d{1,2})?$");

    /**
     * Конструктор команды BenchPressInput.
     *
     * @param userService сервис пользователей
     * @param userValidator валидатор пользователей
     */
    public BenchPressInputCommand(UserService userService, UserValidator userValidator) {
        super(userService, userValidator);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String execute(Long telegramId, String input) {
        checkAndInitStates();

        log.info("{}_BENCH_PRESS_INPUT_BEGIN: обработка ввода '{}' для Telegram ID: {}",
                SERVICE_NAME, input, telegramId);

        String userState = userStates.get(telegramId);

        if (!"awaiting_bench_press".equals(userState)) {
            log.warn("{}_BENCH_PRESS_INPUT_UNEXPECTED: Telegram ID {} не ожидает ввода жима лежа",
                    SERVICE_NAME, telegramId);
            return "Неожиданный запрос. Пожалуйста, используйте команды из меню.";
        }

        try {
            String benchPressInput = input.trim().replace(',', '.');

            if (!BENCH_PRESS_PATTERN.matcher(benchPressInput).matches()) {
                log.warn("{}_BENCH_PRESS_INPUT_INVALID_FORMAT: некорректный формат: {}",
                        SERVICE_NAME, benchPressInput);
                return "Неверный формат!\n\n" +
                        "Пожалуйста, введите число.\n" +
                        "Пример: 102,5 или 105\n" +
                        "Можно использовать десятичную точку или запятую.";
            }

            double maxBenchPress;
            try {
                maxBenchPress = Double.parseDouble(benchPressInput);
            } catch (NumberFormatException e) {
                log.warn("{}_BENCH_PRESS_INPUT_INVALID_NUMBER_FORMAT: не удалось преобразовать: {}",
                        SERVICE_NAME, benchPressInput);
                return "Неверный формат числа!\n\n" +
                        "Пожалуйста, введите число.\n" +
                        "Пример: 102,5 или 105";
            }

            log.info("{}_BENCH_PRESS_INPUT_PROCESSING: пользователь {}, жим лежа: {} кг",
                    SERVICE_NAME, telegramId, maxBenchPress);

            pendingBenchPressValues.put(telegramId, maxBenchPress);
            userStates.put(telegramId, "awaiting_format_selection");

            return "Спасибо!\n\n" +
                    "Максимальный жим лежа: " + maxBenchPress + " кг\n\n" +
                    "В каком формате предоставить программу тренировок?\n\n" +
                    "1. Изображение (рекомендуется для удобного просмотра в Telegram)\n" +
                    "2. Excel таблица (для открытия на компьютере)\n\n" +
                    "Введите '1' или '2'";

        } catch (Exception e) {
            log.error("{}_BENCH_PRESS_INPUT_ERROR: ошибка при обработке ввода для {}: {}",
                    SERVICE_NAME, telegramId, e.getMessage());

            return "Произошла ошибка при обработке вашего ввода.\n\n" +
                    "Пожалуйста, попробуйте еще раз.";
        }
    }
}