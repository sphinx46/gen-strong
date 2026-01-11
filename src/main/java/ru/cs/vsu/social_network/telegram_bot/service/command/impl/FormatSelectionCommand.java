package ru.cs.vsu.social_network.telegram_bot.service.command.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.cs.vsu.social_network.telegram_bot.dto.request.UserBenchPressRequest;
import ru.cs.vsu.social_network.telegram_bot.dto.response.UserInfoResponse;
import ru.cs.vsu.social_network.telegram_bot.dto.response.UserTrainingResponse;
import ru.cs.vsu.social_network.telegram_bot.service.*;
import ru.cs.vsu.social_network.telegram_bot.service.command.BaseTelegramCommand;
import ru.cs.vsu.social_network.telegram_bot.validation.UserValidator;

import java.io.File;

/**
 * Команда для обработки выбора формата программы тренировок.
 * Генерирует программу тренировок в выбранном формате.
 */
@Slf4j
@Component
public class FormatSelectionCommand extends BaseTelegramCommand {

    private final ExcelTrainingService excelTrainingService;
    private final ImageTrainingService imageTrainingService;
    private final UserTrainingService userTrainingService;
    private final DocumentSenderService documentSenderService;

    /**
     * Конструктор команды FormatSelection.
     *
     * @param userService сервис пользователей
     * @param userValidator валидатор пользователей
     * @param excelTrainingService сервис генерации Excel программ
     * @param imageTrainingService сервис генерации изображений программ
     * @param userTrainingService сервис тренировок пользователей
     * @param documentSenderService сервис отправки документов
     */
    public FormatSelectionCommand(UserService userService, UserValidator userValidator,
                                  ExcelTrainingService excelTrainingService,
                                  ImageTrainingService imageTrainingService,
                                  UserTrainingService userTrainingService,
                                  DocumentSenderService documentSenderService) {
        super(userService, userValidator);
        this.excelTrainingService = excelTrainingService;
        this.imageTrainingService = imageTrainingService;
        this.userTrainingService = userTrainingService;
        this.documentSenderService = documentSenderService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String execute(Long telegramId, String input) {
        log.info("{}_FORMAT_SELECTION_BEGIN: обработка выбора формата '{}' для Telegram ID: {}",
                SERVICE_NAME, input, telegramId);

        String userState = userStates.get(telegramId);

        if (!"awaiting_format_selection".equals(userState)) {
            log.warn("{}_FORMAT_SELECTION_UNEXPECTED: Telegram ID {} не ожидает выбора формата. Текущий статус: {}",
                    SERVICE_NAME, telegramId, userState);
            return "Неожиданный запрос. Пожалуйста, используйте команды из меню.";
        }

        try {
            UserInfoResponse user = getUserInfo(telegramId);
            Double benchPressValue = pendingBenchPressValues.get(telegramId);

            if (benchPressValue == null) {
                log.error("{}_FORMAT_SELECTION_DATA_ERROR: значение жима лежа не найдено для {}",
                        SERVICE_NAME, telegramId);
                userStates.remove(telegramId);
                pendingBenchPressValues.remove(telegramId);
                return "Произошла ошибка при обработке данных.\n\n" +
                        "Пожалуйста, начните заново.";
            }

            UserBenchPressRequest benchPressRequest = UserBenchPressRequest.builder()
                    .maxBenchPress(benchPressValue)
                    .build();

            log.info("{}_BENCH_PRESS_SAVING: пользователь {}, жим лежа: {} кг",
                    SERVICE_NAME, user.getId(), benchPressValue);

            UserTrainingResponse trainingResponse =
                    userTrainingService.saveOrUpdateMaxBenchPressByTelegramId(telegramId, benchPressRequest);

            log.info("{}_BENCH_PRESS_SAVING_SUCCESS: данные сохранены, запись ID {}",
                    SERVICE_NAME, trainingResponse.getId());

            File trainingFile;
            String formatType;

            String trimmedChoice = input.trim();

            log.info("{}_FORMAT_SELECTION_CHOICE: получен выбор '{}', trimmed: '{}'",
                    SERVICE_NAME, input, trimmedChoice);

            if ("1".equals(trimmedChoice) || "один".equalsIgnoreCase(trimmedChoice)) {
                log.info("{}_IMAGE_GENERATION_BEGIN: пользователь {} выбрал '1' - изображение",
                        SERVICE_NAME, telegramId);

                trainingFile = imageTrainingService.generateTrainingPlanImage(user.getId(), benchPressRequest);
                formatType = "изображение";

                log.info("{}_IMAGE_GENERATION_SUCCESS: файл создан: {}",
                        SERVICE_NAME, trainingFile.getAbsolutePath());

            } else if ("2".equals(trimmedChoice) || "два".equalsIgnoreCase(trimmedChoice)) {
                log.info("{}_EXCEL_GENERATION_BEGIN: пользователь {} выбрал '2' - Excel",
                        SERVICE_NAME, telegramId);

                trainingFile = excelTrainingService.generateTrainingPlan(user.getId(), benchPressRequest);
                formatType = "Excel таблица";

                log.info("{}_EXCEL_GENERATION_SUCCESS: файл создан: {}",
                        SERVICE_NAME, trainingFile.getAbsolutePath());

            } else {
                String normalizedChoice = trimmedChoice.toLowerCase();

                if ("изображение".equals(normalizedChoice) ||
                        "картинка".equals(normalizedChoice) ||
                        "image".equals(normalizedChoice) ||
                        "img".equals(normalizedChoice) ||
                        "фото".equals(normalizedChoice)) {

                    log.info("{}_IMAGE_GENERATION_BEGIN: пользователь {} выбрал '{}' - изображение",
                            SERVICE_NAME, telegramId, input);

                    trainingFile = imageTrainingService.generateTrainingPlanImage(user.getId(), benchPressRequest);
                    formatType = "изображение";

                    log.info("{}_IMAGE_GENERATION_SUCCESS: файл создан: {}",
                            SERVICE_NAME, trainingFile.getAbsolutePath());

                } else if ("excel".equals(normalizedChoice) ||
                        "таблица".equals(normalizedChoice) ||
                        "exl".equals(normalizedChoice) ||
                        "эксэль".equals(normalizedChoice) ||
                        "эксель".equals(normalizedChoice)) {

                    log.info("{}_EXCEL_GENERATION_BEGIN: пользователь {} выбрал '{}' - Excel",
                            SERVICE_NAME, telegramId, input);

                    trainingFile = excelTrainingService.generateTrainingPlan(user.getId(), benchPressRequest);
                    formatType = "Excel таблица";

                    log.info("{}_EXCEL_GENERATION_SUCCESS: файл создан: {}",
                            SERVICE_NAME, trainingFile.getAbsolutePath());

                } else {
                    log.warn("{}_FORMAT_SELECTION_UNKNOWN: неизвестный формат '{}' (trimmed: '{}')",
                            SERVICE_NAME, input, trimmedChoice);
                    return "Пожалуйста, выберите корректный формат:\n\n" +
                            "1️⃣ *Изображение* (рекомендуется для Telegram)\n" +
                            "2️⃣ *Excel таблица* (для компьютера)\n\n" +
                            "📝 Введите '1' или '2'";
                }
            }

            String caption = buildTrainingProgramCaption(user, benchPressValue, formatType);
            documentSenderService.sendDocument(telegramId, trainingFile, caption);

            userStates.remove(telegramId);
            pendingBenchPressValues.remove(telegramId);

            log.info("{}_TRAINING_PROGRAM_SEND_SUCCESS: программа в формате {} отправлена пользователю {}",
                    SERVICE_NAME, formatType, telegramId);

            return "Программа отправлена!\n\n" +
                    "Файл с индивидуальной программой тренировок загружается...";

        } catch (Exception e) {
            log.error("{}_FORMAT_SELECTION_ERROR: ошибка при генерации программы для {}: {}",
                    SERVICE_NAME, telegramId, e.getMessage(), e);

            userStates.remove(telegramId);
            pendingBenchPressValues.remove(telegramId);

            return "Не удалось сгенерировать программу тренировок.\n\n" +
                    "Пожалуйста, попробуйте позже или обратитесь к администратору.";
        }
    }

    /**
     * Создает подпись для программы тренировок.
     *
     * @param user информация о пользователе
     * @param currentBenchPress текущий жим лежа
     * @param formatType тип формата
     * @return текст подписи
     */
    private String buildTrainingProgramCaption(UserInfoResponse user,
                                               double currentBenchPress,
                                               String formatType) {
        StringBuilder caption = new StringBuilder();

        caption.append(String.format("%s, ваша индивидуальная программа тренировок готова!\n\n",
                user.getDisplayName() != null ? user.getDisplayName() : user.getFirstName()));

        caption.append(String.format("Максимальный жим лежа: %.1f кг\n\n", currentBenchPress));

        caption.append("Тренировочная система «Гусеница новая»\n");
        caption.append("Автор: заслуженный тренер России Суровецкий А.Е.\n\n");

        caption.append("Файл содержит:\n");
        caption.append("• Расчет рабочих весов по формуле\n");
        caption.append("• План тренировок на 8-недельный цикл\n");
        caption.append("• Процентные соотношения от вашего максимума\n");
        caption.append("• Рекомендации по прогрессии нагрузки\n\n");

        caption.append("Удачных тренировок и новых рекордов!\n\n");
        caption.append("Формат: ").append(formatType);

        return caption.toString();
    }
}