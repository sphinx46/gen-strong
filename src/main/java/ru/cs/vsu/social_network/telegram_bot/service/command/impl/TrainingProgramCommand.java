package ru.cs.vsu.social_network.telegram_bot.service.command.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.cs.vsu.social_network.telegram_bot.dto.request.UserBenchPressRequest;
import ru.cs.vsu.social_network.telegram_bot.dto.response.TrainingCycleInfo;
import ru.cs.vsu.social_network.telegram_bot.dto.response.UserInfoResponse;
import ru.cs.vsu.social_network.telegram_bot.service.DocumentSenderService;
import ru.cs.vsu.social_network.telegram_bot.service.ExcelTrainingService;
import ru.cs.vsu.social_network.telegram_bot.service.UserService;
import ru.cs.vsu.social_network.telegram_bot.service.command.BaseTelegramCommand;
import ru.cs.vsu.social_network.telegram_bot.validation.UserValidator;

import java.io.File;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class TrainingProgramCommand extends BaseTelegramCommand {

    private static final String STATE_AWAITING_CYCLE_SELECTION = "awaiting_cycle_selection";
    private static final String STATE_AWAITING_BENCH_PRESS = "awaiting_bench_press";

    private final ExcelTrainingService excelTrainingService;
    private final DocumentSenderService documentSenderService;
    private final List<TrainingCycleInfo> availableCycles;

    public TrainingProgramCommand(UserService userService,
                                  UserValidator userValidator,
                                  ExcelTrainingService excelTrainingService,
                                  DocumentSenderService documentSenderService) {
        super(userService, userValidator);
        this.excelTrainingService = excelTrainingService;
        this.documentSenderService = documentSenderService;
        this.availableCycles = excelTrainingService.getAvailableTrainingCycles();
    }

    @Override
    public String execute(Long telegramId, String input) {
        log.info("{}_TRAINING_PROGRAM_BEGIN: пользователь {} запрашивает программу, ввод: {}",
                SERVICE_NAME, telegramId, input);

        try {
            checkAndInitStates();
            String userState = userStates.get(telegramId);

            if (userState == null) {
                return handleNewTrainingRequest(telegramId);
            } else if (STATE_AWAITING_CYCLE_SELECTION.equals(userState)) {
                return handleCycleSelection(telegramId, input);
            } else if (STATE_AWAITING_BENCH_PRESS.equals(userState)) {
                return handleBenchPressInput(telegramId, input);
            } else {
                return handleUnexpectedState(telegramId, userState);
            }

        } catch (Exception e) {
            return handleTrainingProgramError(telegramId, e);
        }
    }

    private String handleNewTrainingRequest(Long telegramId) {
        log.info("{}_TRAINING_PROGRAM_NEW_REQUEST: новый запрос программы от пользователя {}",
                SERVICE_NAME, telegramId);

        UserInfoResponse user = getUserInfo(telegramId);
        setUserState(telegramId, STATE_AWAITING_CYCLE_SELECTION);

        return buildCycleSelectionMessage(user);
    }

    private String handleCycleSelection(Long telegramId, String input) {
        log.info("{}_TRAINING_PROGRAM_CYCLE_SELECTION: пользователь {}, выбор '{}'",
                SERVICE_NAME, telegramId, input);

        try {
            int selection = Integer.parseInt(input.trim());

            if (isInvalidCycleSelection(selection)) {
                return buildInvalidCycleSelectionMessage();
            }

            TrainingCycleInfo selectedCycle = getSelectedCycle(selection);
            if (selectedCycle == null) {
                return buildInvalidCycleSelectionMessage();
            }

            pendingTrainingCycles.put(telegramId, selectedCycle.getId());
            setUserState(telegramId, STATE_AWAITING_BENCH_PRESS);

            return buildBenchPressRequestMessage(selectedCycle);

        } catch (NumberFormatException e) {
            log.warn("{}_TRAINING_PROGRAM_INVALID_CYCLE_INPUT: пользователь {}, ввод '{}'",
                    SERVICE_NAME, telegramId, input);
            return buildInvalidCycleSelectionMessage();
        }
    }

    private String handleBenchPressInput(Long telegramId, String input) {
        log.info("{}_TRAINING_PROGRAM_BENCH_PRESS_INPUT: пользователь {}, ввод '{}'",
                SERVICE_NAME, telegramId, input);

        try {
            double maxBenchPress = parseBenchPressInput(input);

            String cycleId = pendingTrainingCycles.get(telegramId);
            if (cycleId == null) {
                throw new RuntimeException("Не найден выбранный цикл");
            }

            TrainingCycleInfo cycleInfo = excelTrainingService.getTrainingCycleInfo(cycleId);
            File excelFile = generateTrainingPlan(telegramId, maxBenchPress, cycleId);
            sendExcelFile(telegramId, excelFile, cycleInfo, maxBenchPress);
            cleanupUserState(telegramId);

            return buildSuccessMessage(cycleInfo, maxBenchPress);

        } catch (NumberFormatException e) {
            return buildInvalidBenchPressFormatMessage();
        } catch (Exception e) {
            return handleExcelGenerationError(telegramId, e);
        }
    }

    private String handleUnexpectedState(Long telegramId, String userState) {
        log.warn("{}_TRAINING_PROGRAM_UNEXPECTED_STATE: неожиданное состояние {} для пользователя {}",
                SERVICE_NAME, userState, telegramId);
        cleanupUserState(telegramId);
        return "Неожиданный запрос. Пожалуйста, используйте команды из меню.";
    }

    private String handleTrainingProgramError(Long telegramId, Exception e) {
        log.error("{}_TRAINING_PROGRAM_ERROR: ошибка для {}: {}",
                SERVICE_NAME, telegramId, e.getMessage(), e);
        cleanupUserState(telegramId);
        return "Произошла ошибка при создании программы тренировок.\n\nПожалуйста, попробуйте позже.";
    }

    private String handleExcelGenerationError(Long telegramId, Exception e) {
        log.error("{}_TRAINING_PROGRAM_GENERATION_ERROR: ошибка генерации Excel для {}: {}",
                SERVICE_NAME, telegramId, e.getMessage(), e);
        cleanupUserState(telegramId);
        return "❌ Произошла ошибка при генерации программы тренировок.\n\nПожалуйста, попробуйте позже или обратитесь к администратору.";
    }

    private String buildCycleSelectionMessage(UserInfoResponse user) {
        StringBuilder response = new StringBuilder();
        response.append(String.format("%s, составим индивидуальную программу тренировок в Excel!\n\n",
                user.getDisplayName() != null ? user.getDisplayName() : user.getFirstName()));
        response.append("Выберите тренировочный цикл:\n\n");

        for (int i = 0; i < availableCycles.size(); i++) {
            TrainingCycleInfo cycle = availableCycles.get(i);
            response.append(String.format("%d. %s -\n", i + 1, cycle.getDisplayName()));
        }

        response.append("\nВсе циклы разработаны заслуженным тренером России А.Е. Суровецким\n\n");
        response.append("Введите номер цикла (");
        response.append(buildCycleNumberRange());
        response.append("):");

        return response.toString();
    }

    private String buildInvalidCycleSelectionMessage() {
        return "Неверный формат!\n\nПожалуйста, введите число от 1 до " + availableCycles.size() +
                ":\n\n" + buildCycleList();
    }

    private String buildBenchPressRequestMessage(TrainingCycleInfo selectedCycle) {
        return "✅ Выбран цикл: " + selectedCycle.getDisplayName() + "\n\n" +
                "Для расчета программы тренировок в Excel мне нужно знать ваш максимальный жим лежа.\n\n" +
                "Какой ваш максимальный жим лежа?\n" +
                "Пример: 102,5 или 105\n" +
                "Введите число в килограммах:";
    }

    private String buildInvalidBenchPressFormatMessage() {
        return """
                Неверный формат числа!
                
                Пожалуйста, введите число.
                (Пример: 102,5 или 105)
                Можно использовать десятичную точку или запятую.
                
                Введите ваш максимальный жим лежа в килограммах:""";
    }

    private String buildSuccessMessage(TrainingCycleInfo cycleInfo, double maxBenchPress) {
        return "✅ Программа тренировок успешно сгенерирована в формате Excel!\n\n" +
                "Файл отправлен вам в чат.\n\n📊 Детали программы:\n" +
                "• Цикл: " + cycleInfo.getDisplayName() + "\n" +
                "• Максимальный жим лежа: " + maxBenchPress + " кг\n\n" +
                "Приятных тренировок!";
    }

    private String buildCycleList() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < availableCycles.size(); i++) {
            builder.append(i + 1).append(". ").append(availableCycles.get(i).getDisplayName()).append("\n");
        }
        return builder.toString();
    }

    private String buildCycleNumberRange() {
        if (availableCycles.size() == 1) {
            return "1";
        }
        return "1 до " + availableCycles.size();
    }

    private boolean isInvalidCycleSelection(int selection) {
        return selection < 1 || selection > availableCycles.size();
    }

    private TrainingCycleInfo getSelectedCycle(int selection) {
        if (selection >= 1 && selection <= availableCycles.size()) {
            return availableCycles.get(selection - 1);
        }
        return null;
    }

    private double parseBenchPressInput(String input) {
        String benchPressInput = input.trim().replace(',', '.');
        return Double.parseDouble(benchPressInput);
    }

    private File generateTrainingPlan(Long telegramId, double maxBenchPress, String cycleId) {
        UserInfoResponse user = getUserInfo(telegramId);
        UUID userId = user.getId();

        UserBenchPressRequest benchPressRequest = UserBenchPressRequest.builder()
                .maxBenchPress(maxBenchPress)
                .build();

        log.info("{}_TRAINING_PROGRAM_GENERATING_EXCEL: генерация Excel для пользователя {}, цикл: {}, жим: {} кг",
                SERVICE_NAME, telegramId, cycleId, maxBenchPress);

        return excelTrainingService.generateTrainingPlan(userId, benchPressRequest, cycleId);
    }

    private void sendExcelFile(Long telegramId, File excelFile, TrainingCycleInfo cycleInfo, double maxBenchPress) {
        String caption = String.format("""
                        Программа тренировок: %s
                        Максимальный жим лежа: %.1f кг
                        Автор цикла: %s""",
                cycleInfo.getDisplayName(), maxBenchPress, cycleInfo.getAuthor());

        documentSenderService.sendDocument(telegramId, excelFile, caption);
    }

    private void cleanupUserState(Long telegramId) {
        userStates.remove(telegramId);
        pendingTrainingCycles.remove(telegramId);
        pendingBenchPressValues.remove(telegramId);
    }
}