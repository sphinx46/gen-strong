package ru.cs.vsu.social_network.telegram_bot.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.cs.vsu.social_network.telegram_bot.bot.GymTelegramBot;
import ru.cs.vsu.social_network.telegram_bot.service.TelegramCommandService;
import ru.cs.vsu.social_network.telegram_bot.service.UserService;

/**
 * Конфигурация Telegram бота.
 * Настраивает параметры бота и создает необходимые бины.
 */
@Slf4j
@Configuration
public class BotConfig {

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.bot.username}")
    private String botUsername;

    @Value("${telegram.bot.name}")
    private String botName;

    /**
     * Создает экземпляр Telegram бота.
     *
     * @param telegramCommandService сервис обработки команд
     * @param userService сервис пользователей
     * @return экземпляр бота
     */
    @Bean
    public GymTelegramBot gymTelegramBot(final TelegramCommandService telegramCommandService,
                                         final UserService userService) {
        log.info("БОТ_КОНФИГ_СОЗДАНИЕ_БОТА: создание GymTelegramBot с username: {}", botUsername);

        final GymTelegramBot bot = new GymTelegramBot(this, telegramCommandService, userService);

        log.info("БОТ_КОНФИГ_СОЗДАНИЕ_БОТА_УСПЕХ: бот {} успешно создан", botUsername);
        return bot;
    }

    /**
     * Создает и регистрирует Telegram бота.
     *
     * @param bot экземпляр бота
     * @return TelegramBotsApi
     * @throws TelegramApiException если не удалось зарегистрировать бота
     */
    @Bean
    public TelegramBotsApi telegramBotsApi(GymTelegramBot bot) throws TelegramApiException {
        log.info("БОТ_КОНФИГ_РЕГИСТРАЦИЯ_БОТА: регистрация бота {}", botUsername);

        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        botsApi.registerBot(bot);

        log.info("БОТ_КОНФИГ_РЕГИСТРАЦИЯ_УСПЕХ: бот {} успешно зарегистрирован", botUsername);
        return botsApi;
    }

    public String getBotToken() {
        return botToken;
    }

    public String getBotUsername() {
        return botUsername;
    }

    public String getBotName() {
        return botName;
    }
}