package ru.cs.vsu.social_network.telegram_bot.service.serviceImpl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.cs.vsu.social_network.telegram_bot.dto.request.UserBenchPressRequest;
import ru.cs.vsu.social_network.telegram_bot.dto.request.UserCreateRequest;
import ru.cs.vsu.social_network.telegram_bot.dto.response.ReportResponse;
import ru.cs.vsu.social_network.telegram_bot.dto.response.UserInfoResponse;
import ru.cs.vsu.social_network.telegram_bot.dto.response.UserTrainingResponse;
import ru.cs.vsu.social_network.telegram_bot.dto.response.VisitResponse;
import ru.cs.vsu.social_network.telegram_bot.dto.response.VisitorLogResponse;
import ru.cs.vsu.social_network.telegram_bot.entity.enums.ROLE;
import ru.cs.vsu.social_network.telegram_bot.provider.UserTrainingEntityProvider;
import ru.cs.vsu.social_network.telegram_bot.service.ExcelTrainingService;
import ru.cs.vsu.social_network.telegram_bot.service.ImageTrainingService;
import ru.cs.vsu.social_network.telegram_bot.service.ReportService;
import ru.cs.vsu.social_network.telegram_bot.service.TelegramCommandService;
import ru.cs.vsu.social_network.telegram_bot.service.UserService;
import ru.cs.vsu.social_network.telegram_bot.service.UserTrainingService;
import ru.cs.vsu.social_network.telegram_bot.service.VisitService;
import ru.cs.vsu.social_network.telegram_bot.service.DocumentSenderService;
import ru.cs.vsu.social_network.telegram_bot.utils.MessageConstants;
import ru.cs.vsu.social_network.telegram_bot.utils.table.TableFormatterService;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Реализация сервиса обработки Telegram команд.
 * Обрабатывает команды пользователей и администраторов тренажерного зала.
 */
@Slf4j
@Service
public class TelegramCommandServiceImpl implements TelegramCommandService {

    private static final String SERVICE_NAME = "TELEGRAM_COMMAND_SERVICE";
    private static final Pattern BENCH_PRESS_PATTERN = Pattern.compile("^\\d+(?:\\.\\d{1,2})?$");

    private final UserService userService;
    private final UserTrainingEntityProvider userTrainingEntityProvider;
    private final VisitService visitService;
    private final ReportService reportService;
    private final TableFormatterService tableFormatterService;
    private final ExcelTrainingService excelTrainingService;
    private final ImageTrainingService imageTrainingService;
    private final UserTrainingService userTrainingService;
    private final DocumentSenderService documentSenderService;

    private static final DateTimeFormatter INPUT_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final Map<Long, String> userStates = new HashMap<>();
    private final Map<Long, String> adminStates = new HashMap<>();
    private final Map<Long, Double> pendingBenchPressValues = new HashMap<>();

    /**
     * Конструктор с зависимостями.
     */
    public TelegramCommandServiceImpl(final UserService userService,
                                      final UserTrainingEntityProvider userTrainingEntityProvider,
                                      final VisitService visitService,
                                      final ReportService reportService,
                                      final TableFormatterService tableFormatterService,
                                      final ExcelTrainingService excelTrainingService,
                                      final ImageTrainingService imageTrainingService,
                                      final UserTrainingService userTrainingService,
                                      final DocumentSenderService documentSenderService) {
        this.userService = userService;
        this.userTrainingEntityProvider = userTrainingEntityProvider;
        this.visitService = visitService;
        this.reportService = reportService;
        this.tableFormatterService = tableFormatterService;
        this.excelTrainingService = excelTrainingService;
        this.imageTrainingService = imageTrainingService;
        this.userTrainingService = userTrainingService;
        this.documentSenderService = documentSenderService;
    }

    /**
     * Обрабатывает команду /start для нового пользователя.
     *
     * @param telegramId идентификатор Telegram пользователя
     * @param username   имя пользователя Telegram
     * @param firstName  имя пользователя
     * @param lastName   фамилия пользователя
     * @return приветственное сообщение
     */
    @Override
    public String handleStartCommand(final Long telegramId, final String username,
                                     final String firstName, final String lastName) {
        log.info("{}_START_COMMAND_BEGIN: обработка команды /start для Telegram ID: {}",
                SERVICE_NAME, telegramId);

        final UserCreateRequest createRequest = UserCreateRequest.builder()
                .telegramId(telegramId)
                .username(username)
                .firstName(firstName)
                .lastName(lastName)
                .displayName(firstName)
                .build();

        final UserInfoResponse user = userService.registerUser(createRequest);

        userStates.put(telegramId, "awaiting_display_name");

        final String response = String.format(
                "Добро пожаловать в \"Поколение сильных!\"\n\n" +
                        "Привет, %s!\n\n" +
                        "Как мне к вам обращаться?\n" +
                        "Введите ваше имя и фамилию\n" +
                        "Пример: Сергей Мордвинов",
                user.getFirstName() != null ? user.getFirstName() : "друг"
        );

        log.info("{}_START_COMMAND_SUCCESS: пользователь {} зарегистрирован/найден",
                SERVICE_NAME, telegramId);

        return response;
    }

