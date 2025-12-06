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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Основной обработчик Telegram бота для тренажерного зала.
 * Получает обновления от Telegram и делегирует обработку сервису команд.
 */
@Slf4j
@Component
public class GymTelegramBot extends TelegramLongPollingBot {

    private static final String BOT_NAME = "GYM_TELEGRAM_BOT";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final BotConfig botConfig;
    private final TelegramCommandService telegramCommandService;

    /**
     * Конструктор основного обработчика бота.
     *
     * @param botOptions опции бота
     * @param botConfig конфигурация бота
     * @param telegramCommandService сервис для обработки команд
     */
    public GymTelegramBot(final DefaultBotOptions botOptions,
                          final BotConfig botConfig,
                          final TelegramCommandService telegramCommandService) {
        super(botOptions);
        this.botConfig = botConfig;
        this.telegramCommandService = telegramCommandService;

        log.info("{}_ИНИЦИАЛИЗАЦИЯ_НАЧАЛО: создание бота {}", BOT_NAME, botConfig.getBotUsername());
    }

    /**
     * Получает username бота.
     *
     * @return username бота из конфигурации
     */
    @Override
    public String getBotUsername() {
        return botConfig.getBotUsername();
    }

    /**
     * Получает токен бота.
     *
     * @return токен бота из конфигурации
     */
    @Override
    public String getBotToken() {
        return botConfig.getBotToken();
    }

    /**
     * Обрабатывает входящие обновления от Telegram.
     * Основной метод, который вызывается при получении нового сообщения.
     *
     * @param update объект обновления от Telegram API
     */
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

    /**
     * Обрабатывает входящее текстовое сообщение и определяет его тип.
     *
     * @param telegramId Telegram ID пользователя
     * @param chatId ID чата
     * @param text текст сообщения
     * @param message исходное сообщение
     * @return текст ответа для пользователя
     */
    private String processMessage(final Long telegramId,
                                  final Long chatId,
                                  final String text,
                                  final Message message) {

        if (text.startsWith("/")) {
            return processCommand(telegramId, text, message);
        } else if ("Я в зале".equalsIgnoreCase(text)) {
            return telegramCommandService.handleInGymCommand(telegramId);
        } else {
            return telegramCommandService.handleDisplayNameInput(telegramId, text);
        }
    }

    /**
     * Обрабатывает команды, начинающиеся с "/".
     * Разбирает команду и передает управление соответствующему обработчику.
     *
     * @param telegramId Telegram ID пользователя
     * @param commandText текст команды
     * @param message исходное сообщение
     * @return текст ответа
     */
    private String processCommand(final Long telegramId,
                                  final String commandText,
                                  final Message message) {

        final String[] parts;
        if (commandText.startsWith("/report") && commandText.contains("(сегодня)")) {
            parts = new String[] { "/report", null };
        } else {
            parts = commandText.split("\\s+", 3);
        }

        final String command = parts[0].toLowerCase();

        log.debug("{}_КОМАНДА_ОБРАБОТКА: команда '{}' от пользователя {}",
                BOT_NAME, command, telegramId);

        switch (command) {
            case "/start":
                return handleStartCommand(telegramId, message);

            case "/report":
                return handleReportCommand(telegramId, parts);

            case "/report period":
                return handleReportPeriodCommand(telegramId, parts);

            case "/table":
                return handleTableCommand(telegramId, parts);

            case "/help":
                return telegramCommandService.handleUnknownCommand(telegramId);

            default:
                log.warn("{}_КОМАНДА_НЕИЗВЕСТНАЯ: неизвестная команда '{}' от {}",
                        BOT_NAME, command, telegramId);
                return telegramCommandService.handleUnknownCommand(telegramId);
        }
    }

