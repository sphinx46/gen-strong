package ru.cs.vsu.social_network.telegram_bot.bot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.cs.vsu.social_network.telegram_bot.config.BotConfig;
import ru.cs.vsu.social_network.telegram_bot.dto.response.UserInfoResponse;
import ru.cs.vsu.social_network.telegram_bot.entity.enums.ROLE;
import ru.cs.vsu.social_network.telegram_bot.service.TelegramCommandService;
import ru.cs.vsu.social_network.telegram_bot.service.UserService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class GymTelegramBot extends TelegramLongPollingBot {

    private static final String BOT_NAME = "GYM_TELEGRAM_BOT";
    private static final DateTimeFormatter INPUT_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final BotConfig botConfig;
    private final TelegramCommandService telegramCommandService;
    private final UserService userService;

    public GymTelegramBot(final DefaultBotOptions botOptions,
                          final BotConfig botConfig,
                          final TelegramCommandService telegramCommandService,
                          final UserService userService) {
        super(botOptions, botConfig.getBotToken());
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
            final String response = processMessage(telegramId, text, message);
            sendResponse(chatId, response, telegramId);

        } catch (Exception e) {
            log.error("{}_ОБРАБОТКА_ОШИБКА: ошибка при обработке сообщения от {}: {}",
                    BOT_NAME, telegramId, e.getMessage(), e);
            sendErrorResponse(chatId, telegramId);
        }
    }

    private String processMessage(final Long telegramId,
                                  final String text,
                                  final Message message) {

        log.debug("{}_ПРОЦЕСС_СООБЩЕНИЯ_НАЧАЛО: текст '{}', telegramId {}", BOT_NAME, text, telegramId);

        if (text.startsWith("/")) {
            return processCommand(telegramId, text, message);
        }

        if ("Я в зале".equalsIgnoreCase(text)) {
            return telegramCommandService.handleInGymCommand(telegramId);
        } else if ("Сменить имя".equalsIgnoreCase(text)) {
            return telegramCommandService.handleChangeNameCommand(telegramId);
        } else if (text.toLowerCase().contains("составить программу")) {
            return telegramCommandService.handleTrainingProgramCommand(telegramId, text);
        } else if ("Внести вклад в развитие".equalsIgnoreCase(text)) {
            return telegramCommandService.handleContributionCommand(telegramId);
        } else if (text.startsWith("Получить журнал")) {
            return telegramCommandService.handleAdminMenuCommand(telegramId, text);
        }

        try {
            String processedText = text.trim();
            String userState = telegramCommandService.getUserState(telegramId);

            log.debug("{}_ТЕКУЩЕЕ_СОСТОЯНИЕ: пользователь {}, состояние: {}",
                    BOT_NAME, telegramId, userState);

            if (isDateInput(processedText)) {
                return telegramCommandService.handleAdminDateInput(telegramId, processedText);
            }

            if (userState != null && userState.startsWith("awaiting")) {
                return telegramCommandService.handleTrainingProgramCommand(telegramId, processedText);
            }

            if ("awaiting_display_name".equals(userState)) {
                log.debug("{}_ИМЯ_ВВОД_ОБНАРУЖЕН: состояние корректно, обработка '{}'", BOT_NAME, processedText);
                return telegramCommandService.handleDisplayNameInput(telegramId, processedText);
            }

            log.debug("{}_СООБЩЕНИЕ_НЕ_ОБРАБОТАНО: текст '{}' не соответствует текущему состоянию '{}'",
                    BOT_NAME, processedText, userState);
            return telegramCommandService.handleUnknownCommand(telegramId);

        } catch (Exception e) {
            log.error("{}_ОБРАБОТКА_ОШИБКА_ДЕТАЛЬ: ошибка при обработке сообщения '{}' от {}: {}",
                    BOT_NAME, text, telegramId, e.getMessage(), e);
            return telegramCommandService.handleUnknownCommand(telegramId);
        }
    }

    private boolean isDateInput(String text) {
        try {
            String trimmed = text.trim().toLowerCase();

            if ("сегодня".equals(trimmed) || "вчера".equals(trimmed)) {
                return true;
            }

            LocalDate.parse(text.trim(), INPUT_DATE_FORMATTER);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    private String processCommand(final Long telegramId,
                                  final String commandText,
                                  final Message message) {

        log.debug("{}_КОМАНДА_ОБРАБОТКА_НАЧАЛО: текст команды '{}' от пользователя {}",
                BOT_NAME, commandText, telegramId);

        final String command = commandText.split("\\s+", 2)[0].toLowerCase();

        log.debug("{}_КОМАНДА_ОБРАБОТКА: команда '{}' от пользователя {}",
                BOT_NAME, command, telegramId);

        return switch (command) {
            case "/start" -> handleStartCommand(telegramId, message);
            case "/help" -> telegramCommandService.handleHelpCommand(telegramId);
            default -> {
                log.warn("{}_КОМАНДА_НЕИЗВЕСТНАЯ: неизвестная команда '{}' от {}",
                        BOT_NAME, command, telegramId);
                yield telegramCommandService.handleUnknownCommand(telegramId);
            }
        };
    }

    private String handleStartCommand(final Long telegramId, final Message message) {
        final String username = message.getFrom().getUserName();
        final String firstName = message.getFrom().getFirstName();
        final String lastName = message.getFrom().getLastName();

        log.info("{}_КОМАНДА_START: обработка /start для пользователя {}", BOT_NAME, telegramId);

        return telegramCommandService.handleStartCommand(telegramId, username, firstName, lastName);
    }

    private void sendResponse(final Long chatId, final String responseText, final Long telegramId) {
        try {
            final SendMessage message = new SendMessage();
            message.setChatId(chatId.toString());
            message.setText(responseText);

            message.setReplyMarkup(createMainMenuKeyboard(telegramId));

            execute(message);

            log.debug("{}_ОТВЕТ_ОТПРАВЛЕН: ответ пользователю {} отправлен",
                    BOT_NAME, telegramId);

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

        final KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("Внести вклад в развитие"));
        keyboard.add(row2);

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

    @Override
    public void onRegister() {
        super.onRegister();
        log.info("{}_РЕГИСТРАЦИЯ_УСПЕХ: бот {} успешно зарегистрирован в Telegram API",
                BOT_NAME, getBotUsername());
    }
}