    /**
     * Обрабатывает команду отметки пользователя в зале.
     *
     * @param telegramId идентификатор Telegram пользователя
     * @return сообщение о результате отметки
     */
    @Override
    public String handleInGymCommand(final Long telegramId) {
        log.info("{}_IN_GYM_COMMAND_BEGIN: обработка команды 'Я в зале' для Telegram ID: {}",
                SERVICE_NAME, telegramId);

        try {
            final VisitResponse visit = visitService.createVisitByTelegramId(telegramId);

            final UserInfoResponse user = userService.getUserByTelegramId(telegramId);

            final String response = String.format(
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

    /**
     * Обрабатывает ввод имени пользователя.
     *
     * @param telegramId  идентификатор Telegram пользователя
     * @param displayName введенное имя
     * @return подтверждение сохранения имени
     */
    @Override
    public String handleDisplayNameInput(final Long telegramId, final String displayName) {
        log.info("{}_DISPLAY_NAME_INPUT_BEGIN: обработка имени '{}' для Telegram ID: {}",
                SERVICE_NAME, displayName, telegramId);

        final String userState = userStates.get(telegramId);

        if ("awaiting_display_name".equals(userState)) {
            try {
                final UserInfoResponse user = userService.getUserByTelegramId(telegramId);

                userService.updateDisplayName(user.getId(), displayName.trim());

                userStates.remove(telegramId);

                final String response = String.format(
                        "Отлично, %s!\n\n" +
                                "Теперь я буду обращаться к вам так.\n\n" +
                                "Доступные команды:\n" +
                                "• Я в зале — Отметиться в зале\n" +
                                "• Сменить имя — Изменить имя для обращения\n" +
                                "• Составить программу тренировок — Создать индивидуальную программу\n" +
                                "• /help — Показать справку по командам",
                        displayName.trim()
                );

                log.info("{}_DISPLAY_NAME_INPUT_SUCCESS: имя пользователя {} обновлено на '{}'",
                        SERVICE_NAME, telegramId, displayName);

                return response;

            } catch (Exception e) {
                log.error("{}_DISPLAY_NAME_INPUT_ERROR: ошибка при обновлении имени для {}: {}",
                        SERVICE_NAME, telegramId, e.getMessage());

                return "Произошла ошибка при сохранении имени.\n\n" +
                        "Пожалуйста, попробуйте еще раз.";
            }
        } else if ("awaiting_new_display_name".equals(userState)) {
            try {
                final UserInfoResponse user = userService.getUserByTelegramId(telegramId);

                userService.updateDisplayName(user.getId(), displayName.trim());

                userStates.remove(telegramId);

                final String response = String.format(
                        "Имя успешно изменено!\n\n" +
                                "Теперь я буду обращаться к вам как %s.",
                        displayName.trim()
                );

                log.info("{}_CHANGE_NAME_SUCCESS: имя пользователя {} изменено на '{}'",
                        SERVICE_NAME, telegramId, displayName);

                return response;

            } catch (Exception e) {
                log.error("{}_CHANGE_NAME_ERROR: ошибка при изменении имени для {}: {}",
                        SERVICE_NAME, telegramId, e.getMessage());

                return "Произошла ошибка при изменении имени.\n\n" +
                        "Пожалуйста, попробуйте еще раз.";
            }
        } else {
            log.warn("{}_DISPLAY_NAME_INPUT_UNEXPECTED: Telegram ID {} не ожидает ввода имени",
                    SERVICE_NAME, telegramId);
            return handleUnknownCommand(telegramId);
        }
    }

    /**
     * Обрабатывает выбор формата программы тренировок.
     *
     * @param telegramId   идентификатор Telegram пользователя
     * @param formatChoice выбранный формат
     * @return сообщение о результате выбора формата
     */
    @Override
    public String handleFormatSelection(final Long telegramId, final String formatChoice) {
        log.info("{}_FORMAT_SELECTION_BEGIN: обработка выбора формата '{}' для Telegram ID: {}",
                SERVICE_NAME, formatChoice, telegramId);

        final String userState = userStates.get(telegramId);

        if (!"awaiting_format_selection".equals(userState)) {
            log.warn("{}_FORMAT_SELECTION_UNEXPECTED: Telegram ID {} не ожидает выбора формата. Текущий статус: {}",
                    SERVICE_NAME, telegramId, userState);
            return handleUnknownCommand(telegramId);
        }

        try {
            final UserInfoResponse user = userService.getUserByTelegramId(telegramId);
            final Double benchPressValue = pendingBenchPressValues.get(telegramId);

            if (benchPressValue == null) {
                log.error("{}_FORMAT_SELECTION_DATA_ERROR: значение жима лежа не найдено для {}",
                        SERVICE_NAME, telegramId);
                userStates.remove(telegramId);
                pendingBenchPressValues.remove(telegramId);
                return "Произошла ошибка при обработке данных.\n\n" +
                        "Пожалуйста, начните заново.";
            }

            final UserBenchPressRequest benchPressRequest = UserBenchPressRequest.builder()
                    .maxBenchPress(benchPressValue)
                    .build();

            log.info("{}_BENCH_PRESS_SAVING: пользователь {}, жим лежа: {} кг",
                    SERVICE_NAME, user.getId(), benchPressValue);

            final UserTrainingResponse trainingResponse =
                    userTrainingService.saveOrUpdateMaxBenchPressByTelegramId(telegramId, benchPressRequest);

            log.info("{}_BENCH_PRESS_SAVING_SUCCESS: данные сохранены, запись ID {}",
                    SERVICE_NAME, trainingResponse.getId());

            File trainingFile = null;
            String formatType = "";

            String trimmedChoice = formatChoice.trim();

            log.info("{}_FORMAT_SELECTION_CHOICE: получен выбор '{}', trimmed: '{}'",
                    SERVICE_NAME, formatChoice, trimmedChoice);

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
                            SERVICE_NAME, telegramId, formatChoice);

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
                            SERVICE_NAME, telegramId, formatChoice);

                    trainingFile = excelTrainingService.generateTrainingPlan(user.getId(), benchPressRequest);
                    formatType = "Excel таблица";

                    log.info("{}_EXCEL_GENERATION_SUCCESS: файл создан: {}",
                            SERVICE_NAME, trainingFile.getAbsolutePath());

                } else {
                    log.warn("{}_FORMAT_SELECTION_UNKNOWN: неизвестный формат '{}' (trimmed: '{}')",
                            SERVICE_NAME, formatChoice, trimmedChoice);
                    return "Пожалуйста, выберите корректный формат:\n\n" +
                            "1️⃣ *Изображение* (рекомендуется для Telegram)\n" +
                            "2️⃣ *Excel таблица* (для компьютера)\n\n" +
                            "📝 Введите '1' или '2'";
                }
            }

            final String caption = buildTrainingProgramCaption(user, benchPressValue, formatType);
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
     * Обрабатывает команду получения отчета за день.
     *
     * @param telegramId идентификатор Telegram администратора
     * @param dateStr    строка с датой
     * @return отчет за указанный день
     */
    @Override
    public String handleDailyReportCommand(final Long telegramId, final String dateStr) {
        log.info("{}_DAILY_REPORT_COMMAND_BEGIN: администратор {}, дата: {}",
                SERVICE_NAME, telegramId, dateStr);

        try {
            final UserInfoResponse user = userService.getUserByTelegramId(telegramId);

            if (user.getRole() != ROLE.ADMIN) {
                return "Доступ запрещен!\n\n" +
                        "Эта команда доступна только администраторам.";
            }

            final LocalDate date;
            if (dateStr == null || dateStr.trim().isEmpty()) {
                date = LocalDate.now();
            } else {
                try {
                    if ("сегодня".equalsIgnoreCase(dateStr.trim())) {
                        date = LocalDate.now();
                    } else if ("вчера".equalsIgnoreCase(dateStr.trim())) {
                        date = LocalDate.now().minusDays(1);
                    } else {
                        date = LocalDate.parse(dateStr.trim(), INPUT_DATE_FORMATTER);
                    }
                } catch (DateTimeParseException e) {
                    return "Неверный формат даты!\n\n" +
                            "Используйте формат: ДД.ММ.ГГГГ\n" +
                            "Пример: /report 06.12.2025\n\n" +
                            "Или специальные значения:\n" +
                            "• сегодня\n" +
                            "• вчера";
                }
            }

            final VisitorLogResponse report = reportService.generateDailyReportForDate(
                    user.getId(), date);

            adminStates.remove(telegramId);

            log.info("{}_DAILY_REPORT_COMMAND_SUCCESS: отчет за {} сгенерирован для администратора {}",
                    SERVICE_NAME, date, telegramId);

            return "Отчет посещений за " + date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) + "\n\n" +
                    report.getFormattedReport();

        } catch (Exception e) {
            log.error("{}_DAILY_REPORT_COMMAND_ERROR: ошибка для {}: {}",
                    SERVICE_NAME, telegramId, e.getMessage());

            return "Произошла ошибка при генерации отчета.\n\n" +
                    "Проверьте формат даты и попробуйте еще раз.";
        }
    }

