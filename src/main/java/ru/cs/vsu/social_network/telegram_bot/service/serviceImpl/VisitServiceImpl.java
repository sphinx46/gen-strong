package ru.cs.vsu.social_network.telegram_bot.service.serviceImpl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.cs.vsu.social_network.telegram_bot.dto.request.pageable.PageRequest;
import ru.cs.vsu.social_network.telegram_bot.dto.response.VisitResponse;
import ru.cs.vsu.social_network.telegram_bot.dto.response.pageable.PageResponse;
import ru.cs.vsu.social_network.telegram_bot.entity.Visit;
import ru.cs.vsu.social_network.telegram_bot.mapping.EntityMapper;
import ru.cs.vsu.social_network.telegram_bot.provider.UserEntityProvider;
import ru.cs.vsu.social_network.telegram_bot.provider.VisitEntityProvider;
import ru.cs.vsu.social_network.telegram_bot.repository.VisitRepository;
import ru.cs.vsu.social_network.telegram_bot.service.VisitService;
import ru.cs.vsu.social_network.telegram_bot.utils.factory.VisitFactory;
import ru.cs.vsu.social_network.telegram_bot.validation.VisitValidator;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Реализация сервиса для управления посещениями тренажерного зала.
 * Обеспечивает бизнес-логику обработки посещений, проверку повторных отметок и статистику.
 */
@Slf4j
@Service
public class VisitServiceImpl implements VisitService {

    private static final String SERVICE_NAME = "ПОСЕЩЕНИЕ_СЕРВИС";

    private final VisitRepository visitRepository;
    private final VisitEntityProvider visitEntityProvider;
    private final UserEntityProvider userEntityProvider;
    private final VisitFactory visitFactory;
    private final VisitValidator visitValidator;
    private final EntityMapper entityMapper;

