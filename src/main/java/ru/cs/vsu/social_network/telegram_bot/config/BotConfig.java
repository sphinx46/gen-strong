package ru.cs.vsu.social_network.telegram_bot.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import ru.cs.vsu.social_network.telegram_bot.bot.GymTelegramBot;

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

    @Value("${telegram.proxy.enabled:false}")
    private boolean proxyEnabled;

    @Value("${telegram.proxy.host:}")
    private String proxyHost;

    @Value("${telegram.proxy.port:0}")
    private Integer proxyPort;

    @Value("${telegram.proxy.type:SOCKS5}")
    private String proxyType;

    /**
     * Создает и настраивает опции бота как Spring Bean.
     *
     * @return настроенные опции бота
     */
    @Bean
    public DefaultBotOptions botOptions() {
        log.info("БОТ_КОНФИГ_СОЗДАНИЕ_ОПЦИЙ: создание DefaultBotOptions бина");

        final DefaultBotOptions botOptions = new DefaultBotOptions();

        if (proxyEnabled && proxyHost != null && !proxyHost.isEmpty() && proxyPort != null && proxyPort > 0) {
            log.info("БОТ_КОНФИГ_ПРОКСИ: включен прокси {}:{} тип {}",
                    proxyHost, proxyPort, proxyType);

            configureProxy(botOptions);
        } else {
            log.debug("БОТ_КОНФИГ_ПРОКСИ: прокси отключен");
        }

        return botOptions;
    }

    /**
     * Настраивает прокси для бота.
     *
     * @param botOptions опции бота для настройки
     */
    private void configureProxy(final DefaultBotOptions botOptions) {
        try {
            final DefaultBotOptions.ProxyType proxyTypeEnum =
                    DefaultBotOptions.ProxyType.valueOf(proxyType);

            botOptions.setProxyHost(proxyHost);
            botOptions.setProxyPort(proxyPort);
            botOptions.setProxyType(proxyTypeEnum);

            log.info("БОТ_КОНФИГ_ПРОКСИ_УСПЕХ: прокси {}:{} типа {} настроен",
                    proxyHost, proxyPort, proxyType);

        } catch (IllegalArgumentException e) {
            log.error("БОТ_КОНФИГ_ПРОКСИ_НЕПОДДЕРЖИВАЕМЫЙ_ТИП: тип прокси '{}' не поддерживается", proxyType);
            log.info("БОТ_КОНФИГ_ПРОКСИ: используется прямое соединение без прокси");
        }
    }

    /**
     * Создает и регистрирует экземпляр Telegram бота.
     *
     * @param botOptions опции бота
     * @return настроенный экземпляр бота
     */
    @Bean
    public GymTelegramBot gymTelegramBot(final DefaultBotOptions botOptions,
                                         final ru.cs.vsu.social_network.telegram_bot.service.TelegramCommandService telegramCommandService) {
        log.info("БОТ_КОНФИГ_СОЗДАНИЕ_БОТА: создание GymTelegramBot с username: {}", botUsername);

        final GymTelegramBot bot = new GymTelegramBot(botOptions, this, telegramCommandService);

        log.info("БОТ_КОНФИГ_СОЗДАНИЕ_БОТА_УСПЕХ: бот {} успешно создан", botUsername);
        return bot;
    }

    /**
     * Получает токен бота.
     *
     * @return токен бота
     */
    public String getBotToken() {
        return botToken;
    }

    /**
     * Получает username бота.
     *
     * @return username бота
     */
    public String getBotUsername() {
        return botUsername;
    }

    /**
     * Получает имя бота.
     *
     * @return имя бота
     */
    public String getBotName() {
        return botName;
    }
}