    /**
     * Обрабатывает команду получения отчета за период.
     *
     * @param telegramId   идентификатор Telegram администратора
     * @param startDateStr начальная дата периода
     * @param endDateStr   конечная дата периода
     * @return отчет за указанный период
     */
    @Override
    public String handlePeriodReportCommand(final Long telegramId,
                                            final String startDateStr,
                                            final String endDateStr) {
        log.info("{}_PERIOD_REPORT_COMMAND_BEGIN: администратор {}, период: {} - {}",
                SERVICE_NAME, telegramId, startDateStr, endDateStr);

        try {
            final UserInfoResponse user = userService.getUserByTelegramId(telegramId);

            if (user.getRole() != ROLE.ADMIN) {
                return "Доступ запрещен!\n\n" +
                        "Эта команда доступна только администраторам.";
            }

            final LocalDate startDate;
            final LocalDate endDate;

            try {
                startDate = LocalDate.parse(startDateStr.trim(), INPUT_DATE_FORMATTER);
                endDate = LocalDate.parse(endDateStr.trim(), INPUT_DATE_FORMATTER);
            } catch (DateTimeParseException e) {
                return "Неверный формат даты!\n\n" +
                        "Используйте формат: ДД.ММ.ГГГГ\n" +
                        "Пример: /report period 01.12.2025 06.12.2025";
            }

            if (startDate.isAfter(endDate)) {
                return "Дата начала не может быть позже даты окончания!";
            }

            final ReportResponse report = reportService.generatePeriodReport(
                    user.getId(), startDate, endDate);

            adminStates.remove(telegramId);

            log.info("{}_PERIOD_REPORT_COMMAND_SUCCESS: отчет за период {} - {} сгенерирован",
                    SERVICE_NAME, startDate, endDate);

            return report.getTelegramFormattedReport();

        } catch (Exception e) {
            log.error("{}_PERIOD_REPORT_COMMAND_ERROR: ошибка для {}: {}",
                    SERVICE_NAME, telegramId, e.getMessage());

            return "Произошла ошибка при генерации отчета.\n\n" +
                    "Проверьте формат дат и попробуйте еще раз.";
        }
    }