    public VisitServiceImpl(final VisitRepository visitRepository,
                            final VisitEntityProvider visitEntityProvider,
                            final UserEntityProvider userEntityProvider,
                            final VisitFactory visitFactory,
                            final VisitValidator visitValidator,
                            final EntityMapper entityMapper) {
        this.visitRepository = visitRepository;
        this.visitEntityProvider = visitEntityProvider;
        this.userEntityProvider = userEntityProvider;
        this.visitFactory = visitFactory;
        this.visitValidator = visitValidator;
        this.entityMapper = entityMapper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public VisitResponse createVisit(final UUID userId) {
        log.info("{}_СОЗДАНИЕ_ПОСЕЩЕНИЯ_НАЧАЛО: создание посещения для пользователя: {}",
                SERVICE_NAME, userId);

        visitValidator.validateRepeatVisit(userId);

        final Visit visit = visitFactory.create(userId, null);
        final Visit savedVisit = visitRepository.save(visit);

        log.info("{}_СОЗДАНИЕ_ПОСЕЩЕНИЯ_УСПЕХ: посещение создано с ID: {} для пользователя: {}",
                SERVICE_NAME, savedVisit.getId(), userId);

        return entityMapper.map(savedVisit, VisitResponse.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public VisitResponse createVisitByTelegramId(final Long telegramId) {
        log.info("{}_СОЗДАНИЕ_ПОСЕЩЕНИЯ_ПО_TELEGRAM_НАЧАЛО: для Telegram ID: {}",
                SERVICE_NAME, telegramId);

        final var user = userEntityProvider.findByTelegramId(telegramId)
                .orElseThrow(() -> {
                    log.error("{}_СОЗДАНИЕ_ПОСЕЩЕНИЯ_ПО_TELEGRAM_ОШИБКА: " +
                            "пользователь с Telegram ID {} не найден", SERVICE_NAME, telegramId);
                    return new RuntimeException("Пользователь не найден");
                });

        visitValidator.validateRepeatVisit(user.getId());

        final Visit visit = visitFactory.createForUser(user);
        final Visit savedVisit = visitRepository.save(visit);

        log.info("{}_СОЗДАНИЕ_ПОСЕЩЕНИЯ_ПО_TELEGRAM_УСПЕХ: " +
                        "посещение создано для пользователя: {} (Telegram ID: {})",
                SERVICE_NAME, user.getId(), telegramId);

        return entityMapper.map(savedVisit, VisitResponse.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public VisitResponse getVisitById(final UUID visitId) {
        log.info("{}_ПОЛУЧЕНИЕ_ПОСЕЩЕНИЯ_ПО_ID_НАЧАЛО: запрос посещения с ID: {}",
                SERVICE_NAME, visitId);

        final Visit visit = visitEntityProvider.getById(visitId);
        final VisitResponse response = entityMapper.map(visit, VisitResponse.class);

        log.info("{}_ПОЛУЧЕНИЕ_ПОСЕЩЕНИЯ_ПО_ID_УСПЕХ: посещение с ID: {} найдено",
                SERVICE_NAME, visitId);

        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PageResponse<VisitResponse> getVisitsByDate(final LocalDate date, final PageRequest pageRequest) {
        log.info("{}_ПОЛУЧЕНИЕ_ПО_ДАТЕ_НАЧАЛО: запрос посещений за дату: {}, страница: {}",
                SERVICE_NAME, date, pageRequest.getPageNumber());

        final Pageable pageable = pageRequest.toPageable();
        final Page<Visit> visitsPage = visitRepository.findAllByDate(date, pageable);
        final Page<VisitResponse> responsePage = visitsPage.map(visit ->
                entityMapper.map(visit, VisitResponse.class));

        log.info("{}_ПОЛУЧЕНИЕ_ПО_ДАТЕ_УСПЕХ: найдено {} посещений за дату: {}",
                SERVICE_NAME, visitsPage.getTotalElements(), date);

        return PageResponse.of(responsePage);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PageResponse<VisitResponse> getVisitsByUser(final UUID userId, final PageRequest pageRequest) {
        log.info("{}_ПОЛУЧЕНИЕ_ПО_ПОЛЬЗОВАТЕЛЮ_НАЧАЛО: " +
                        "запрос посещений пользователя: {}, страница: {}",
                SERVICE_NAME, userId, pageRequest.getPageNumber());

        final var user = userEntityProvider.getById(userId);
        final Pageable pageable = pageRequest.toPageable();
        final Page<Visit> visitsPage = visitRepository.findAllByUser(user, pageable);
        final Page<VisitResponse> responsePage = visitsPage.map(visit ->
                entityMapper.map(visit, VisitResponse.class));

        log.info("{}_ПОЛУЧЕНИЕ_ПО_ПОЛЬЗОВАТЕЛЮ_УСПЕХ: " +
                        "найдено {} посещений для пользователя: {}",
                SERVICE_NAME, visitsPage.getTotalElements(), userId);

        return PageResponse.of(responsePage);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasUserVisitedToday(final UUID userId) {
        log.debug("{}_ПРОВЕРКА_ПОСЕЩЕНИЯ_СЕГОДНЯ_НАЧАЛО: для пользователя: {}",
                SERVICE_NAME, userId);

        final LocalDate today = LocalDate.now();
        final boolean hasVisited = visitEntityProvider.existsByUserIdAndDate(userId, today);

        log.debug("{}_ПРОВЕРКА_ПОСЕЩЕНИЯ_СЕГОДНЯ_УСПЕХ: " +
                "пользователь {} посетил сегодня: {}", SERVICE_NAME, userId, hasVisited);

        return hasVisited;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getVisitCountByDate(final LocalDate date) {
        log.debug("{}_ПОДСЧЕТ_ПО_ДАТЕ_НАЧАЛО: подсчет посещений за дату: {}",
                SERVICE_NAME, date);

        final long count = visitEntityProvider.countByDate(date);

        log.debug("{}_ПОДСЧЕТ_ПО_ДАТЕ_УСПЕХ: найдено {} посещений за дату: {}",
                SERVICE_NAME, count, date);

        return count;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getVisitCountByUser(final UUID userId) {
        log.debug("{}_ПОДСЧЕТ_ПО_ПОЛЬЗОВАТЕЛЮ_НАЧАЛО: подсчет посещений пользователя: {}",
                SERVICE_NAME, userId);

        final var user = userEntityProvider.getById(userId);
        final Pageable pageable = Pageable.unpaged();
        final Page<Visit> visitsPage = visitRepository.findAllByUser(user, pageable);
        final long count = visitsPage.getTotalElements();

        log.debug("{}_ПОДСЧЕТ_ПО_ПОЛЬЗОВАТЕЛЮ_УСПЕХ: " +
                "у пользователя {} найдено {} посещений", SERVICE_NAME, userId, count);

        return count;
    }
}