    /**
     * Обрабатывает команду /start.
     * Регистрирует нового пользователя или приветствует существующего.
     *
     * @param telegramId Telegram ID пользователя
     * @param message исходное сообщение
     * @return текст приветствия
     */
    private String handleStartCommand(final Long telegramId, final Message message) {
        final String username = message.getFrom().getUserName();
        final String firstName = message.getFrom().getFirstName();
        final String lastName = message.getFrom().getLastName();

        log.info("{}_КОМАНДА_START: обработка /start для пользователя {}", BOT_NAME, telegramId);

        return telegramCommandService.handleStartCommand(telegramId, username, firstName, lastName);
    }

    /**
     * Обрабатывает команду /report.
     * Поддерживает три варианта:
     * 1. /report (сегодня) - отчет за текущий день
     * 2. /report - отчет за текущий день
     * 3. /report ДД.ММ.ГГГГ - отчет за указанный день
     *
     * @param telegramId Telegram ID пользователя
     * @param parts части команды
     * @return сформированный отчет
     */
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

    /**
     * Обрабатывает команду /report period.
     * Генерирует отчет за указанный период времени.
     * Формат команды: /report period ДД.ММ.ГГГГ ДД.ММ.ГГГГ
     *
     * @param telegramId Telegram ID пользователя
     * @param parts части команды: [0]="/report period", [1]=начальная дата, [2]=конечная дата
     * @return отчет за период или сообщение об ошибке
     */
    private String handleReportPeriodCommand(final Long telegramId, final String[] parts) {
        if (parts.length < 3) {
            log.warn("{}_КОМАНДА_REPORT_PERIOD_НЕПОЛНАЯ: недостаточно параметров от {}",
                    BOT_NAME, telegramId);
            return "❌ *Неверный формат команды!*\n" +
                    "Используйте: /report period ДД.ММ.ГГГГ ДД.ММ.ГГГГ\n" +
                    "Пример: /report period 01.12.2025 06.12.2025";
        }

        final String startDateStr = parts[1];
        final String endDateStr = parts[2];

        log.debug("{}_КОМАНДА_REPORT_PERIOD: отчет за период {} - {} от {}",
                BOT_NAME, startDateStr, endDateStr, telegramId);

        return telegramCommandService.handlePeriodReportCommand(telegramId, startDateStr, endDateStr);
    }

    /**
     * Обрабатывает команду /table.
     * Позволяет получить таблицу посещений в различных форматах:
     * 1. /table - за текущий день
     * 2. /table ДД.ММ.ГГГГ - за указанный день
     * 3. /table ДД.ММ.ГГГГ ДД.ММ.ГГГГ - за указанный период
     *
     * @param telegramId Telegram ID пользователя
     * @param parts части команды
     * @return форматированная таблица посещений
     */
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

    /**
     * Отправляет ответ пользователю в Telegram.
     * Форматирует сообщение, добавляет клавиатуру при необходимости.
     *
     * @param chatId ID чата
     * @param responseText текст ответа
     * @param telegramId Telegram ID пользователя
     */
    private void sendResponse(final Long chatId, final String responseText, final Long telegramId) {
        try {
            final SendMessage message = new SendMessage();
            message.setChatId(chatId.toString());
            message.setText(responseText);
            message.enableMarkdown(true);
            message.setParseMode("Markdown");

            if (shouldAddMenu(telegramId, responseText)) {
                message.setReplyMarkup(createMainMenuKeyboard(telegramId));
            }

            final Message sentMessage = execute(message);

            log.debug("{}_ОТВЕТ_ОТПРАВЛЕН: ответ пользователю {} отправлен, messageId: {}",
                    BOT_NAME, telegramId, sentMessage.getMessageId());

        } catch (TelegramApiException e) {
            log.error("{}_ОТВЕТ_ОШИБКА: не удалось отправить ответ пользователю {}: {}",
                    BOT_NAME, telegramId, e.getMessage(), e);
        }
    }

    /**
     * Определяет, нужно ли добавлять меню к ответу.
     * Меню не добавляется при определенных типах сообщений.
     *
     * @param telegramId Telegram ID пользователя
     * @param responseText текст ответа
     * @return true, если нужно добавить меню
     */
    private boolean shouldAddMenu(final Long telegramId, final String responseText) {
        return !responseText.contains("Как мне к вам обращаться") &&
                !responseText.contains("Пожалуйста, введите имя") &&
                !responseText.startsWith("❌");
    }

