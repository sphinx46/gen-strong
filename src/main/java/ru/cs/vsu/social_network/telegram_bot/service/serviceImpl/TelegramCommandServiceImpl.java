package ru.cs.vsu.social_network.telegram_bot.service.serviceImpl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.cs.vsu.social_network.telegram_bot.dto.request.UserCreateRequest;
import ru.cs.vsu.social_network.telegram_bot.dto.response.ReportResponse;
import ru.cs.vsu.social_network.telegram_bot.dto.response.UserInfoResponse;
import ru.cs.vsu.social_network.telegram_bot.dto.response.VisitResponse;
import ru.cs.vsu.social_network.telegram_bot.dto.response.VisitorLogResponse;
import ru.cs.vsu.social_network.telegram_bot.entity.enums.ROLE;
import ru.cs.vsu.social_network.telegram_bot.provider.UserEntityProvider;
import ru.cs.vsu.social_network.telegram_bot.service.ReportService;
import ru.cs.vsu.social_network.telegram_bot.service.TelegramCommandService;
import ru.cs.vsu.social_network.telegram_bot.service.UserService;
import ru.cs.vsu.social_network.telegram_bot.service.VisitService;
import ru.cs.vsu.social_network.telegram_bot.utils.MessageConstants;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

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

    private static final DateTimeFormatter INPUT_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final Map<Long, String> userStates = new HashMap<>();

    public TelegramCommandServiceImpl(final UserService userService,
                                      final VisitService visitService,
                                      final ReportService reportService) {
        this.userService = userService;
        this.visitService = visitService;
        this.reportService = reportService;
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
                        "Как мне к вам обращаться? (Введите ваше имя или никнейм)",
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
                            "Время: %s\n" +
                            "Журнал за сегодня будет сформирован администратором.",
                    user.getDisplayName() != null ? user.getDisplayName() : user.getFirstName(),
                    visit.getCreatedAt().format(DateTimeFormatter.ofPattern("HH:mm"))
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

        if (!"awaiting_display_name".equals(userStates.get(telegramId))) {
            log.warn("{}_ВВОД_ИМЕНИ_НЕОЖИДАННО: Telegram ID {} не ожидает ввода имени",
                    SERVICE_NAME, telegramId);
            return handleUnknownCommand(telegramId);
        }

        try {
            final UserInfoResponse user = userService.getUserByTelegramId(telegramId);

            userService.updateDisplayName(user.getId(), displayName.trim());

            userStates.remove(telegramId);

            final String response = String.format(
                    "✅ *Отлично, %s!*\n\n" +
                            "Теперь я буду обращаться к вам так.\n\n" +
                            "*Доступные команды:*\n" +
                            "• /start - Начать работу с ботом\n" +
                            "• Я в зале - Отметиться в зале\n" +
                            "• /report - Получить отчет (только для администраторов)",
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
                    date = LocalDate.parse(dateStr.trim(), INPUT_DATE_FORMATTER);
                } catch (DateTimeParseException e) {
                    return "❌ *Неверный формат даты!*\n" +
                            "Используйте формат: ДД.ММ.ГГГГ\n" +
                            "Пример: 06.12.2025";
                }
            }

            final VisitorLogResponse report = reportService.generateDailyReportForDate(
                    user.getId(), date);

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
                        "Пример: /report_period 01.12.2025 06.12.2025";
            }

            if (startDate.isAfter(endDate)) {
                return "❌ *Дата начала не может быть позже даты окончания!*";
            }

            final ReportResponse report = reportService.generatePeriodReport(
                    user.getId(), startDate, endDate);

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
    public String handleUnknownCommand(final Long telegramId) {
        log.debug("{}_НЕИЗВЕСТНАЯ_КОМАНДА: Telegram ID {}",
                SERVICE_NAME, telegramId);

        final String userState = userStates.get(telegramId);

        if ("awaiting_display_name".equals(userState)) {
            return "Пожалуйста, введите имя для обращения. " +
                    "Пример: *Иван* или *Спортсмен123*";
        }

        try {
            final UserInfoResponse user = userService.getUserByTelegramId(telegramId);

            final String displayName = user.getDisplayName() != null ?
                    user.getDisplayName() : user.getFirstName();

            final StringBuilder response = new StringBuilder();
            response.append(String.format("🤔 *%s, я не понял вашу команду.*\n\n", displayName));
            response.append("*Доступные команды:*\n");
            response.append("• /start - Начать работу с ботом\n");
            response.append("• Я в зале - Отметиться в тренажерном зале\n");

            if (user.getRole() == ROLE.ADMIN) {
                response.append("\n*Команды администратора:*\n");
                response.append("• /report [дата] - Отчет за день\n");
                response.append("• /report_period [начало] [конец] - Отчет за период\n");
                response.append("• /users - Список пользователей\n");
            }

            response.append("\nИспользуйте кнопки меню или введите команду вручную.");

            return response.toString();

        } catch (Exception e) {
            return "👋 *Добро пожаловать в тренажерный зал!*\n\n" +
                    "Для начала работы введите команду /start";
        }
    }
}