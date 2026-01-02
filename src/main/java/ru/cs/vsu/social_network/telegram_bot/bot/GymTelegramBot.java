package ru.cs.vsu.social_network.telegram_bot.bot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.cs.vsu.social_network.telegram_bot.config.BotConfig;
import ru.cs.vsu.social_network.telegram_bot.service.TelegramCommandService;
import ru.cs.vsu.social_network.telegram_bot.entity.enums.ROLE;
import ru.cs.vsu.social_network.telegram_bot.dto.response.UserInfoResponse;
import ru.cs.vsu.social_network.telegram_bot.service.UserService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Component
public class GymTelegramBot extends TelegramLongPollingBot {

    private static final String BOT_NAME = "GYM_TELEGRAM_BOT";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter INPUT_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final Pattern BENCH_PRESS_PATTERN = Pattern.compile("^\\d+(?:\\.\\d{1,2})?$");

    private final BotConfig botConfig;
    private final TelegramCommandService telegramCommandService;
    private final UserService userService;

    public GymTelegramBot(final DefaultBotOptions botOptions,
                          final BotConfig botConfig,
                          final TelegramCommandService telegramCommandService,
                          final UserService userService) {
        super(botOptions);
        this.botConfig = botConfig;
        this.telegramCommandService = telegramCommandService;
        this.userService = userService;

        log.info("{}_ИНИЦИАЛИЗАЦИЯ_НАЧАЛО: создание бота {}", BOT_NAME, botConfig.getBotUsername());
    }

    @Override
    public String getBotUsername() {
        return botConfig.getBotUsername();
    }

    @Override
    public String getBotToken() {
        return botConfig.getBotToken();
    }

    @Override
    public void onUpdateReceived(final Update update) {
        log.debug("{}_ОБНОВЛЕНИЕ_ПОЛУЧЕНО: получено новое обновление", BOT_NAME);

        if (!update.hasMessage() || !update.getMessage().hasText()) {
            log.debug("{}_ОБНОВЛЕНИЕ_ПРОПУЩЕНО: обновление не содержит текстового сообщения", BOT_NAME);
            return;
        }

        final Message message = update.getMessage();
        final Long chatId = message.getChatId();
        final Long telegramId = message.getFrom().getId();
        final String text = message.getText().trim();

        log.info("{}_СООБЩЕНИЕ_ПОЛУЧЕНО: от пользователя {} (chatId: {}), текст: '{}'",
                BOT_NAME, telegramId, chatId, text);

        try {
            final String response = processMessage(telegramId, chatId, text, message);
            sendResponse(chatId, response, telegramId);

        } catch (Exception e) {
            log.error("{}_ОБРАБОТКА_ОШИБКА: ошибка при обработке сообщения от {}: {}",
                    BOT_NAME, telegramId, e.getMessage(), e);
            sendErrorResponse(chatId, telegramId);
        }
    }

    private String processMessage(final Long telegramId,
                                  final Long chatId,
                                  final String text,
                                  final Message message) {

        if (text.startsWith("/")) {
            return processCommand(telegramId, text, message);
        } else if ("Я в зале".equalsIgnoreCase(text)) {
            return telegramCommandService.handleInGymCommand(telegramId);
        } else if ("Сменить имя".equalsIgnoreCase(text)) {
            return telegramCommandService.handleChangeNameCommand(telegramId);
        } else if ("Составить программу тренировок".equalsIgnoreCase(text)) {
            return telegramCommandService.handleTrainingProgramCommand(telegramId);
        } else if (text.startsWith("Получить журнал")) {
            return telegramCommandService.handleAdminMenuCommand(telegramId, text);
        } else if (isDateInput(text)) {
            return telegramCommandService.handleAdminDateInput(telegramId, text);
        } else if (isBenchPressInput(text)) {
            return telegramCommandService.handleBenchPressInput(telegramId, text);
        } else if (isFormatSelection(text)) {
            return telegramCommandService.handleFormatSelection(telegramId, text);
        } else {
            return telegramCommandService.handleDisplayNameInput(telegramId, text);
        }
    }

