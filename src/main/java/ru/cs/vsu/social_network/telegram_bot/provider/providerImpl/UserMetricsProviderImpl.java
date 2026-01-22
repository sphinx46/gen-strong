package ru.cs.vsu.social_network.telegram_bot.provider.providerImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.cs.vsu.social_network.telegram_bot.entity.UserMetrics;
import ru.cs.vsu.social_network.telegram_bot.exception.UserMetricsNotFoundException;
import ru.cs.vsu.social_network.telegram_bot.provider.UserMetricsProvider;
import ru.cs.vsu.social_network.telegram_bot.repository.UserMetricsRepository;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserMetricsProviderImpl implements UserMetricsProvider {

    private final UserMetricsRepository userMetricsRepository;

    /**
     * {@inheritDoc}
     */
    @Override
    public UserMetrics getByTelegramId(Long telegramId) {
        log.info("USER_METRICS_ПРОВАЙДЕР_ПОЛУЧЕНИЕ_ПО_TELEGRAM_ID_НАЧАЛО: запрос метрик пользователя с telegramId: {}", telegramId);

        UserMetrics entity = userMetricsRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> {
                    log.error("USER_METRICS_ПРОВАЙДЕР_ПОЛУЧЕНИЕ_ПО_TELEGRAM_ID_ОШИБКА: метрики пользователя с telegramId: {} не найдены", telegramId);
                    return new UserMetricsNotFoundException("Метрики пользователя с telegramId: " + telegramId + " не найдены");
                });

        log.info("USER_METRICS_ПРОВАЙДЕР_ПОЛУЧЕНИЕ_ПО_TELEGRAM_ID_УСПЕХ: метрики пользователя с telegramId: {} найдены", telegramId);
        return entity;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<UserMetrics> findOptionalByTelegramId(Long telegramId) {
        log.debug("USER_METRICS_ПРОВАЙДЕР_ПОИСК_ПО_TELEGRAM_ID: поиск метрик пользователя с telegramId: {}", telegramId);
        return userMetricsRepository.findByTelegramId(telegramId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean existsByTelegramId(Long telegramId) {
        log.debug("USER_METRICS_ПРОВАЙДЕР_ПРОВЕРКА_СУЩЕСТВОВАНИЯ: проверка метрик пользователя с telegramId: {}", telegramId);
        return userMetricsRepository.existsByTelegramId(telegramId);
    }
}