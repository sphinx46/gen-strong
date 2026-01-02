package ru.cs.vsu.social_network.telegram_bot.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.cs.vsu.social_network.telegram_bot.bot.GymTelegramBot;

@Slf4j
@Component
public class BotInitializer {

    private static final String INITIALIZER_NAME = "БОТ_ИНИЦИАЛИЗАЦИЯ";

    private final GymTelegramBot gymTelegramBot;

    @Autowired
    public BotInitializer(GymTelegramBot gymTelegramBot) {
        this.gymTelegramBot = gymTelegramBot;
    }

    @EventListener({ContextRefreshedEvent.class})
    public void init() {
        log.info("{}_НАЧАЛО: попытка регистрации бота {}",
                INITIALIZER_NAME, gymTelegramBot.getBotUsername());

        try {
            TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);

            log.info("{}_СОЗДАНИЕ_API: создан TelegramBotsApi", INITIALIZER_NAME);

            telegramBotsApi.registerBot(gymTelegramBot);

            log.info("{}_УСПЕХ: бот {} успешно зарегистрирован",
                    INITIALIZER_NAME, gymTelegramBot.getBotUsername());

        } catch (TelegramApiRequestException e) {
            if (e.getMessage().contains("Error removing old webhook")) {
                log.warn("{}_ПРЕДУПРЕЖДЕНИЕ: ошибка при удалении старого вебхука. " +
                                "Это может быть связано с проблемами сети или настройками прокси. {}",
                        INITIALIZER_NAME, e.getMessage());

                try {
                    Thread.sleep(2000);
                    TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
                    telegramBotsApi.registerBot(gymTelegramBot);

                    log.info("{}_УСПЕХ_ПОВТОР: бот {} успешно зарегистрирован после повторной попытки",
                            INITIALIZER_NAME, gymTelegramBot.getBotUsername());

                } catch (Exception retryException) {
                    log.error("{}_КРИТИЧЕСКАЯ_ОШИБКА: не удалось зарегистрировать бота после повторной попытки. {}",
                            INITIALIZER_NAME, retryException.getMessage(), retryException);
                }
            } else {
                log.error("{}_ОШИБКА_API: ошибка при регистрации бота: {}",
                        INITIALIZER_NAME, e.getMessage(), e);
            }
        } catch (TelegramApiException e) {
            log.error("{}_ОШИБКА: ошибка при создании Telegram API: {}",
                    INITIALIZER_NAME, e.getMessage(), e);
        } catch (Exception e) {
            log.error("{}_НЕИЗВЕСТНАЯ_ОШИБКА: {}", INITIALIZER_NAME, e.getMessage(), e);
        }
    }

    public void shutdown() {
        log.info("{}_ЗАВЕРШЕНИЕ: остановка бота {}", INITIALIZER_NAME, gymTelegramBot.getBotUsername());
    }
}