    private boolean isDateInput(String text) {
        try {
            if ("сегодня".equalsIgnoreCase(text.trim()) ||
                    "вчера".equalsIgnoreCase(text.trim())) {
                return true;
            }
            LocalDate.parse(text.trim(), INPUT_DATE_FORMATTER);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    private boolean isBenchPressInput(String text) {
        return BENCH_PRESS_PATTERN.matcher(text.trim()).matches();
    }

    private boolean isFormatSelection(String text) {
        String lowerText = text.trim().toLowerCase();
        return "изображение".equals(lowerText) ||
                "картинка".equals(lowerText) ||
                "excel".equals(lowerText) ||
                "таблица".equals(lowerText) ||
                "1".equals(text.trim()) ||
                "2".equals(text.trim());
    }

    private String processCommand(final Long telegramId,
                                  final String commandText,
                                  final Message message) {

        log.debug("{}_КОМАНДА_ОБРАБОТКА_НАЧАЛО: текст команды '{}' от пользователя {}",
                BOT_NAME, commandText, telegramId);

        if (commandText.startsWith("/report period")) {
            log.debug("{}_КОМАНДА_ОБРАБОТКА: команда /report period от пользователя {}",
                    BOT_NAME, telegramId);

            String periodParams = commandText.substring("/report period".length()).trim();
            String[] dateParts = periodParams.split("\\s+");

            String[] parts = new String[3];
            parts[0] = "/report period";

            if (dateParts.length >= 2) {
                parts[1] = dateParts[0];
                parts[2] = dateParts[1];
            } else if (dateParts.length == 1) {
                parts[1] = dateParts[0];
            }

            return handleReportPeriodCommand(telegramId, parts);
        }

        final String[] parts;
        if (commandText.startsWith("/report") && commandText.contains("(сегодня)")) {
            parts = new String[] { "/report", null };
        } else {
            parts = commandText.split("\\s+", 3);
        }

        final String command = parts[0].toLowerCase();

        log.debug("{}_КОМАНДА_ОБРАБОТКА: команда '{}' от пользователя {}, части: {}",
                BOT_NAME, command, telegramId, Arrays.toString(parts));

        switch (command) {
            case "/start":
                return handleStartCommand(telegramId, message);

            case "/report":
                return handleReportCommand(telegramId, parts);

            case "/table":
                return handleTableCommand(telegramId, parts);

            case "/help":
                return telegramCommandService.handleHelpCommand(telegramId);

            default:
                log.warn("{}_КОМАНДА_НЕИЗВЕСТНАЯ: неизвестная команда '{}' от {}",
                        BOT_NAME, command, telegramId);
                return telegramCommandService.handleUnknownCommand(telegramId);
        }
    }

    private String handleStartCommand(final Long telegramId, final Message message) {
        final String username = message.getFrom().getUserName();
        final String firstName = message.getFrom().getFirstName();
        final String lastName = message.getFrom().getLastName();

        log.info("{}_КОМАНДА_START: обработка /start для пользователя {}", BOT_NAME, telegramId);

        return telegramCommandService.handleStartCommand(telegramId, username, firstName, lastName);
    }

    private String handleReportCommand(final Long telegramId, final String[] parts) {
        String dateStr = null;

        if (parts.length > 1 && parts[1] != null) {
            final String param = parts[1];
            if (param.matches("\\d{2}\\.\\d{2}\\.\\d{4}")) {
                dateStr = param;
            }
        }

        log.debug("{}_КОМАНДА_REPORT: отчет за дату '{}' от {}",
                BOT_NAME, dateStr != null ? dateStr : "сегодня", telegramId);

        return telegramCommandService.handleDailyReportCommand(telegramId, dateStr);
    }

    private String handleReportPeriodCommand(final Long telegramId, final String[] parts) {
        if (parts.length < 3) {
            log.warn("{}_КОМАНДА_REPORT_PERIOD_НЕПОЛНАЯ: недостаточно параметров от {}",
                    BOT_NAME, telegramId);
            return "Неверный формат команды!\n" +
                    "Используйте: /report period ДД.ММ.ГГГГ ДД.ММ.ГГГГ\n" +
                    "Пример: /report period 01.12.2025 06.12.2025";
        }

        final String startDateStr = parts[1];
        final String endDateStr = parts[2];

        log.debug("{}_КОМАНДА_REPORT_PERIOD: отчет за период {} - {} от {}",
                BOT_NAME, startDateStr, endDateStr, telegramId);

        return telegramCommandService.handlePeriodReportCommand(telegramId, startDateStr, endDateStr);
    }

    private String handleTableCommand(final Long telegramId, final String[] parts) {
        String input = null;
        if (parts.length > 1) {
            final StringBuilder sb = new StringBuilder();
            for (int i = 1; i < parts.length; i++) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(parts[i]);
            }
            input = sb.toString();
        }

        log.debug("{}_КОМАНДА_TABLE: таблица посещений от пользователя {}, параметры: '{}'",
                BOT_NAME, telegramId, input != null ? input : "без параметров");

        return telegramCommandService.handleTableCommand(telegramId, input);
    }

    private void sendResponse(final Long chatId, final String responseText, final Long telegramId) {
        try {
            final SendMessage message = new SendMessage();
            message.setChatId(chatId.toString());
            message.setText(responseText);

            message.setReplyMarkup(createMainMenuKeyboard(telegramId));

            final Message sentMessage = execute(message);

            log.debug("{}_ОТВЕТ_ОТПРАВЛЕН: ответ пользователю {} отправлен, messageId: {}",
                    BOT_NAME, telegramId, sentMessage.getMessageId());

        } catch (TelegramApiException e) {
            log.error("{}_ОТВЕТ_ОШИБКА: не удалось отправить ответ пользователю {}: {}",
                    BOT_NAME, telegramId, e.getMessage(), e);
        }
    }

    private ReplyKeyboardMarkup createMainMenuKeyboard(final Long telegramId) {
        final ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setSelective(true);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);

        final List<KeyboardRow> keyboard = new ArrayList<>();

        final KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("Я в зале"));
        row1.add(new KeyboardButton("Сменить имя"));
        row1.add(new KeyboardButton("Составить программу тренировок"));
        keyboard.add(row1);

