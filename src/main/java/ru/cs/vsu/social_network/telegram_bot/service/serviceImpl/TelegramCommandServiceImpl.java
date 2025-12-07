package ru.cs.vsu.social_network.telegram_bot.service.serviceImpl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.cs.vsu.social_network.telegram_bot.dto.request.UserCreateRequest;
import ru.cs.vsu.social_network.telegram_bot.dto.response.ReportResponse;
import ru.cs.vsu.social_network.telegram_bot.dto.response.UserInfoResponse;
import ru.cs.vsu.social_network.telegram_bot.dto.response.VisitResponse;
import ru.cs.vsu.social_network.telegram_bot.dto.response.VisitorLogResponse;
import ru.cs.vsu.social_network.telegram_bot.entity.enums.ROLE;
import ru.cs.vsu.social_network.telegram_bot.service.ReportService;
import ru.cs.vsu.social_network.telegram_bot.service.TelegramCommandService;
import ru.cs.vsu.social_network.telegram_bot.service.UserService;
import ru.cs.vsu.social_network.telegram_bot.service.VisitService;
import ru.cs.vsu.social_network.telegram_bot.utils.MessageConstants;
import ru.cs.vsu.social_network.telegram_bot.utils.table.TableFormatterService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Реализация сервиса для обработки команд Telegram бота.
 * Управляет входящими командами, состояниями пользователей и формированием ответов.
 */
@Slf4j
@Service
public class TelegramCommandServiceImpl implements TelegramCommandService {

    private static final String SERVICE_NAME = "TELEGRAM_КОМАНДА_СЕРВИС";

    private final UserService userService;
    private final VisitService visitService;
    private final ReportService reportService;
    private final TableFormatterService tableFormatterService;

    private static final DateTimeFormatter INPUT_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final Map<Long, String> userStates = new HashMap<>();
    private final Map<Long, String> adminStates = new HashMap<>();