    /**
     * Обрабатывает команду получения таблицы посещений.
     *
     * @param telegramId идентификатор Telegram администратора
     * @param input      входные параметры команды
     * @return таблица посещений
     */
    @Override
    public String handleTableCommand(final Long telegramId, final String input) {
        log.info("{}_TABLE_COMMAND_BEGIN: администратор {}, ввод: {}",
                SERVICE_NAME, telegramId, input);

        try {
            final UserInfoResponse user = userService.getUserByTelegramId(telegramId);

            if (user.getRole() != ROLE.ADMIN) {
                log.warn("{}_TABLE_COMMAND_ACCESS_DENIED: пользователь {} не является администратором",
                        SERVICE_NAME, telegramId);
                return "Доступ запрещен! Эта команда доступна только администраторам.";
            }

            if (input == null || input.trim().isEmpty()) {
                log.info("{}_TABLE_COMMAND_GET_TODAY: администратор {}", SERVICE_NAME, telegramId);
                return getTableForToday(user.getId());
            }

            final String[] parts = input.trim().split("\\s+");

            if (parts.length == 1) {
                return getTableForDate(user.getId(), parts[0]);
            } else if (parts.length == 2) {
                return getTableForPeriod(user.getId(), parts[0], parts[1]);
            } else {
                log.warn("{}_TABLE_COMMAND_INVALID_FORMAT: неверное количество параметров: {}",
                        SERVICE_NAME, parts.length);
                return tableFormatterService.getTableUsageInstructions();
            }

        } catch (Exception e) {
            log.error("{}_TABLE_COMMAND_ERROR: ошибка при обработке команды для {}: {}",
                    SERVICE_NAME, telegramId, e.getMessage(), e);
            return "Произошла ошибка при получении таблицы.\n\n" +
                    "Проверьте формат даты и попробуйте еще раз.";
        }
    }