        try {
            UserInfoResponse user = userService.getUserByTelegramId(telegramId);

            if (user.getRole() == ROLE.ADMIN) {
                final KeyboardRow adminRow1 = new KeyboardRow();
                adminRow1.add(new KeyboardButton("Получить журнал за сегодня"));
                keyboard.add(adminRow1);

                final KeyboardRow adminRow2 = new KeyboardRow();
                adminRow2.add(new KeyboardButton("Получить журнал за день"));
                keyboard.add(adminRow2);

                final KeyboardRow adminRow3 = new KeyboardRow();
                adminRow3.add(new KeyboardButton("Получить журнал за период"));
                keyboard.add(adminRow3);
            }

        } catch (Exception e) {
            log.warn("{}_МЕНЮ_ОШИБКА: не удалось определить роль пользователя {}: {}",
                    BOT_NAME, telegramId, e.getMessage());
        }

        final KeyboardRow helpRow = new KeyboardRow();
        helpRow.add(new KeyboardButton("/help"));
        keyboard.add(helpRow);

        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }

    private String getCurrentDateFormatted() {
        return LocalDate.now().format(DATE_FORMATTER);
    }

    private void sendErrorResponse(final Long chatId, final Long telegramId) {
        try {
            final SendMessage message = new SendMessage();
            message.setChatId(chatId.toString());
            message.setText("Произошла внутренняя ошибка.\n" +
                    "Пожалуйста, попробуйте позже или обратитесь к администратору.");

            execute(message);

            log.warn("{}_ОШИБКА_ОТПРАВЛЕНА: сообщение об ошибке отправлено пользователю {}",
                    BOT_NAME, telegramId);

        } catch (TelegramApiException e) {
            log.error("{}_ОШИБКА_ОТПРАВКИ_ОШИБКИ: не удалось отправить сообщение об ошибке пользователю {}: {}",
                    BOT_NAME, telegramId, e.getMessage());
        }
    }

    private void deleteMessage(final Long chatId, final Integer messageId, final Long telegramId) {
        try {
            final DeleteMessage deleteMessage = new DeleteMessage();
            deleteMessage.setChatId(chatId.toString());
            deleteMessage.setMessageId(messageId);

            execute(deleteMessage);

            log.debug("{}_СООБЩЕНИЕ_УДАЛЕНО: сообщение {} удалено из чата {}",
                    BOT_NAME, messageId, chatId);

        } catch (TelegramApiException e) {
            log.warn("{}_УДАЛЕНИЕ_ОШИБКА: не удалось удалить сообщение {}: {}",
                    BOT_NAME, messageId, e.getMessage());
        }
    }

    public String getBotName() {
        return botConfig.getBotName();
    }

    @Override
    public void onRegister() {
        super.onRegister();
        log.info("{}_РЕГИСТРАЦИЯ_УСПЕХ: бот {} успешно зарегистрирован в Telegram API",
                BOT_NAME, getBotUsername());
    }
}