    public TelegramCommandServiceImpl(final UserService userService,
                                      final VisitService visitService,
                                      final ReportService reportService,
                                      final TableFormatterService tableFormatterService) {
        this.userService = userService;
        this.visitService = visitService;
        this.reportService = reportService;
        this.tableFormatterService = tableFormatterService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String handleStartCommand(final Long telegramId, final String username,
                                     final String firstName, final String lastName) {
        log.info("{}_КОМАНДА_START_НАЧАЛО: обработка команды /start для Telegram ID: {}",
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
                "👋 Привет, %s! Добро пожаловать в \"Поколение сильных!\"\n\n" +
                        "Как мне к вам обращаться? (Введите ваше имя и фамилию)\n" +
                        "Пример: *Иван* или *Спортсмен123*",
                user.getFirstName() != null ? user.getFirstName() : "друг"
        );

        log.info("{}_КОМАНДА_START_УСПЕХ: пользователь {} зарегистрирован/найден",
                SERVICE_NAME, telegramId);

        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String handleInGymCommand(final Long telegramId) {
        log.info("{}_КОМАНДА_В_ЗАЛЕ_НАЧАЛО: обработка команды 'Я в зале' для Telegram ID: {}",
                SERVICE_NAME, telegramId);

        try {
            final VisitResponse visit = visitService.createVisitByTelegramId(telegramId);

            final UserInfoResponse user = userService.getUserByTelegramId(telegramId);

            final String response = String.format(
                    "✅ *%s, вы успешно отмечены в зале!*\n\n" +
                            "Журнал за сегодня будет сформирован администратором.",
                    user.getDisplayName() != null ? user.getDisplayName() : user.getFirstName()
            );

            log.info("{}_КОМАНДА_В_ЗАЛЕ_УСПЕХ: пользователь {} отмечен в зале",
                    SERVICE_NAME, telegramId);

            return response;

        } catch (Exception e) {
            log.error("{}_КОМАНДА_В_ЗАЛЕ_ОШИБКА: ошибка при отметке пользователя {}: {}",
                    SERVICE_NAME, telegramId, e.getMessage());

            if (e.getMessage().contains(MessageConstants.VISIT_ALREADY_FAILURE)) {
                return "⚠️ *Вы уже отметились сегодня в зале!*\n" +
                        "Одна отметка в день - этого достаточно! 💪";
            }

            return "❌ *Произошла ошибка при отметке в зале.*\n" +
                    "Пожалуйста, попробуйте позже или обратитесь к администратору.";
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String handleDisplayNameInput(final Long telegramId, final String displayName) {
        log.info("{}_ВВОД_ИМЕНИ_НАЧАЛО: обработка имени '{}' для Telegram ID: {}",
                SERVICE_NAME, displayName, telegramId);

        final String userState = userStates.get(telegramId);

        if ("awaiting_display_name".equals(userState)) {
            try {
                final UserInfoResponse user = userService.getUserByTelegramId(telegramId);

                userService.updateDisplayName(user.getId(), displayName.trim());

                userStates.remove(telegramId);

                final String response = String.format(
                        "✅ *Отлично, %s!*\n\n" +
                                "Теперь я буду обращаться к вам так.\n\n" +
                                "*Доступные команды:*\n" +
                                "• Я в зале - Отметиться в зале\n" +
                                "• Сменить имя - Изменить имя для обращения\n" +
                                "• /help - Показать справку по командам",
                        displayName.trim()
                );

                log.info("{}_ВВОД_ИМЕНИ_УСПЕХ: имя пользователя {} обновлено на '{}'",
                        SERVICE_NAME, telegramId, displayName);

                return response;

            } catch (Exception e) {
                log.error("{}_ВВОД_ИМЕНИ_ОШИБКА: ошибка при обновлении имени для {}: {}",
                        SERVICE_NAME, telegramId, e.getMessage());

                return "❌ *Произошла ошибка при сохранении имени.*\n" +
                        "Пожалуйста, попробуйте еще раз.";
            }
        } else if ("awaiting_new_display_name".equals(userState)) {
            try {
                final UserInfoResponse user = userService.getUserByTelegramId(telegramId);

                userService.updateDisplayName(user.getId(), displayName.trim());

                userStates.remove(telegramId);

                final String response = String.format(
                        "✅ *Имя успешно изменено!*\n\n" +
                                "Теперь я буду обращаться к вам как *%s*.",
                        displayName.trim()
                );

                log.info("{}_СМЕНА_ИМЕНИ_УСПЕХ: имя пользователя {} изменено на '{}'",
                        SERVICE_NAME, telegramId, displayName);

                return response;

            } catch (Exception e) {
                log.error("{}_СМЕНА_ИМЕНИ_ОШИБКА: ошибка при изменении имени для {}: {}",
                        SERVICE_NAME, telegramId, e.getMessage());

                return "❌ *Произошла ошибка при изменении имени.*\n" +
                        "Пожалуйста, попробуйте еще раз.";
            }
        } else {
            log.warn("{}_ВВОД_ИМЕНИ_НЕОЖИДАННО: Telegram ID {} не ожидает ввода имени",
                    SERVICE_NAME, telegramId);
            return handleUnknownCommand(telegramId);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String handleDailyReportCommand(final Long telegramId, final String dateStr) {
        log.info("{}_КОМАНДА_ОТЧЕТ_ЗА_ДЕНЬ_НАЧАЛО: " +
                "администратор {}, дата: {}", SERVICE_NAME, telegramId, dateStr);

        try {
            final UserInfoResponse user = userService.getUserByTelegramId(telegramId);

            if (user.getRole() != ROLE.ADMIN) {
                return "❌ *Доступ запрещен!*\n" +
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
                    return "❌ *Неверный формат даты!*\n" +
                            "Используйте формат: ДД.ММ.ГГГГ\n" +
                            "Пример: /report 06.12.2025\n" +
                            "Или специальные значения: 'сегодня', 'вчера'";
                }
            }

            final VisitorLogResponse report = reportService.generateDailyReportForDate(
                    user.getId(), date);

            adminStates.remove(telegramId);

            log.info("{}_КОМАНДА_ОТЧЕТ_ЗА_ДЕНЬ_УСПЕХ: " +
                            "отчет за {} сгенерирован для администратора {}",
                    SERVICE_NAME, date, telegramId);

            return report.getFormattedReport();

        } catch (Exception e) {
            log.error("{}_КОМАНДА_ОТЧЕТ_ЗА_ДЕНЬ_ОШИБКА: ошибка для {}: {}",
                    SERVICE_NAME, telegramId, e.getMessage());

            return "❌ *Произошла ошибка при генерации отчета.*\n" +
                    "Проверьте формат даты и попробуйте еще раз.";
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String handlePeriodReportCommand(final Long telegramId,
                                            final String startDateStr,
                                            final String endDateStr) {
        log.info("{}_КОМАНДА_ОТЧЕТ_ЗА_ПЕРИОД_НАЧАЛО: " +
                        "администратор {}, период: {} - {}",
                SERVICE_NAME, telegramId, startDateStr, endDateStr);

        try {
            final UserInfoResponse user = userService.getUserByTelegramId(telegramId);

            if (user.getRole() != ROLE.ADMIN) {
                return "❌ *Доступ запрещен!*\n" +
                        "Эта команда доступна только администраторам.";
            }

            final LocalDate startDate;
            final LocalDate endDate;

            try {
                startDate = LocalDate.parse(startDateStr.trim(), INPUT_DATE_FORMATTER);
                endDate = LocalDate.parse(endDateStr.trim(), INPUT_DATE_FORMATTER);
            } catch (DateTimeParseException e) {
                return "❌ *Неверный формат даты!*\n" +
                        "Используйте формат: ДД.ММ.ГГГГ\n" +
                        "Пример: /report period 01.12.2025 06.12.2025";
            }

            if (startDate.isAfter(endDate)) {
                return "❌ *Дата начала не может быть позже даты окончания!*";
            }

            final ReportResponse report = reportService.generatePeriodReport(
                    user.getId(), startDate, endDate);

            adminStates.remove(telegramId);

            log.info("{}_КОМАНДА_ОТЧЕТ_ЗА_ПЕРИОД_УСПЕХ: " +
                            "отчет за период {} - {} сгенерирован",
                    SERVICE_NAME, startDate, endDate);

            return report.getTelegramFormattedReport();

        } catch (Exception e) {
            log.error("{}_КОМАНДА_ОТЧЕТ_ЗА_ПЕРИОД_ОШИБКА: ошибка для {}: {}",
                    SERVICE_NAME, telegramId, e.getMessage());

            return "❌ *Произошла ошибка при генерации отчета.*\n" +
                    "Проверьте формат дат и попробуйте еще раз.";
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String handleTableCommand(final Long telegramId, final String input) {
        log.info("{}_КОМАНДА_ТАБЛИЦА_НАЧАЛО: администратор {}, ввод: {}",
                SERVICE_NAME, telegramId, input);

        try {
            final UserInfoResponse user = userService.getUserByTelegramId(telegramId);

            if (user.getRole() != ROLE.ADMIN) {
                log.warn("{}_КОМАНДА_ТАБЛИЦА_ДОСТУП_ЗАПРЕЩЕН: " +
                        "пользователь {} не является администратором", SERVICE_NAME, telegramId);
                return "❌ *Доступ запрещен!*\nЭта команда доступна только администраторам.";
            }

            if (input == null || input.trim().isEmpty()) {
                log.info("{}_КОМАНДА_ТАБЛИЦА_ПОЛУЧЕНИЕ_ТЕКУЩЕГО_ДНЯ: " +
                        "администратор {}", SERVICE_NAME, telegramId);
                return getTableForToday(user.getId());
            }

            final String[] parts = input.trim().split("\\s+");

            if (parts.length == 1) {
                return getTableForDate(user.getId(), parts[0]);
            } else if (parts.length == 2) {
                return getTableForPeriod(user.getId(), parts[0], parts[1]);
            } else {
                log.warn("{}_КОМАНДА_ТАБЛИЦА_НЕВЕРНЫЙ_ФОРМАТ: " +
                        "неверное количество параметров: {}", SERVICE_NAME, parts.length);
                return tableFormatterService.getTableUsageInstructions();
            }

        } catch (Exception e) {
            log.error("{}_КОМАНДА_ТАБЛИЦА_ОШИБКА: ошибка при обработке команды для {}: {}",
                    SERVICE_NAME, telegramId, e.getMessage(), e);
            return "❌ *Произошла ошибка при получении таблицы.*\n" +
                    "Проверьте формат даты и попробуйте еще раз.";
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String handleAdminMenuCommand(final Long telegramId, final String menuCommand) {
        log.info("{}_КОМАНДА_АДМИН_МЕНЮ_НАЧАЛО: администратор {}, команда меню: {}",
                SERVICE_NAME, telegramId, menuCommand);

        try {
            final UserInfoResponse user = userService.getUserByTelegramId(telegramId);

            if (user.getRole() != ROLE.ADMIN) {
                return "❌ *Доступ запрещен!*\nЭта команда доступна только администраторам.";
            }

            if ("Получить журнал за сегодня".equals(menuCommand)) {
                return handleDailyReportCommand(telegramId, null);
            } else if (menuCommand.startsWith("Получить журнал за день")) {
                String datePart = menuCommand.replace("Получить журнал за день", "").trim();
                if (datePart.isEmpty()) {
                    adminStates.put(telegramId, "awaiting_specific_date");
                    return "📅 *Выберите дату для отчета*\n\n" +
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
                return "📅 *Выберите период для отчета*\n\n" +
                        "Введите *начальную дату* в формате ДД.ММ.ГГГГ\n" +
                        "Пример: 01.12.2025";
            }

            return handleUnknownCommand(telegramId);

        } catch (Exception e) {
            log.error("{}_КОМАНДА_АДМИН_МЕНЮ_ОШИБКА: ошибка для {}: {}",
                    SERVICE_NAME, telegramId, e.getMessage());
            return "❌ *Произошла ошибка при обработке команды меню.*";
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String handleAdminDateInput(final Long telegramId, final String dateInput) {
        log.info("{}_ВВОД_ДАТЫ_АДМИН_НАЧАЛО: администратор {}, ввод: {}",
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
                    return "📅 *Теперь введите конечную дату* в формате ДД.ММ.ГГГГ\n" +
                            "Пример: 06.12.2025";
                } catch (DateTimeParseException e) {
                    return "❌ *Неверный формат даты!*\n" +
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
            log.error("{}_ВВОД_ДАТЫ_АДМИН_ОШИБКА: ошибка для {}: {}",
                    SERVICE_NAME, telegramId, e.getMessage());
            return "❌ *Произошла ошибка при обработке даты.*\n" +
                    "Проверьте формат и попробуйте еще раз.";
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String handleUnknownCommand(final Long telegramId) {
        log.debug("{}_НЕИЗВЕСТНАЯ_КОМАНДА: Telegram ID {}",
                SERVICE_NAME, telegramId);

        final String userState = userStates.get(telegramId);
        final String adminState = adminStates.get(telegramId);

        if ("awaiting_display_name".equals(userState)) {
            return "Пожалуйста, введите имя для обращения. " +
                    "Пример: *Иван* или *Спортсмен123*";
        }

        if (adminState != null) {
            if ("awaiting_specific_date".equals(adminState)) {
                return "📅 *Ожидается ввод даты*\n\n" +
                        "Введите дату в формате ДД.ММ.ГГГГ\n" +
                        "Пример: 06.12.2025\n\n" +
                        "Или используйте специальные значения:\n" +
                        "• сегодня\n" +
                        "• вчера";
            } else if ("awaiting_start_date".equals(adminState)) {
                return "📅 *Ожидается ввод начальной даты*\n\n" +
                        "Введите дату в формате ДД.ММ.ГГГГ\n" +
                        "Пример: 01.12.2025";
            } else if (adminState.startsWith("awaiting_end_date_")) {
                return "📅 *Ожидается ввод конечной даты*\n\n" +
                        "Введите дату в формате ДД.ММ.ГГГГ\n" +
                        "Пример: 06.12.2025";
            }
        }

        try {
            final UserInfoResponse user = userService.getUserByTelegramId(telegramId);

            final String displayName = user.getDisplayName() != null ?
                    user.getDisplayName() : user.getFirstName();

            final StringBuilder response = new StringBuilder();
            response.append(String.format("🤔 *%s, я не понял вашу команду.*\n\n", displayName));
            response.append("*Доступные команды:*\n");
            response.append("• Я в зале - Отметиться в тренажерном зале\n");
            response.append("• Сменить имя - Изменить имя для обращения\n");

            if (user.getRole() == ROLE.ADMIN) {
                response.append("\n*Команды администратора:*\n");
                response.append("• /report - Отчет посещений за сегодня\n");
                response.append("• /report дата - Отчет за определенный день\n");
                response.append("  Пример: /report 06.12.2025\n");
                response.append("• /report period начало конец - Отчет за период\n");
                response.append("  Пример: /report period 01.12.2025 06.12.2025\n");
                response.append("• /table - Таблица посещений за сегодня\n");
                response.append("• /table дата - Таблица за определенный день\n");
                response.append("• /table дата_нач дата_кон - Таблица за период\n");
            }

            response.append("\n*Общие команды:*\n");
            response.append("• /start - Начать работу с ботом\n");
            response.append("• /help - Показать эту справку\n");

            response.append("\nИспользуйте кнопки меню или введите команду вручную.");

            return response.toString();

        } catch (Exception e) {
            return "👋 *Добро пожаловать в тренажерный зал!*\n\n" +
                    "Для начала работы введите команду /start";
        }
    }

    /**
     * Обрабатывает команду смены имени пользователя.
     * Устанавливает состояние ожидания нового имени.
     *
     * @param telegramId Telegram ID пользователя
     * @return запрос на ввод нового имени
     */
    @Override
    public String handleChangeNameCommand(final Long telegramId) {
        log.info("{}_КОМАНДА_СМЕНЫ_ИМЕНИ_НАЧАЛО: пользователь {} хочет сменить имя",
                SERVICE_NAME, telegramId);

        try {
            final UserInfoResponse user = userService.getUserByTelegramId(telegramId);

            userStates.put(telegramId, "awaiting_new_display_name");

            final String response = String.format(
                    "✏️ *%s, вы хотите изменить имя для обращения.*\n\n" +
                            "Пожалуйста, введите новое имя и фамилию.\n" +
                            "Пример: *Сергей Мордвинов*",
                    user.getDisplayName() != null ? user.getDisplayName() : user.getFirstName()
            );

            log.info("{}_КОМАНДА_СМЕНЫ_ИМЕНИ_УСПЕХ: пользователь {} ожидает ввода нового имени",
                    SERVICE_NAME, telegramId);

            return response;

        } catch (Exception e) {
            log.error("{}_КОМАНДА_СМЕНЫ_ИМЕНИ_ОШИБКА: ошибка для {}: {}",
                    SERVICE_NAME, telegramId, e.getMessage());

            return "❌ *Произошла ошибка при запросе смены имени.*\n" +
                    "Пожалуйста, попробуйте позже.";
        }
    }

    /**
     * Возвращает таблицу посещений за текущий день.
     *
     * @param adminUserId идентификатор администратора
     * @return форматированная таблица посещений за сегодня
     */
    private String getTableForToday(final UUID adminUserId) {
        log.info("{}_ТАБЛИЦА_ЗА_ТЕКУЩИЙ_ДЕНЬ_НАЧАЛО: администратор {}",
                SERVICE_NAME, adminUserId);

        final LocalDate today = LocalDate.now();
        final Optional<VisitorLogResponse> existingLog = reportService.getVisitorLogByDate(adminUserId, today);

        return tableFormatterService.formatTableForToday(adminUserId.toString(), existingLog);
    }

    /**
     * Возвращает таблицу посещений за указанную дату.
     *
     * @param adminUserId идентификатор администратора
     * @param dateStr     строка с датой в формате ДД.ММ.ГГГГ
     * @return форматированная таблица посещений за указанную дату
     */
    private String getTableForDate(final UUID adminUserId, final String dateStr) {
        log.info("{}_ТАБЛИЦА_ЗА_ДАТУ_НАЧАЛО: администратор {}, дата: {}",
                SERVICE_NAME, adminUserId, dateStr);

        try {
            final LocalDate date = LocalDate.parse(dateStr.trim(), INPUT_DATE_FORMATTER);
            final Optional<VisitorLogResponse> existingLog = reportService.getVisitorLogByDate(adminUserId, date);

            return tableFormatterService.formatTableForDate(adminUserId.toString(), date, existingLog);
        } catch (DateTimeParseException e) {
            log.warn("{}_ТАБЛИЦА_ЗА_ДАТУ_НЕВЕРНЫЙ_ФОРМАТ: неверный формат даты: {}",
                    SERVICE_NAME, dateStr);
            return "❌ *Неверный формат даты!*\n" +
                    "Используйте формат: ДД.ММ.ГГГГ\n" +
                    "Пример: /report 06.12.2025";
        }
    }

    /**
     * Возвращает таблицу посещений за указанный период.
     *
     * @param adminUserId  идентификатор администратора
     * @param startDateStr строка с начальной датой в формате ДД.ММ.ГГГГ
     * @param endDateStr   строка с конечной датой в формате ДД.ММ.ГГГГ
     * @return форматированная таблица посещений за указанный период
     */
    private String getTableForPeriod(final UUID adminUserId, final String startDateStr, final String endDateStr) {
        log.info("{}_ТАБЛИЦА_ЗА_ПЕРИОД_НАЧАЛО: администратор {}, период: {} - {}",
                SERVICE_NAME, adminUserId, startDateStr, endDateStr);

        try {
            final LocalDate startDate = LocalDate.parse(startDateStr.trim(), INPUT_DATE_FORMATTER);
            final LocalDate endDate = LocalDate.parse(endDateStr.trim(), INPUT_DATE_FORMATTER);

            if (startDate.isAfter(endDate)) {
                log.warn("{}_ТАБЛИЦА_ЗА_ПЕРИОД_НЕВЕРНЫЕ_ДАТЫ: " +
                        "дата начала {} позже даты окончания {}", SERVICE_NAME, startDate, endDate);
                return "❌ *Дата начала не может быть позже даты окончания!*";
            }

            final var logs = reportService.getVisitorLogsByPeriod(adminUserId, startDate, endDate);

            if (logs.isEmpty()) {
                return tableFormatterService.formatPeriodTableEmpty(startDate, endDate);
            }

            return tableFormatterService.formatTableForPeriod(startDate, endDate, logs);

        } catch (DateTimeParseException e) {
            log.warn("{}_ТАБЛИЦА_ЗА_ПЕРИОД_НЕВЕРНЫЙ_ФОРМАТ: неверный формат дат: {} - {}",
                    SERVICE_NAME, startDateStr, endDateStr);
            return "❌ *Неверный формат даты!*\n" +
                    "Используйте формат: ДД.ММ.ГГГГ ДД.ММ.ГГГГ\n" +
                    "Пример: /report period 01.12.2025 06.12.2025";
        }
    }
}