    /**
     * Обрабатывает команды администраторского меню.
     *
     * @param telegramId  идентификатор Telegram администратора
     * @param menuCommand команда меню
     * @return результат обработки команды меню
     */
    @Override
    public String handleAdminMenuCommand(final Long telegramId, final String menuCommand) {
        log.info("{}_ADMIN_MENU_COMMAND_BEGIN: администратор {}, команда меню: {}",
                SERVICE_NAME, telegramId, menuCommand);

        try {
            final UserInfoResponse user = userService.getUserByTelegramId(telegramId);

            if (user.getRole() != ROLE.ADMIN) {
                return "Доступ запрещен! Эта команда доступна только администраторам.";
            }

            if ("Получить журнал за сегодня".equals(menuCommand)) {
                return handleDailyReportCommand(telegramId, null);
            } else if (menuCommand.startsWith("Получить журнал за день")) {
                String datePart = menuCommand.replace("Получить журнал за день", "").trim();
                if (datePart.isEmpty()) {
                    adminStates.put(telegramId, "awaiting_specific_date");
                    return "Выберите дату для отчета\n\n" +
                            "Введите дату в формате ДД.ММ.ГГГГ\n" +
                            "Пример: 06.12.2025\n\n" +
                            "Или используйте специальные значения:\n" +
                            "• сегодня\n" +
                            "• вчера";
                } else {
                    return handleDailyReportCommand(telegramId, datePart);
                }
            } else if ("Получить журнал за период".equals(menuCommand)) {
                adminStates.put(telegramId, "awaiting_start_date");
                return "Выберите период для отчета\n\n" +
                        "Введите начальную дату в формате ДД.ММ.ГГГГ\n" +
                        "Пример: 01.12.2025";
            }

            return handleUnknownCommand(telegramId);

        } catch (Exception e) {
            log.error("{}_ADMIN_MENU_COMMAND_ERROR: ошибка для {}: {}",
                    SERVICE_NAME, telegramId, e.getMessage());
            return "Произошла ошибка при обработке команды меню.";
        }
    }

    /**
     * Обрабатывает ввод даты администратором.
     *
     * @param telegramId идентификатор Telegram администратора
     * @param dateInput  введенная дата
     * @return результат обработки ввода даты
     */
    @Override
    public String handleAdminDateInput(final Long telegramId, final String dateInput) {
        log.info("{}_ADMIN_DATE_INPUT_BEGIN: администратор {}, ввод: {}",
                SERVICE_NAME, telegramId, dateInput);

        final String state = adminStates.get(telegramId);
        if (state == null) {
            return handleUnknownCommand(telegramId);
        }

        try {
            if ("awaiting_specific_date".equals(state)) {
                adminStates.remove(telegramId);
                return handleDailyReportCommand(telegramId, dateInput);
            } else if ("awaiting_start_date".equals(state)) {
                try {
                    LocalDate.parse(dateInput.trim(), INPUT_DATE_FORMATTER);
                    adminStates.put(telegramId, "awaiting_end_date_" + dateInput);
                    return "Теперь введите конечную дату\n\n" +
                            "Формат: ДД.ММ.ГГГГ\n" +
                            "Пример: 06.12.2025";
                } catch (DateTimeParseException e) {
                    return "Неверный формат даты!\n\n" +
                            "Используйте формат: ДД.ММ.ГГГГ\n" +
                            "Пример: 01.12.2025";
                }
            } else if (state.startsWith("awaiting_end_date_")) {
                final String startDateStr = state.substring("awaiting_end_date_".length());
                adminStates.remove(telegramId);
                return handlePeriodReportCommand(telegramId, startDateStr, dateInput);
            }

            return handleUnknownCommand(telegramId);

        } catch (Exception e) {
            log.error("{}_ADMIN_DATE_INPUT_ERROR: ошибка для {}: {}",
                    SERVICE_NAME, telegramId, e.getMessage());
            return "Произошла ошибка при обработке даты.\n\n" +
                    "Проверьте формат и попробуйте еще раз.";
        }
    }

