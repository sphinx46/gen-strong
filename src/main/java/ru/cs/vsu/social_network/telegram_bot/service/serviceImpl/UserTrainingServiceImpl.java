package ru.cs.vsu.social_network.telegram_bot.service.serviceImpl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.cs.vsu.social_network.telegram_bot.dto.request.UserBenchPressRequest;
import ru.cs.vsu.social_network.telegram_bot.dto.response.UserTrainingResponse;
import ru.cs.vsu.social_network.telegram_bot.entity.User;
import ru.cs.vsu.social_network.telegram_bot.entity.UserTraining;
import ru.cs.vsu.social_network.telegram_bot.exception.UserNotFoundException;
import ru.cs.vsu.social_network.telegram_bot.mapping.EntityMapper;
import ru.cs.vsu.social_network.telegram_bot.provider.UserEntityProvider;
import ru.cs.vsu.social_network.telegram_bot.provider.UserTrainingEntityProvider;
import ru.cs.vsu.social_network.telegram_bot.repository.UserTrainingRepository;
import ru.cs.vsu.social_network.telegram_bot.service.UserTrainingService;
import ru.cs.vsu.social_network.telegram_bot.utils.MessageConstants;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Реализация сервиса для управления данными тренировок пользователей.
 * Обеспечивает бизнес-логику работы с тренировочными данными, включая
 * сохранение, обновление и получение максимального жима лежа.
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class UserTrainingServiceImpl implements UserTrainingService {

    private static final String SERVICE_NAME = "USER_TRAINING_СЕРВИС";
    private static final String DEFAULT_TRAINING_CYCLE = "Гусеница_8_недель";

    private final UserTrainingRepository userTrainingRepository;
    private final UserTrainingEntityProvider userTrainingEntityProvider;
    private final UserEntityProvider userEntityProvider;
    private final EntityMapper entityMapper;

    public UserTrainingServiceImpl(final UserTrainingRepository userTrainingRepository,
                                   final UserTrainingEntityProvider userTrainingEntityProvider,
                                   final UserEntityProvider userEntityProvider,
                                   final EntityMapper entityMapper) {
        this.userTrainingRepository = userTrainingRepository;
        this.userTrainingEntityProvider = userTrainingEntityProvider;
        this.userEntityProvider = userEntityProvider;
        this.entityMapper = entityMapper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public UserTrainingResponse saveOrUpdateMaxBenchPress(final UUID userId,
                                                          final UserBenchPressRequest benchPressRequest) {
        log.info("{}_СОХРАНЕНИЕ_ЖИМА_ЛЕЖА_НАЧАЛО: пользователь {}, жим: {} кг",
                SERVICE_NAME, userId, benchPressRequest.getMaxBenchPress());

        final Optional<UserTraining> existingTraining = userTrainingEntityProvider.findByUserId(userId);
        final UserTraining training;

        training = existingTraining.map(userTraining -> updateExistingTraining(userTraining, benchPressRequest)).orElseGet(() -> createNewTraining(userId, benchPressRequest));

        final UserTrainingResponse response = entityMapper.map(training, UserTrainingResponse.class);

        log.info("{}_СОХРАНЕНИЕ_ЖИМА_ЛЕЖА_УСПЕХ: запись ID {} сохранена для пользователя {}",
                SERVICE_NAME, training.getId(), userId);

        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public UserTrainingResponse saveOrUpdateMaxBenchPressByTelegramId(final Long telegramId,
                                                                      final UserBenchPressRequest benchPressRequest) {
        log.info("{}_СОХРАНЕНИЕ_ЖИМА_ЛЕЖА_ПО_TELEGRAM_НАЧАЛО: Telegram ID {}, жим: {} кг",
                SERVICE_NAME, telegramId, benchPressRequest.getMaxBenchPress());

        final User user = userEntityProvider.findByTelegramId(telegramId)
                .orElseThrow(() -> new UserNotFoundException(MessageConstants.USER_NOT_FOUND_FAILURE));

        final UserTrainingResponse response = saveOrUpdateMaxBenchPress(user.getId(), benchPressRequest);

        log.info("{}_СОХРАНЕНИЕ_ЖИМА_ЛЕЖА_ПО_TELEGRAM_УСПЕХ: данные сохранены для Telegram ID {}",
                SERVICE_NAME, telegramId);

        return response;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<UserTrainingResponse> getUserTrainingByUserId(final UUID userId) {
        log.debug("{}_ПОЛУЧЕНИЕ_ТРЕНИРОВОЧНЫХ_ДАННЫХ_НАЧАЛО: пользователь {}", SERVICE_NAME, userId);

        final Optional<UserTraining> training = userTrainingEntityProvider.findByUserId(userId);
        final Optional<UserTrainingResponse> result = training.map(t -> entityMapper.map(t, UserTrainingResponse.class));

        if (result.isPresent()) {
            log.debug("{}_ПОЛУЧЕНИЕ_ТРЕНИРОВОЧНЫХ_ДАННЫХ_УСПЕХ: пользователь {}, запись ID {}",
                    SERVICE_NAME, userId, result.get().getId());
        } else {
            log.debug("{}_ПОЛУЧЕНИЕ_ТРЕНИРОВОЧНЫХ_ДАННЫХ_ДАННЫЕ_ОТСУТСТВУЮТ: пользователь {}",
                    SERVICE_NAME, userId);
        }

        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<UserTrainingResponse> getUserTrainingByTelegramId(final Long telegramId) {
        log.debug("{}_ПОЛУЧЕНИЕ_ТРЕНИРОВОЧНЫХ_ДАННЫХ_ПО_TELEGRAM_НАЧАЛО: Telegram ID {}",
                SERVICE_NAME, telegramId);

        final Optional<UserTraining> training = userTrainingEntityProvider.findByTelegramId(telegramId);
        final Optional<UserTrainingResponse> result = training.map(t -> entityMapper.map(t, UserTrainingResponse.class));

        if (result.isPresent()) {
            log.debug("{}_ПОЛУЧЕНИЕ_ТРЕНИРОВОЧНЫХ_ДАННЫХ_ПО_TELEGRAM_УСПЕХ: Telegram ID {}, " +
                            "запись ID {}",
                    SERVICE_NAME, telegramId, result.get().getId());
        } else {
            log.debug("{}_ПОЛУЧЕНИЕ_ТРЕНИРОВОЧНЫХ_ДАННЫХ_ПО_TELEGRAM_ДАННЫЕ_ОТСУТСТВУЮТ: " +
                            "Telegram ID {}",
                    SERVICE_NAME, telegramId);
        }

        return result;
    }


    /**
     * Обновляет существующую запись тренировок новым значением жима лежа.
     *
     * @param training существующая запись тренировок
     * @param benchPressRequest запрос с новым значением жима лежа
     * @return обновленная запись тренировок
     */
    private UserTraining updateExistingTraining(final UserTraining training,
                                                final UserBenchPressRequest benchPressRequest) {
        log.info("{}_ОБНОВЛЕНИЕ_СУЩЕСТВУЮЩЕЙ_ЗАПИСИ_НАЧАЛО: запись ID {}, новый жим: {} кг",
                SERVICE_NAME, training.getId(), benchPressRequest.getMaxBenchPress());

        final Double previousValue = training.getMaxBenchPress();
        final LocalDateTime now = LocalDateTime.now();

        training.setMaxBenchPress(benchPressRequest.getMaxBenchPress());
        training.setLastTrainingDate(now);
        training.setUpdatedAt(now);
        training.setTrainingCycle(DEFAULT_TRAINING_CYCLE);

        final UserTraining updatedTraining = userTrainingRepository.save(training);

        log.info("{}_ОБНОВЛЕНИЕ_СУЩЕСТВУЮЩЕЙ_ЗАПИСИ_УСПЕХ: " +
                        "запись ID {} обновлена (с {} кг на {} кг)",
                SERVICE_NAME, updatedTraining.getId(), previousValue,
                benchPressRequest.getMaxBenchPress());

        return updatedTraining;
    }

    /**
     * Создает новую запись тренировок для пользователя.
     *
     * @param userId идентификатор пользователя
     * @param benchPressRequest запрос с значением жима лежа
     * @return созданная запись тренировок
     */
    private UserTraining createNewTraining(final UUID userId,
                                           final UserBenchPressRequest benchPressRequest) {
        log.info("{}_СОЗДАНИЕ_НОВОЙ_ЗАПИСИ_НАЧАЛО: пользователь {}, жим: {} кг",
                SERVICE_NAME, userId, benchPressRequest.getMaxBenchPress());

        final User user = userEntityProvider.getById(userId);

        final LocalDateTime now = LocalDateTime.now();
        final UserTraining training = new UserTraining();

        training.setUser(user);
        training.setMaxBenchPress(benchPressRequest.getMaxBenchPress());
        training.setLastTrainingDate(now);
        training.setCreatedAt(now);
        training.setUpdatedAt(now);
        training.setTrainingCycle(DEFAULT_TRAINING_CYCLE);

        final UserTraining savedTraining = userTrainingRepository.save(training);

        log.info("{}_СОЗДАНИЕ_НОВОЙ_ЗАПИСИ_УСПЕХ: создана запись ID {} для пользователя {}",
                SERVICE_NAME, savedTraining.getId(), userId);

        return savedTraining;
    }
}