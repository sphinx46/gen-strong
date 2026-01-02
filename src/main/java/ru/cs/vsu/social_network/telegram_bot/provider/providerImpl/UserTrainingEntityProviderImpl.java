package ru.cs.vsu.social_network.telegram_bot.provider.providerImpl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.cs.vsu.social_network.telegram_bot.entity.UserTraining;
import ru.cs.vsu.social_network.telegram_bot.exception.UserTrainingNotFoundException;
import ru.cs.vsu.social_network.telegram_bot.provider.AbstractEntityProvider;
import ru.cs.vsu.social_network.telegram_bot.provider.UserTrainingEntityProvider;
import ru.cs.vsu.social_network.telegram_bot.repository.UserTrainingRepository;
import ru.cs.vsu.social_network.telegram_bot.utils.MessageConstants;

import java.util.Optional;
import java.util.UUID;

/**
 * Реализация провайдера для получения сущности UserTraining.
 * Обеспечивает доступ к данным тренировок пользователей с обработкой исключительных ситуаций.
 */
@Slf4j
@Component
public final class UserTrainingEntityProviderImpl extends AbstractEntityProvider<UserTraining>
        implements UserTrainingEntityProvider {

    private static final String ENTITY_NAME = "ТРЕНИРОВОЧНЫЕ_ДАННЫЯ";
    private final UserTrainingRepository userTrainingRepository;

    public UserTrainingEntityProviderImpl(final UserTrainingRepository userTrainingRepository) {
        super(userTrainingRepository, ENTITY_NAME, () ->
                new UserTrainingNotFoundException(MessageConstants.USER_TRAINING_NOT_FOUND_FAILURE));
        this.userTrainingRepository = userTrainingRepository;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<UserTraining> findByTelegramId(final Long telegramId) {
        log.info("{}_ПРОВАЙДЕР_ПОИСК_ПО_TELEGRAM_ID_НАЧАЛО: " +
                        "поиск тренировочных данных с Telegram ID: {}",
                ENTITY_NAME, telegramId);

        final Optional<UserTraining> userTraining = userTrainingRepository.findByUserTelegramId(telegramId);

        if (userTraining.isPresent()) {
            log.info("{}_ПРОВАЙДЕР_ПОИСК_ПО_TELEGRAM_ID_УСПЕХ: " +
                            "тренировочные данные найдены для Telegram ID: {}",
                    ENTITY_NAME, telegramId);
        } else {
            log.info("{}_ПРОВАЙДЕР_ПОИСК_ПО_TELEGRAM_ID_НЕ_НАЙДЕНО: " +
                            "тренировочные данные не найдены для Telegram ID: {}",
                    ENTITY_NAME, telegramId);
        }

        return userTraining;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<UserTraining> findByUserId(final UUID userId) {
        log.info("{}_ПРОВАЙДЕР_ПОИСК_ПО_ПОЛЬЗОВАТЕЛЮ_НАЧАЛО: " +
                        "поиск тренировочных данных для пользователя: {}",
                ENTITY_NAME, userId);

        final Optional<UserTraining> userTraining = userTrainingRepository.findByUserId(userId);

        if (userTraining.isPresent()) {
            log.info("{}_ПРОВАЙДЕР_ПОИСК_ПО_ПОЛЬЗОВАТЕЛЮ_УСПЕХ: " +
                            "тренировочные данные найдены для пользователя: {}",
                    ENTITY_NAME, userId);
        } else {
            log.info("{}_ПРОВАЙДЕР_ПОИСК_ПО_ПОЛЬЗОВАТЕЛЮ_НЕ_НАЙДЕНО: " +
                            "тренировочные данные не найдены для пользователя: {}",
                    ENTITY_NAME, userId);
        }

        return userTraining;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean existsByUserId(final UUID userId) {
        log.debug("{}_ПРОВАЙДЕР_ПРОВЕРКА_СУЩЕСТВОВАНИЯ_ПО_ПОЛЬЗОВАТЕЛЮ_НАЧАЛО: " +
                        "проверка существования тренировочных данных для пользователя: {}",
                ENTITY_NAME, userId);

        final boolean exists = userTrainingRepository.existsByUserId(userId);

        log.debug("{}_ПРОВАЙДЕР_ПРОВЕРКА_СУЩЕСТВОВАНИЯ_ПО_ПОЛЬЗОВАТЕЛЮ_УСПЕХ: " +
                        "тренировочные данные для пользователя {} {}",
                ENTITY_NAME, userId, exists ? "существуют" : "не существуют");

        return exists;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean existsByTelegramId(final Long telegramId) {
        log.debug("{}_ПРОВАЙДЕР_ПРОВЕРКА_СУЩЕСТВОВАНИЯ_ПО_TELEGRAM_НАЧАЛО: " +
                        "проверка существования тренировочных данных для Telegram ID: {}",
                ENTITY_NAME, telegramId);

        final Optional<UserTraining> training = userTrainingRepository.findByUserTelegramId(telegramId);
        final boolean exists = training.isPresent();

        log.debug("{}_ПРОВАЙДЕР_ПРОВЕРКА_СУЩЕСТВОВАНИЯ_ПО_TELEGRAM_УСПЕХ: " +
                        "тренировочные данные для Telegram ID {} {}",
                ENTITY_NAME, telegramId, exists ? "существуют" : "не существуют");

        return exists;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Double> getMaxBenchPressByUserId(final UUID userId) {
        log.debug("{}_ПРОВАЙДЕР_ПОЛУЧЕНИЕ_ЖИМА_ЛЕЖА_ПО_ПОЛЬЗОВАТЕЛЮ_НАЧАЛО: пользователь {}",
                ENTITY_NAME, userId);

        final Optional<UserTraining> training = userTrainingRepository.findByUserId(userId);
        final Optional<Double> result = training.map(UserTraining::getMaxBenchPress);

        if (result.isPresent()) {
            log.debug("{}_ПРОВАЙДЕР_ПОЛУЧЕНИЕ_ЖИМА_ЛЕЖА_ПО_ПОЛЬЗОВАТЕЛЮ_УСПЕХ: " +
                            "пользователь {}, значение: {} кг",
                    ENTITY_NAME, userId, result.get());
        } else {
            log.debug("{}_ПРОВАЙДЕР_ПОЛУЧЕНИЕ_ЖИМА_ЛЕЖА_ПО_ПОЛЬЗОВАТЕЛЮ_НЕ_НАЙДЕНО: пользователь {}",
                    ENTITY_NAME, userId);
        }

        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Double> getMaxBenchPressByTelegramId(final Long telegramId) {
        log.debug("{}_ПРОВАЙДЕР_ПОЛУЧЕНИЕ_ЖИМА_ЛЕЖА_ПО_TELEGRAM_НАЧАЛО: Telegram ID {}",
                ENTITY_NAME, telegramId);

        final Optional<UserTraining> training = userTrainingRepository.findByUserTelegramId(telegramId);
        final Optional<Double> result = training.map(UserTraining::getMaxBenchPress);

        if (result.isPresent()) {
            log.debug("{}_ПРОВАЙДЕР_ПОЛУЧЕНИЕ_ЖИМА_ЛЕЖА_ПО_TELEGRAM_УСПЕХ: " +
                            "Telegram ID {}, значение: {} кг",
                    ENTITY_NAME, telegramId, result.get());
        } else {
            log.debug("{}_ПРОВАЙДЕР_ПОЛУЧЕНИЕ_ЖИМА_ЛЕЖА_ПО_TELEGRAM_НЕ_НАЙДЕНО: Telegram ID {}",
                    ENTITY_NAME, telegramId);
        }

        return result;
    }
}