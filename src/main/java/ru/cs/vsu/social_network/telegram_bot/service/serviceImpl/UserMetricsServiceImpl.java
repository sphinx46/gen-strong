package ru.cs.vsu.social_network.telegram_bot.service.serviceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.cs.vsu.social_network.telegram_bot.dto.request.UserMetricsRequest;
import ru.cs.vsu.social_network.telegram_bot.dto.response.UserMetricsResponse;
import ru.cs.vsu.social_network.telegram_bot.entity.User;
import ru.cs.vsu.social_network.telegram_bot.entity.UserMetrics;
import ru.cs.vsu.social_network.telegram_bot.mapping.EntityMapper;
import ru.cs.vsu.social_network.telegram_bot.provider.UserMetricsProvider;
import ru.cs.vsu.social_network.telegram_bot.repository.UserMetricsRepository;
import ru.cs.vsu.social_network.telegram_bot.repository.UserRepository;
import ru.cs.vsu.social_network.telegram_bot.service.UserMetricsService;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserMetricsServiceImpl implements UserMetricsService {

    private final UserMetricsProvider userMetricsProvider;
    private final UserMetricsRepository userMetricsRepository;
    private final UserRepository userRepository;
    private final EntityMapper entityMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public UserMetricsResponse saveMetrics(UserMetricsRequest request) {
        log.info("СОХРАНЕНИЕ_МЕТРИК_НАЧАЛО: для пользователя {}", request.getTelegramId());

        Optional<UserMetrics> existingMetrics = userMetricsProvider.findOptionalByTelegramId(request.getTelegramId());
        UserMetrics userMetrics;

        if (existingMetrics.isPresent()) {
            userMetrics = existingMetrics.get();
            updateUserMetricsFromRequest(request, userMetrics);
            log.debug("ОБНОВЛЕНИЕ_МЕТРИК: обновление существующих метрик для пользователя {}", request.getTelegramId());
        } else {
            userMetrics = createUserMetricsFromRequest(request);
            log.debug("СОЗДАНИЕ_МЕТРИК: создание новых метрик для пользователя {}", request.getTelegramId());
        }

        UserMetrics savedMetrics = userMetricsRepository.save(userMetrics);
        UserMetricsResponse response = entityMapper.map(savedMetrics, UserMetricsResponse.class);

        Optional<User> userOptional = userRepository.findByTelegramId(request.getTelegramId());
        userOptional.ifPresent(user -> response.setDisplayName(user.getDisplayName()));

        log.info("СОХРАНЕНИЕ_МЕТРИК_УСПЕХ: метрики сохранены для пользователя " +
                "{}", request.getTelegramId());

        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public UserMetricsResponse getMetricsByTelegramId(Long telegramId) {
        log.debug("ПОЛУЧЕНИЕ_МЕТРИК_НАЧАЛО: поиск метрик для пользователя {}", telegramId);

        Optional<UserMetrics> metricsOptional = userMetricsProvider
                .findOptionalByTelegramId(telegramId);

        if (metricsOptional.isEmpty()) {
            log.debug("МЕТРИКИ_НЕ_НАЙДЕНЫ: для пользователя {}", telegramId);
            return null;
        }

        UserMetrics metrics = metricsOptional.get();
        UserMetricsResponse response = entityMapper.map(metrics, UserMetricsResponse.class);

        Optional<User> userOptional = userRepository.findByTelegramId(telegramId);
        userOptional.ifPresent(user -> response.setDisplayName(user.getDisplayName()));

        log.debug("ПОЛУЧЕНИЕ_МЕТРИК_УСПЕХ: метрики найдены для пользователя {}", telegramId);
        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public boolean existsByTelegramId(Long telegramId) {
        boolean exists = userMetricsProvider.existsByTelegramId(telegramId);
        log.debug("ПРОВЕРКА_СУЩЕСТВОВАНИЯ_МЕТРИК: для пользователя {} - {}", telegramId, exists);
        return exists;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void deleteByTelegramId(Long telegramId) {
        log.info("УДАЛЕНИЕ_МЕТРИК_НАЧАЛО: удаление метрик пользователя {}", telegramId);

        if (userMetricsProvider.existsByTelegramId(telegramId)) {
            userMetricsRepository.deleteByTelegramId(telegramId);
            log.info("УДАЛЕНИЕ_МЕТРИК_УСПЕХ: метрики удалены для пользователя {}", telegramId);
        } else {
            log.warn("УДАЛЕНИЕ_МЕТРИК_НЕ_НАЙДЕНЫ: метрики не найдены для пользователя {}", telegramId);
        }
    }

    /**
     * Создать сущность UserMetrics из запроса
     * @param request запрос с данными метрик
     * @return созданная сущность UserMetrics
     */
    private UserMetrics createUserMetricsFromRequest(UserMetricsRequest request) {
        log.debug("СОЗДАНИЕ_МЕТРИК_ИЗ_ЗАПРОСА: создание сущности для пользователя {}", request.getTelegramId());

        return UserMetrics.builder()
                .telegramId(request.getTelegramId())
                .weight(request.getWeight())
                .goal(request.getGoal())
                .goalRussianName(request.getGoal() != null ? request.getGoal().getRussianName() : null)
                .workoutsPerWeek(request.getWorkoutsPerWeek())
                .trainingExperience(request.getTrainingExperience())
                .age(request.getAge())
                .comment(request.getComment())
                .build();
    }

    /**
     * Обновить сущность UserMetrics из запроса
     * @param request запрос с данными метрик
     * @param userMetrics сущность для обновления
     */
    private void updateUserMetricsFromRequest(UserMetricsRequest request, UserMetrics userMetrics) {
        log.debug("ОБНОВЛЕНИЕ_МЕТРИК_ИЗ_ЗАПРОСА: обновление сущности для пользователя {}", request.getTelegramId());

        if (request.getWeight() != null) {
            userMetrics.setWeight(request.getWeight());
        }

        if (request.getGoal() != null) {
            userMetrics.setGoal(request.getGoal());
            userMetrics.setGoalRussianName(request.getGoal().getRussianName());
        }

        if (request.getWorkoutsPerWeek() != null) {
            userMetrics.setWorkoutsPerWeek(request.getWorkoutsPerWeek());
        }

        if (request.getTrainingExperience() != null) {
            userMetrics.setTrainingExperience(request.getTrainingExperience());
        }

        if (request.getAge() != null) {
            userMetrics.setAge(request.getAge());
        }

        if (request.getComment() != null) {
            userMetrics.setComment(request.getComment());
        }
    }
}