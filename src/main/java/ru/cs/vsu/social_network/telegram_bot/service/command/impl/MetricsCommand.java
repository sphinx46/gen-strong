package ru.cs.vsu.social_network.telegram_bot.service.command.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.cs.vsu.social_network.telegram_bot.dto.response.UserMetricsResponse;
import ru.cs.vsu.social_network.telegram_bot.entity.enums.FITNESS_GOAL;
import ru.cs.vsu.social_network.telegram_bot.service.UserService;
import ru.cs.vsu.social_network.telegram_bot.service.command.BaseTelegramCommand;
import ru.cs.vsu.social_network.telegram_bot.validation.UserValidator;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class MetricsCommand extends BaseTelegramCommand {

    private static final String COMMAND_NAME = "METRICS_COMMAND";
    private static final Map<Long, UserMetricsResponse> userMetricsCache = new ConcurrentHashMap<>();

    public MetricsCommand(UserService userService, UserValidator userValidator) {
        super(userService, userValidator);
    }

    @Override
    public String execute(Long telegramId, String input) {
        log.info("{}_ВЫПОЛНЕНИЕ_НАЧАЛО: пользователь {}, input: '{}'", COMMAND_NAME, telegramId, input);

        try {
            checkAndInitStates();
            String currentState = getUserState(telegramId);

            if (currentState == null || !currentState.startsWith("awaiting_metrics")) {
                return initializeMetricsCollection(telegramId);
            }

            return processMetricsInput(telegramId, input);

        } catch (Exception e) {
            log.error("{}_ОШИБКА_ВЫПОЛНЕНИЯ: для пользователя {}: {}",
                    COMMAND_NAME, telegramId, e.getMessage(), e);
            resetUserState(telegramId);
            return "Произошла ошибка при сборе метрик. Пожалуйста, начните заново.";
        }
    }

    private String initializeMetricsCollection(Long telegramId) {
        UserMetricsResponse initialMetrics = UserMetricsResponse.builder()
                .telegramId(telegramId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        userMetricsCache.put(telegramId, initialMetrics);
        setUserState(telegramId, "awaiting_metrics_weight");

        log.info("{}_ИНИЦИАЛИЗАЦИЯ: начало сбора метрик для {}", COMMAND_NAME, telegramId);

        return """
                 Сбор индивидуальных метрик
                
                1. Введите ваш текущий вес (в кг, например: 75.5):""";
    }

    private String processMetricsInput(Long telegramId, String input) {
        String state = getUserState(telegramId);
        UserMetricsResponse metrics = userMetricsCache.get(telegramId);

        log.debug("{}_ОБРАБОТКА_ВВОДА: состояние '{}', input '{}' для пользователя {}",
                COMMAND_NAME, state, input, telegramId);

        if (input == null || input.trim().isEmpty()) {
            return getNextQuestion(state, true);
        }

        String trimmedInput = input.trim();

        switch (state) {
            case "awaiting_metrics_weight":
                Double weight = parseWeight(trimmedInput);
                if (weight == null) {
                    return "❌ Пожалуйста, введите корректный вес (например: 75.5 или 80):\n" +
                            "Текущий вес (в кг):";
                }
                metrics.setWeight(weight);
                setUserState(telegramId, "awaiting_metrics_goal");
                break;

            case "awaiting_metrics_goal":
                FITNESS_GOAL goal = parseGoal(trimmedInput);
                if (goal == null) {
                    return """
                            ❌ Пожалуйста, выберите одну из целей:
                            1 - Набор мышечной массы
                            2 - Похудение
                            3 - Поддержание формы
                            Ваша цель (введите номер 1-3):""";
                }
                metrics.setGoal(goal);
                metrics.setGoalRussianName(goal.getRussianName());
                setUserState(telegramId, "awaiting_metrics_workouts");
                break;

            case "awaiting_metrics_workouts":
                Integer workouts = parseWorkoutsCount(trimmedInput);
                if (workouts == null) {
                    return "❌ Пожалуйста, введите число тренировок от 1 до 7:\n" +
                            "Сколько тренировок в неделю планируете:";
                }
                metrics.setWorkoutsPerWeek(workouts);
                setUserState(telegramId, "awaiting_metrics_experience");
                break;

            case "awaiting_metrics_experience":
                Double experience = parseExperience(trimmedInput);
                if (experience == null) {
                    return "❌ Пожалуйста, введите корректный тренировочный стаж (в годах, например: 2.5 или 1):\n" +
                            "Ваш тренировочный стаж (в годах):";
                }
                metrics.setTrainingExperience(experience);
                setUserState(telegramId, "awaiting_metrics_age");
                break;

            case "awaiting_metrics_age":
                Integer age = parseAge(trimmedInput);
                if (age == null) {
                    return "❌ Пожалуйста, введите корректный возраст (от 14 до 100):\n" +
                            "Ваш возраст:";
                }
                metrics.setAge(age);
                setUserState(telegramId, "awaiting_metrics_comment");
                break;

            case "awaiting_metrics_comment":
                metrics.setComment(trimmedInput);
                metrics.setUpdatedAt(LocalDateTime.now());

                UserMetricsResponse finalMetrics = finalizeMetricsCollection(telegramId);
                return buildSuccessMessage(finalMetrics);

            default:
                log.warn("{}_НЕИЗВЕСТНОЕ_СОСТОЯНИЕ: {} для пользователя {}",
                        COMMAND_NAME, state, telegramId);
                resetUserState(telegramId);
                return initializeMetricsCollection(telegramId);
        }

        return getNextQuestion(getUserState(telegramId), false);
    }

    private String getNextQuestion(String nextState, boolean isError) {
        if (isError) {
            return "Пожалуйста, введите ответ на предыдущий вопрос.";
        }

        return switch (nextState) {
            case "awaiting_metrics_goal" -> """
                    2. Выберите вашу цель:
                    1 - Набор мышечной массы
                    2 - Похудение
                    3 - Поддержание формы
                    Введите номер (1-3):""";
            case "awaiting_metrics_workouts" -> "3. Сколько тренировок в неделю планируете?\n" +
                    "(Введите число от 1 до 7):";
            case "awaiting_metrics_experience" -> "4. Ваш тренировочный стаж (в годах):\n" +
                    "(Например: 1, 2.5, 0.5):";
            case "awaiting_metrics_age" -> "5. Ваш возраст:";
            case "awaiting_metrics_comment" ->
                    "6. Комментарий (например: \"Больше внимания хотелось бы уделить отстающим группам мышц: плечи, ноги\"):\n" +
                            "(Если комментария нет - введите любой символ):";
            default -> "Пожалуйста, продолжайте ввод.";
        };
    }

    private UserMetricsResponse finalizeMetricsCollection(Long telegramId) {
        try {
            UserMetricsResponse metrics = userMetricsCache.get(telegramId);


            log.info("{}_МЕТРИКИ_СОБРАНЫ: для пользователя {}, данные: {}",
                    COMMAND_NAME, telegramId, metrics);

            resetUserState(telegramId);

            return metrics;

        } catch (Exception e) {
            log.error("{}_ОШИБКА_СБОРА: для пользователя {}: {}",
                    COMMAND_NAME, telegramId, e.getMessage(), e);
            throw new RuntimeException("Ошибка при сборе метрик", e);
        }
    }

    private String buildSuccessMessage(UserMetricsResponse metrics) {
        StringBuilder sb = new StringBuilder();
        sb.append("✅ Метрики успешно собраны!\n\n");
        sb.append("📋 Ваши метрики:\n");
        sb.append("• Вес: ").append(metrics.getWeight()).append(" кг\n");
        sb.append("• Цель: ").append(metrics.getGoalRussianName()).append("\n");
        sb.append("• Тренировок в неделю: ").append(metrics.getWorkoutsPerWeek()).append("\n");
        sb.append("• Стаж: ").append(metrics.getTrainingExperience()).append(" лет\n");
        sb.append("• Возраст: ").append(metrics.getAge()).append(" лет\n");

        String comment = metrics.getComment();
        if (comment != null && comment.length() > 1 && !comment.matches("^[\\s\\S]{1,2}$")) {
            sb.append("• Комментарий: ").append(comment);
        }

        sb.append("\n\nЭти данные будут использованы для персонализации рекомендаций.");

        return sb.toString();
    }

    private void resetUserState(Long telegramId) {
        userStates.remove(telegramId);
        userMetricsCache.remove(telegramId);
        log.info("{}_СБРОС_СОСТОЯНИЯ: для пользователя {}", COMMAND_NAME, telegramId);
    }

    private Double parseWeight(String input) {
        try {
            double weight = Double.parseDouble(input.replace(',', '.'));
            return weight > 20 && weight < 300 ? weight : null;
        } catch (NumberFormatException e) {
            log.warn("{}_ОШИБКА_ПАРСИНГА_ВЕСА: некорректный ввод '{}'", COMMAND_NAME, input);
            return null;
        }
    }

    private FITNESS_GOAL parseGoal(String input) {
        try {
            return switch (input.trim()) {
                case "1" -> FITNESS_GOAL.MUSCLE_GAIN;
                case "2" -> FITNESS_GOAL.WEIGHT_LOSS;
                case "3" -> FITNESS_GOAL.MAINTENANCE;
                default -> FITNESS_GOAL.valueOf(input.toUpperCase());
            };
        } catch (IllegalArgumentException e) {
            log.warn("{}_ОШИБКА_ПАРСИНГА_ЦЕЛИ: некорректный ввод '{}'", COMMAND_NAME, input);
            return null;
        }
    }

    private Integer parseWorkoutsCount(String input) {
        try {
            int count = Integer.parseInt(input);
            return count >= 1 && count <= 7 ? count : null;
        } catch (NumberFormatException e) {
            log.warn("{}_ОШИБКА_ПАРСИНГА_ТРЕНИРОВОК: некорректный ввод '{}'", COMMAND_NAME, input);
            return null;
        }
    }

    private Double parseExperience(String input) {
        try {
            double experience = Double.parseDouble(input.replace(',', '.'));
            return experience >= 0 && experience <= 100 ? experience : null;
        } catch (NumberFormatException e) {
            log.warn("{}_ОШИБКА_ПАРСИНГА_СТАЖА: некорректный ввод '{}'", COMMAND_NAME, input);
            return null;
        }
    }

    private Integer parseAge(String input) {
        try {
            int age = Integer.parseInt(input);
            return age >= 14 && age <= 100 ? age : null;
        } catch (NumberFormatException e) {
            log.warn("{}_ОШИБКА_ПАРСИНГА_ВОЗРАСТА: некорректный ввод '{}'", COMMAND_NAME, input);
            return null;
        }
    }

    /**
     * Получить собранные метрики для пользователя
     *
     * @param telegramId идентификатор пользователя
     * @return собранные метрики или null если метрики не собраны
     */
    public UserMetricsResponse getCollectedMetrics(Long telegramId) {
        return userMetricsCache.get(telegramId);
    }
}