    /**
     * Создает основное меню с кнопками.
     * Для администраторов добавляются дополнительные кнопки отчетов.
     *
     * @param telegramId Telegram ID пользователя для определения роли
     * @return клавиатура с меню
     */
    private ReplyKeyboardMarkup createMainMenuKeyboard(final Long telegramId) {
        final ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setSelective(true);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);

        final List<KeyboardRow> keyboard = new ArrayList<>();

        final KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("Я в зале"));
        keyboard.add(row1);

        try {
            final String response = telegramCommandService.handleUnknownCommand(telegramId);
            if (response.contains("Команды администратора")) {
                final KeyboardRow adminRow1 = new KeyboardRow();
                adminRow1.add(new KeyboardButton("/report (сегодня)"));
                keyboard.add(adminRow1);

                final KeyboardRow adminRow2 = new KeyboardRow();
                adminRow2.add(new KeyboardButton("/report " + getCurrentDateFormatted()));
                keyboard.add(adminRow2);

                final KeyboardRow adminRow3 = new KeyboardRow();
                adminRow3.add(new KeyboardButton("/report period"));
                keyboard.add(adminRow3);

                final KeyboardRow helpRow = new KeyboardRow();
                helpRow.add(new KeyboardButton("/help"));
                keyboard.add(helpRow);
            } else {
                final KeyboardRow helpRow = new KeyboardRow();
                helpRow.add(new KeyboardButton("/help"));
                keyboard.add(helpRow);
            }
        } catch (Exception e) {
            log.warn("{}_МЕНЮ_ОШИБКА: не удалось определить роль пользователя {}: {}",
                    BOT_NAME, telegramId, e.getMessage());

            final KeyboardRow helpRow = new KeyboardRow();
            helpRow.add(new KeyboardButton("/help"));
            keyboard.add(helpRow);
        }

        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }

    /**
     * Получает текущую дату в формате ДД.ММ.ГГГГ.
     *
     * @return текущая дата в формате ДД.ММ.ГГГГ
     */
    private String getCurrentDateFormatted() {
        return LocalDate.now().format(DATE_FORMATTER);
    }

    /**
     * Отправляет сообщение об ошибке пользователю.
     * Используется при возникновении исключений в процессе обработки.
     *
     * @param chatId ID чата
     * @param telegramId Telegram ID пользователя
     */
    private void sendErrorResponse(final Long chatId, final Long telegramId) {
        try {
            final SendMessage message = new SendMessage();
            message.setChatId(chatId.toString());
            message.setText("❌ *Произошла внутренняя ошибка.*\n" +
                    "Пожалуйста, попробуйте позже или обратитесь к администратору.");
            message.enableMarkdown(true);
            message.setParseMode("Markdown");

            execute(message);

            log.warn("{}_ОШИБКА_ОТПРАВЛЕНА: сообщение об ошибке отправлено пользователю {}",
                    BOT_NAME, telegramId);

        } catch (TelegramApiException e) {
            log.error("{}_ОШИБКА_ОТПРАВКИ_ОШИБКИ: не удалось отправить сообщение об ошибке пользователю {}: {}",
                    BOT_NAME, telegramId, e.getMessage());
        }
    }

    /**
     * Удаляет сообщение из чата.
     * Может использоваться для удаления устаревших или конфиденциальных сообщений.
     *
     * @param chatId ID чата
     * @param messageId ID сообщения
     * @param telegramId Telegram ID пользователя
     */
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

    /**
     * Получает имя бота.
     *
     * @return имя бота из конфигурации
     */
    public String getBotName() {
        return botConfig.getBotName();
    }

    /**
     * Вызывается при успешной регистрации бота в Telegram API.
     */
    @Override
    public void onRegister() {
        super.onRegister();
        log.info("{}_РЕГИСТРАЦИЯ_УСПЕХ: бот {} успешно зарегистрирован в Telegram API",
                BOT_NAME, getBotUsername());
    }
}