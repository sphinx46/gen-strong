package ru.cs.vsu.social_network.telegram_bot.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.cs.vsu.social_network.telegram_bot.bot.GymTelegramBot;

@Slf4j
@Component
public class BotInitializer {

    private final GymTelegramBot gymTelegramBot;

    public BotInitializer(GymTelegramBot gymTelegramBot) {
        this.gymTelegramBot = gymTelegramBot;
    }

    @EventListener({ContextRefreshedEvent.class})
    public void init() {
        try {
            TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
            telegramBotsApi.registerBot(gymTelegramBot);
            log.info("БОТ_ИНИЦИАЛИЗАЦИЯ: Бот {} успешно зарегистрирован в Telegram API",
                    gymTelegramBot.getBotUsername());
        } catch (TelegramApiException e) {
            log.error("БОТ_ИНИЦИАЛИЗАЦИЯ_ОШИБКА: Ошибка при регистрации бота", e);
            log.error("Проверьте: 1) Токен бота 2) Интернет соединение 3) Настройки прокси");
        }
    }
}