    /**
     * Обрабатывает команду /help.
     *
     * @param telegramId идентификатор Telegram пользователя
     * @return справочная информация по командам
     */
    @Override
    public String handleHelpCommand(final Long telegramId) {
        log.info("{}_HELP_COMMAND_BEGIN: обработка команды /help для Telegram ID: {}",
                SERVICE_NAME, telegramId);

        try {
            final UserInfoResponse user = userService.getUserByTelegramId(telegramId);

            final String displayName = user.getDisplayName() != null ?
                    user.getDisplayName() : user.getFirstName();

            final StringBuilder response = new StringBuilder();
            response.append(String.format("Справка по командам, %s!\n\n", displayName));

            response.append("Основные команды:\n");
            response.append("• /start — Начать работу с ботом\n");
            response.append("• Я в зале — Отметиться в тренажерном зале\n");
            response.append("• Сменить имя — Изменить имя для обращения\n");
            response.append("• Составить программу тренировок — Создать индивидуальную программу\n");
            response.append("• /help — Показать эту справку\n");

            if (user.getRole() == ROLE.ADMIN) {
                response.append("\nКоманды администратора:\n");
                response.append("• /report — Отчет посещений за сегодня\n");
                response.append("• /report дата — Отчет за определенный день\n");
                response.append("  Пример: /report 06.12.2025\n");
                response.append("• /report period начало конец — Отчет за период\n");
                response.append("  Пример: /report period 01.12.2025 06.12.2025\n");
                response.append("• /table — Таблица посещений за сегодня\n");
                response.append("• /table дата — Таблица за определенный день\n");
                response.append("• /table дата-начало дата-конец — Таблица за период\n");

                response.append("\nКнопки меню администратора:\n");
                response.append("• Получить журнал за сегодня\n");
                response.append("• Получить журнал за день\n");
                response.append("• Получить журнал за период\n");
            }

            response.append("\nИспользуйте кнопки меню или введите команду вручную.");

            log.info("{}_HELP_COMMAND_SUCCESS: справка отправлена пользователю {}",
                    SERVICE_NAME, telegramId);

            return response.toString();

        } catch (Exception e) {
            log.error("{}_HELP_COMMAND_ERROR: ошибка при обработке команды /help для {}: {}",
                    SERVICE_NAME, telegramId, e.getMessage());

            return "Добро пожаловать в тренажерный зал!\n\n" +
                    "Для начала работы введите команду /start";
        }
    }

    /**
     * Обрабатывает неизвестную команду.
     *
     * @param telegramId идентификатор Telegram пользователя
     * @return сообщение с подсказкой
     */
    @Override
    public String handleUnknownCommand(final Long telegramId) {
        log.debug("{}_UNKNOWN_COMMAND: Telegram ID {}", SERVICE_NAME, telegramId);

        final String userState = userStates.get(telegramId);
        final String adminState = adminStates.get(telegramId);

        if ("awaiting_display_name".equals(userState)) {
            return "Пожалуйста, введите имя для обращения.\n\n" +
                    "Пример: Сергей Мордвинов";
        }

        if ("awaiting_bench_press".equals(userState)) {
            return "Программа тренировок\n\n" +
                    "Какой ваш максимальный жим лежа?\n" +
                    "Пример: 102,5 или 105\n\n" +
                    "Введите число в килограммах (можно с десятичной точкой):";
        }

        if ("awaiting_format_selection".equals(userState)) {
            return "Выберите формат программы тренировок:\n\n" +
                    "1. Изображение (рекомендуется для Telegram)\n" +
                    "2. Excel таблица (для компьютера)\n\n" +
                    "Введите '1' или '2'";
        }

        if (adminState != null) {
            if ("awaiting_specific_date".equals(adminState)) {
                return "Ожидается ввод даты\n\n" +
                        "Введите дату в формате ДД.ММ.ГГГГ\n" +
                        "Пример: 06.12.2025\n\n" +
                        "Или используйте специальные значения:\n" +
                        "• сегодня\n" +
                        "• вчера";
            } else if ("awaiting_start_date".equals(adminState)) {
                return "Ожидается ввод начальной даты\n\n" +
                        "Введите дату в формате ДД.ММ.ГГГГ\n" +
                        "Пример: 01.12.2025";
            } else if (adminState.startsWith("awaiting_end_date_")) {
                return "Ожидается ввод конечной даты\n\n" +
                        "Введите дату в формате ДД.ММ.ГГГГ\n" +
                        "Пример: 06.12.2025";
            }
        }

        try {
            final UserInfoResponse user = userService.getUserByTelegramId(telegramId);

            final String displayName = user.getDisplayName() != null ?
                    user.getDisplayName() : user.getFirstName();

            final StringBuilder response = new StringBuilder();
            response.append(String.format("%s, я не понял вашу команду.\n\n", displayName));

            response.append("Основные команды:\n");
            response.append("• /start — Начать работу с ботом\n");
            response.append("• Я в зале — Отметиться в тренажерном зале\n");
            response.append("• Сменить имя — Изменить имя для обращения\n");
            response.append("• Составить программу тренировок — Создать индивидуальную программу\n");
            response.append("• /help — Показать справку по командам\n");

            if (user.getRole() == ROLE.ADMIN) {
                response.append("\nКоманды администратора:\n");
                response.append("• /report — Отчет посещений за сегодня\n");
                response.append("• /report дата — Отчет за определенный день\n");
                response.append("  Пример: /report 06.12.2025\n");
                response.append("• /report period начало конец — Отчет за период\n");
                response.append("  Пример: /report period 01.12.2025 06.12.2025\n");
                response.append("• /table — Таблица посещений за сегодня\n");
                response.append("• /table дата — Таблица за определенный день\n");
                response.append("• /table дата-начало дата-конец — Таблица за период\n");

                response.append("\nКнопки меню администратора:\n");
                response.append("• Получить журнал за сегодня\n");
                response.append("• Получить журнал за день\n");
                response.append("• Получить журнал за период\n");
            }

            response.append("\nИспользуйте кнопки меню или введите команду вручную.");

            return response.toString();

        } catch (Exception e) {
            return "Добро пожаловать в тренажерный зал!\n\n" +
                    "Для начала работы введите команду /start";
        }
    }

