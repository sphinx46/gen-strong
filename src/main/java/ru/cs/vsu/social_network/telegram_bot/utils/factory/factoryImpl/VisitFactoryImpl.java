package ru.cs.vsu.social_network.telegram_bot.utils.factory.factoryImpl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.cs.vsu.social_network.telegram_bot.entity.User;
import ru.cs.vsu.social_network.telegram_bot.entity.Visit;
import ru.cs.vsu.social_network.telegram_bot.exception.UserNotFoundException;
import ru.cs.vsu.social_network.telegram_bot.provider.UserEntityProvider;
import ru.cs.vsu.social_network.telegram_bot.utils.MessageConstants;
import ru.cs.vsu.social_network.telegram_bot.utils.factory.AbstractEntityFactory;
import ru.cs.vsu.social_network.telegram_bot.utils.factory.VisitFactory;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Реализация фабрики для создания сущностей Visit.
 * Создает новые экземпляры посещений.
 */
@Slf4j
@Component
public final class VisitFactoryImpl extends AbstractEntityFactory<Visit, Void>
        implements VisitFactory {

    private static final String ENTITY_NAME = "ПОСЕЩЕНИЕ";
    private final UserEntityProvider userEntityProvider;

    public VisitFactoryImpl(UserEntityProvider userEntityProvider) {
        this.userEntityProvider = userEntityProvider;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Visit buildEntity(Void request) {
        log.warn("{}_ФАБРИКА_ПОСТРОЕНИЕ_ПРЕДУПРЕЖДЕНИЕ: попытка создания посещения без пользователя",
                ENTITY_NAME);
        throw new IllegalStateException("Для создания посещения требуется идентификатор пользователя");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Visit buildEntityWithUserId(UUID userId, Void request) {
        log.info("{}_ФАБРИКА_ПОСТРОЕНИЕ_НАЧАЛО: построение посещения для пользователя ID: {}",
                ENTITY_NAME, userId);

        final User user = userEntityProvider.getById(userId);

        final Visit visit = Visit.builder()
                .user(user)
                .visitDate(LocalDateTime.now())
                .build();

        log.info("{}_ФАБРИКА_ПОСТРОЕНИЕ_УСПЕХ: посещение построено для пользователя: {}",
                ENTITY_NAME, userId);

        return visit;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getFactoryName() {
        return ENTITY_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Visit createFromTelegramId(Long telegramId) {
        log.info("{}_ФАБРИКА_СОЗДАНИЕ_ИЗ_TELEGRAM_НАЧАЛО: создание посещения для Telegram ID: {}",
                ENTITY_NAME, telegramId);

        final User user = userEntityProvider.findByTelegramId(telegramId)
                .orElseThrow(() -> {
                    log.error("{}_ФАБРИКА_СОЗДАНИЕ_ИЗ_TELEGRAM_ОШИБКА: " +
                                    "пользователь с Telegram ID {} не найден",
                            ENTITY_NAME, telegramId);
                    return new UserNotFoundException(
                            String.format(MessageConstants.USER_NOT_FOUND_BY_TELEGRAM_ID_FAILURE, telegramId));
                });

        final Visit visit = Visit.builder()
                .user(user)
                .visitDate(LocalDateTime.now())
                .build();

        log.info("{}_ФАБРИКА_СОЗДАНИЕ_ИЗ_TELEGRAM_УСПЕХ: " +
                        "посещение создано для пользователя: {} (Telegram ID: {})",
                ENTITY_NAME, user.getId(), telegramId);

        return visit;
    }

    /**
     * {@inheritDoc}
     */
    public Visit createForUser(User user) {
        log.info("{}_ФАБРИКА_СОЗДАНИЕ_ДЛЯ_ПОЛЬЗОВАТЕЛЯ_НАЧАЛО: для пользователя: {}",
                ENTITY_NAME, user.getId());

        final Visit visit = Visit.builder()
                .user(user)
                .visitDate(LocalDateTime.now())
                .build();

        log.info("{}_ФАБРИКА_СОЗДАНИЕ_ДЛЯ_ПОЛЬЗОВАТЕЛЯ_УСПЕХ: для пользователя: {}",
                ENTITY_NAME, user.getId());

        return visit;
    }
}