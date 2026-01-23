package ru.cs.vsu.social_network.telegram_bot.service.command.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.cs.vsu.social_network.telegram_bot.dto.request.UserMetricsRequest;
import ru.cs.vsu.social_network.telegram_bot.dto.response.UserMetricsResponse;
import ru.cs.vsu.social_network.telegram_bot.entity.enums.FITNESS_GOAL;
import ru.cs.vsu.social_network.telegram_bot.service.UserMetricsService;
import ru.cs.vsu.social_network.telegram_bot.service.UserService;
import ru.cs.vsu.social_network.telegram_bot.service.command.BaseTelegramCommand;
import ru.cs.vsu.social_network.telegram_bot.validation.UserValidator;

@Slf4j
@Component
public class MetricsCommand extends BaseTelegramCommand {

    private static final String COMMAND_NAME = "METRICS_COMMAND";
    private static final String STATE_AWAITING_METRICS_WEIGHT = "awaiting_metrics_weight";
    private static final String STATE_AWAITING_METRICS_GOAL = "awaiting_metrics_goal";
    private static final String STATE_AWAITING_METRICS_WORKOUTS = "awaiting_metrics_workouts";
    private static final String STATE_AWAITING_METRICS_EXPERIENCE = "awaiting_metrics_experience";
    private static final String STATE_AWAITING_METRICS_AGE = "awaiting_metrics_age";
    private static final String STATE_AWAITING_METRICS_COMMENT = "awaiting_metrics_comment";
    private static final String STATE_AWAITING_METRICS_CHOICE = "awaiting_metrics_choice";

    private final UserMetricsService userMetricsService;

    public MetricsCommand(UserService userService, UserValidator userValidator,
                          UserMetricsService userMetricsService) {
        super(userService, userValidator);
        this.userMetricsService = userMetricsService;
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
        boolean metricsExist = userMetricsService.existsByTelegramId(telegramId);

        if (metricsExist) {
            UserMetricsResponse existingMetrics = userMetricsService.getMetricsByTelegramId(telegramId);
            if (existingMetrics != null) {
                setUserState(telegramId, STATE_AWAITING_METRICS_CHOICE);
                return buildExistingMetricsMessage(existingMetrics);
            }
        }

        setUserState(telegramId, STATE_AWAITING_METRICS_WEIGHT);
        log.info("{}_ИНИЦИАЛИЗАЦИЯ: начало сбора метрик для {}", COMMAND_NAME, telegramId);
        return """
                 Сбор индивидуальных метрик
                
                1. Введите ваш текущий вес (в кг, например: 75.5):""";
    }

    private String buildExistingMetricsMessage(UserMetricsResponse metrics) {
        StringBuilder sb = new StringBuilder();
        sb.append("📋 У вас уже есть сохраненные метрики:\n");
        sb.append(metrics.toString()).append("\n\n");
        sb.append("Вы хотите:\n");
        sb.append("1 - Оставить текущие метрики\n");
        sb.append("2 - Изменить метрики\n\n");
        sb.append("Введите номер выбора (1 или 2):");
        return sb.toString();
    }