    /**
     * Обрабатывает команду смены имени.
     *
     * @param telegramId идентификатор Telegram пользователя
     * @return запрос на ввод нового имени
     */
    @Override
    public String handleChangeNameCommand(final Long telegramId) {
        log.info("{}_CHANGE_NAME_COMMAND_BEGIN: пользователь {} хочет сменить имя",
                SERVICE_NAME, telegramId);

        try {
            final UserInfoResponse user = userService.getUserByTelegramId(telegramId);

            userStates.put(telegramId, "awaiting_new_display_name");

            final String response = String.format(
                    "%s, вы хотите изменить имя для обращения.\n\n" +
                            "Пожалуйста, введите новое имя и фамилию.\n" +
                            "Пример: Сергей Мордвинов",
                    user.getDisplayName() != null ? user.getDisplayName() : user.getFirstName()
            );

            log.info("{}_CHANGE_NAME_COMMAND_SUCCESS: пользователь {} ожидает ввода нового имени",
                    SERVICE_NAME, telegramId);

            return response;

        } catch (Exception e) {
            log.error("{}_CHANGE_NAME_COMMAND_ERROR: ошибка для {}: {}",
                    SERVICE_NAME, telegramId, e.getMessage());

            return "Произошла ошибка при запросе смены имени.\n\n" +
                    "Пожалуйста, попробуйте позже.";
        }
    }