    private String processMetricsInput(Long telegramId, String input) {
        String state = getUserState(telegramId);

        log.debug("{}_ОБРАБОТКА_ВВОДА: состояние '{}', input '{}' для пользователя {}",
                COMMAND_NAME, state, input, telegramId);

        if (input == null || input.trim().isEmpty()) {
            return getNextQuestion(state, true);
        }

        String trimmedInput = input.trim();

        switch (state) {
            case STATE_AWAITING_METRICS_CHOICE:
                return handleMetricsChoice(telegramId, trimmedInput);

            case STATE_AWAITING_METRICS_WEIGHT:
                Double weight = parseWeight(trimmedInput);
                if (weight == null) {
                    return "❌ Пожалуйста, введите корректный вес (например: 75.5 или 80):\n" +
                            "Текущий вес (в кг):";
                }
                UserMetricsRequest request = UserMetricsRequest.builder()
                        .telegramId(telegramId)
                        .weight(weight)
                        .build();
                userMetricsService.saveMetrics(request);
                setUserState(telegramId, STATE_AWAITING_METRICS_GOAL);
                break;

            case STATE_AWAITING_METRICS_GOAL:
                FITNESS_GOAL goal = parseGoal(trimmedInput);
                if (goal == null) {
                    return """
                            ❌ Пожалуйста, выберите одну из целей:
                            1 - Набор мышечной массы
                            2 - Похудение
                            3 - Поддержание формы
                            Ваша цель (введите номер 1-3):""";
                }
                UserMetricsRequest goalRequest = UserMetricsRequest.builder()
                        .telegramId(telegramId)
                        .goal(goal)
                        .build();
                userMetricsService.saveMetrics(goalRequest);
                setUserState(telegramId, STATE_AWAITING_METRICS_WORKOUTS);
                break;

            case STATE_AWAITING_METRICS_WORKOUTS:
                Integer workouts = parseWorkoutsCount(trimmedInput);
                if (workouts == null) {
                    return "❌ Пожалуйста, введите число тренировок от 1 до 7:\n" +
                            "Сколько тренировок в неделю планируете:";
                }
                UserMetricsRequest workoutsRequest = UserMetricsRequest.builder()
                        .telegramId(telegramId)
                        .workoutsPerWeek(workouts)
                        .build();
                userMetricsService.saveMetrics(workoutsRequest);
                setUserState(telegramId, STATE_AWAITING_METRICS_EXPERIENCE);
                break;

            case STATE_AWAITING_METRICS_EXPERIENCE:
                Double experience = parseExperience(trimmedInput);
                if (experience == null) {
                    return "❌ Пожалуйста, введите корректный тренировочный стаж (в годах, например: 2.5 или 1):\n" +
                            "Ваш тренировочный стаж (в годах):";
                }
                UserMetricsRequest experienceRequest = UserMetricsRequest.builder()
                        .telegramId(telegramId)
                        .trainingExperience(experience)
                        .build();
                userMetricsService.saveMetrics(experienceRequest);
                setUserState(telegramId, STATE_AWAITING_METRICS_AGE);
                break;

            case STATE_AWAITING_METRICS_AGE:
                Integer age = parseAge(trimmedInput);
                if (age == null) {
                    return "❌ Пожалуйста, введите корректный возраст (от 14 до 100):\n" +
                            "Ваш возраст:";
                }
                UserMetricsRequest ageRequest = UserMetricsRequest.builder()
                        .telegramId(telegramId)
                        .age(age)
                        .build();
                userMetricsService.saveMetrics(ageRequest);
                setUserState(telegramId, STATE_AWAITING_METRICS_COMMENT);
                break;

            case STATE_AWAITING_METRICS_COMMENT:
                UserMetricsRequest commentRequest = UserMetricsRequest.builder()
                        .telegramId(telegramId)
                        .comment(trimmedInput)
                        .build();
                UserMetricsResponse savedMetrics = userMetricsService.saveMetrics(commentRequest);
                resetUserState(telegramId);
                return buildSuccessMessage(savedMetrics);

            default:
                log.warn("{}_НЕИЗВЕСТНОЕ_СОСТОЯНИЕ: {} для пользователя {}",
                        COMMAND_NAME, state, telegramId);
                resetUserState(telegramId);
                return initializeMetricsCollection(telegramId);
        }

        return getNextQuestion(getUserState(telegramId), false);
    }

    private String handleMetricsChoice(Long telegramId, String input) {
        if ("1".equals(input)) {
            resetUserState(telegramId);
            UserMetricsResponse existingMetrics = userMetricsService.getMetricsByTelegramId(telegramId);
            return "✅ Текущие метрики сохранены.\n\n" + buildSuccessMessage(existingMetrics);
        } else if ("2".equals(input)) {
            setUserState(telegramId, STATE_AWAITING_METRICS_WEIGHT);
            return """
                 Сбор новых метрик
                
                1. Введите ваш текущий вес (в кг, например: 75.5):""";
        } else {
            return "❌ Пожалуйста, введите 1 (оставить текущие) или 2 (изменить):\n" +
                    "1 - Оставить текущие метрики\n" +
                    "2 - Изменить метрики";
        }
    }

    private String getNextQuestion(String nextState, boolean isError) {
        if (isError) {
            return "Пожалуйста, введите ответ на предыдущий вопрос.";
        }

        return switch (nextState) {
            case STATE_AWAITING_METRICS_GOAL -> """
                    2. Выберите вашу цель:
                    1 - Набор мышечной массы
                    2 - Похудение
                    3 - Поддержание формы
                    Введите номер (1-3):""";
            case STATE_AWAITING_METRICS_WORKOUTS -> "3. Сколько тренировок в неделю планируете?\n" +
                    "(Введите число от 1 до 7):";
            case STATE_AWAITING_METRICS_EXPERIENCE -> "4. Ваш тренировочный стаж (в годах):\n" +
                    "(Например: 1, 2.5, 0.5):";
            case STATE_AWAITING_METRICS_AGE -> "5. Ваш возраст:";
            case STATE_AWAITING_METRICS_COMMENT ->
                    "6. Комментарий (например: \"Больше внимания хотелось бы уделить отстающим группам мышц: плечи, ноги\"):\n" +
                            "(Если комментария нет - введите любой символ):";
            default -> "Пожалуйста, продолжайте ввод.";
        };
    }

    private String buildSuccessMessage(UserMetricsResponse metrics) {
        StringBuilder sb = new StringBuilder();
        sb.append("📋 Ваши метрики:\n");
        sb.append(metrics.toString());
        sb.append("\n\nЭти данные будут использованы для персонализации рекомендаций.");

        return sb.toString();
    }

    private void resetUserState(Long telegramId) {
        userStates.remove(telegramId);
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
}