    /**
     * Обрабатывает команду составления программы тренировок.
     *
     * @param telegramId идентификатор Telegram пользователя
     * @return запрос на ввод максимального жима лежа
     */
    @Override
    public String handleTrainingProgramCommand(final Long telegramId) {
        log.info("{}_TRAINING_PROGRAM_COMMAND_BEGIN: пользователь {} запрашивает программу",
                SERVICE_NAME, telegramId);

        try {
            final UserInfoResponse user = userService.getUserByTelegramId(telegramId);

            userStates.put(telegramId, "awaiting_bench_press");

            final Optional<Double> existingBenchPress = userTrainingEntityProvider.getMaxBenchPressByTelegramId(telegramId);

            final StringBuilder response = new StringBuilder();
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

    /**
     * Обрабатывает ввод максимального жима лежа.
     *
     * @param telegramId      идентификатор Telegram пользователя
     * @param benchPressInput введенное значение жима лежа
     * @return запрос на выбор формата программы
     */
    @Override
    public String handleBenchPressInput(final Long telegramId, String benchPressInput) {
        log.info("{}_BENCH_PRESS_INPUT_BEGIN: обработка ввода '{}' для Telegram ID: {}",
                SERVICE_NAME, benchPressInput, telegramId);

        final String userState = userStates.get(telegramId);

        if (!"awaiting_bench_press".equals(userState)) {
            log.warn("{}_BENCH_PRESS_INPUT_UNEXPECTED: Telegram ID {} не ожидает ввода жима лежа",
                    SERVICE_NAME, telegramId);
            return handleUnknownCommand(telegramId);
        }

        try {
            benchPressInput = benchPressInput.trim().replace(',', '.');

            if (!BENCH_PRESS_PATTERN.matcher(benchPressInput).matches()) {
                log.warn("{}_BENCH_PRESS_INPUT_INVALID_FORMAT: некорректный формат: {}",
                        SERVICE_NAME, benchPressInput);
                return "Неверный формат!\n\n" +
                        "Пожалуйста, введите число.\n" +
                        "Пример: 102,5 или 105\n" +
                        "Можно использовать десятичную точку или запятую.";
            }

            final double maxBenchPress;
            try {
                maxBenchPress = Double.parseDouble(benchPressInput);
            } catch (NumberFormatException e) {
                log.warn("{}_BENCH_PRESS_INPUT_INVALID_NUMBER_FORMAT: не удалось преобразовать: {}",
                        SERVICE_NAME, benchPressInput);
                return "Неверный формат числа!\n\n" +
                        "Пожалуйста, введите число.\n" +
                        "Пример: 102,5 или 105";
            }

            final UserInfoResponse user = userService.getUserByTelegramId(telegramId);

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

    /**
     * Создает подпись для программы тренировок.
     *
     * @param user              информация о пользователе
     * @param currentBenchPress текущий жим лежа
     * @param formatType        тип формата
     * @return текст подписи
     */
    private String buildTrainingProgramCaption(final UserInfoResponse user,
                                               final double currentBenchPress,
                                               final String formatType) {
        final StringBuilder caption = new StringBuilder();

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

    /**
     * Получает таблицу посещений за текущий день.
     *
     * @param adminUserId идентификатор администратора
     * @return таблица посещений за сегодня
     */
    private String getTableForToday(final UUID adminUserId) {
        log.info("{}_TABLE_FOR_TODAY_BEGIN: администратор {}", SERVICE_NAME, adminUserId);

        final LocalDate today = LocalDate.now();
        final Optional<VisitorLogResponse> existingLog = reportService.getVisitorLogByDate(adminUserId, today);

        return tableFormatterService.formatTableForToday(adminUserId.toString(), existingLog);
    }

    /**
     * Получает таблицу посещений за указанную дату.
     *
     * @param adminUserId идентификатор администратора
     * @param dateStr     строка с датой
     * @return таблица посещений за указанную дату
     */
    private String getTableForDate(final UUID adminUserId, final String dateStr) {
        log.info("{}_TABLE_FOR_DATE_BEGIN: администратор {}, дата: {}",
                SERVICE_NAME, adminUserId, dateStr);

        try {
            final LocalDate date = LocalDate.parse(dateStr.trim(), INPUT_DATE_FORMATTER);
            final Optional<VisitorLogResponse> existingLog = reportService.getVisitorLogByDate(adminUserId, date);

            return tableFormatterService.formatTableForDate(adminUserId.toString(), date, existingLog);
        } catch (DateTimeParseException e) {
            log.warn("{}_TABLE_FOR_DATE_INVALID_FORMAT: неверный формат даты: {}",
                    SERVICE_NAME, dateStr);
            return "Неверный формат даты!\n\n" +
                    "Используйте формат: ДД.ММ.ГГГГ\n" +
                    "Пример: /report 06.12.2025";
        }
    }

    /**
     * Получает таблицу посещений за указанный период.
     *
     * @param adminUserId   идентификатор администратора
     * @param startDateStr  начальная дата периода
     * @param endDateStr    конечная дата периода
     * @return таблица посещений за период
     */
    private String getTableForPeriod(final UUID adminUserId, final String startDateStr, final String endDateStr) {
        log.info("{}_TABLE_FOR_PERIOD_BEGIN: администратор {}, период: {} - {}",
                SERVICE_NAME, adminUserId, startDateStr, endDateStr);

        try {
            final LocalDate startDate = LocalDate.parse(startDateStr.trim(), INPUT_DATE_FORMATTER);
            final LocalDate endDate = LocalDate.parse(endDateStr.trim(), INPUT_DATE_FORMATTER);

            if (startDate.isAfter(endDate)) {
                log.warn("{}_TABLE_FOR_PERIOD_INVALID_DATES: дата начала {} позже даты окончания {}",
                        SERVICE_NAME, startDate, endDate);
                return "Дата начала не может быть позже даты окончания!";
            }

            final var logs = reportService.getVisitorLogsByPeriod(adminUserId, startDate, endDate);

            if (logs.isEmpty()) {
                return tableFormatterService.formatPeriodTableEmpty(startDate, endDate);
            }

            return tableFormatterService.formatTableForPeriod(startDate, endDate, logs);

        } catch (DateTimeParseException e) {
            log.warn("{}_TABLE_FOR_PERIOD_INVALID_FORMAT: неверный формат дат: {} - {}",
                    SERVICE_NAME, startDateStr, endDateStr);
            return "Неверный формат даты!\n\n" +
                    "Используйте формат: ДД.ММ.ГГГГ ДД.ММ.ГГГГ\n" +
                    "Пример: /report period 01.12.2025 06.12.2025";
        }
    }
}