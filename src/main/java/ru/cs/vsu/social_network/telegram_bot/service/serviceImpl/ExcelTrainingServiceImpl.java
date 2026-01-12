package ru.cs.vsu.social_network.telegram_bot.service.serviceImpl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.cs.vsu.social_network.telegram_bot.dto.request.UserBenchPressRequest;
import ru.cs.vsu.social_network.telegram_bot.dto.response.TrainingCycleInfo;
import ru.cs.vsu.social_network.telegram_bot.exception.GenerateTrainingPlanException;
import ru.cs.vsu.social_network.telegram_bot.provider.UserTrainingEntityProvider;
import ru.cs.vsu.social_network.telegram_bot.service.ExcelTrainingService;
import ru.cs.vsu.social_network.telegram_bot.service.training.strategy.TrainingPlanGenerationStrategy;
import ru.cs.vsu.social_network.telegram_bot.utils.MessageConstants;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Реализация сервиса для генерации Excel файлов тренировочных планов.
 * Использует стратегии для генерации различных тренировочных циклов.
 */
@Slf4j
@Service
public class ExcelTrainingServiceImpl implements ExcelTrainingService {

    private final UserTrainingEntityProvider userTrainingEntityProvider;
    private final Map<String, TrainingPlanGenerationStrategy> strategies;
    private final List<TrainingCycleInfo> availableCycles;

    /**
     * Конструктор для внедрения зависимостей
     *
     * @param userTrainingEntityProvider провайдер данных тренировок пользователя
     * @param strategyList список стратегий генерации планов
     */
    public ExcelTrainingServiceImpl(UserTrainingEntityProvider userTrainingEntityProvider,
                                    List<TrainingPlanGenerationStrategy> strategyList) {
        this.userTrainingEntityProvider = userTrainingEntityProvider;
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(
                        strategy -> strategy.getCycleInfo().getId(),
                        strategy -> strategy
                ));
        this.availableCycles = strategyList.stream()
                .map(TrainingPlanGenerationStrategy::getCycleInfo)
                .sorted(Comparator.comparing(TrainingCycleInfo::getId))
                .collect(Collectors.toList());

        log.info("EXCEL_TRAINING_SERVICE_ИНИЦИАЛИЗАЦИЯ: загружено {} тренировочных циклов",
                availableCycles.size());
    }

    /** {@inheritDoc} */
    @Override
    public File generateTrainingPlan(UUID userId, UserBenchPressRequest userBenchPressRequest, String cycleId) {
        final String logPrefix = "EXCEL_ТРЕНИРОВОЧНЫЙ_ПЛАН";

        log.info("{}_ГЕНЕРАЦИЯ_НАЧАЛО: пользователь {}, цикл {}",
                logPrefix, userId, cycleId);

        try {
            log.info("{}_ПРОВЕРКА_ДАННЫХ_ПОЛЬЗОВАТЕЛЬСКИХ: пользователь {}", logPrefix, userId);
            validateUserBenchPressData(userId, userBenchPressRequest, logPrefix);

            log.info("{}_ПОИСК_СТРАТЕГИИ: идентификатор цикла '{}'", logPrefix, cycleId);
            final TrainingPlanGenerationStrategy strategy = getStrategyById(cycleId);

            return strategy.generateTrainingPlan(userId, userBenchPressRequest);

        } catch (GenerateTrainingPlanException e) {
            log.error("{}_ГЕНЕРАЦИЯ_ОШИБКА: {}", logPrefix, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("{}_ГЕНЕРАЦИЯ_ОШИБКА_НЕИЗВЕСТНАЯ: пользователь {}, цикл {}, ошибка: {}",
                    logPrefix, userId, cycleId, e.getMessage(), e);
            throw new GenerateTrainingPlanException(MessageConstants.GENERATE_PLAN_FAILURE);
        }
    }

    /**
     * Валидирует данные пользователя о жиме лежа
     *
     * @param userId идентификатор пользователя
     * @param userBenchPressRequest запрос с данными пользователя
     * @param logPrefix префикс для логирования
     */
    private void validateUserBenchPressData(UUID userId, UserBenchPressRequest userBenchPressRequest, String logPrefix) {
        final Optional<Double> existingBenchPress = userTrainingEntityProvider.getMaxBenchPressByUserId(userId);

        if (existingBenchPress.isPresent()) {
            final double existingValue = existingBenchPress.get();
            final double newValue = userBenchPressRequest.getMaxBenchPress();

            log.info("{}_СРАВНЕНИЕ_ЗНАЧЕНИЙ: существующее значение: {} кг, новое значение: {} кг",
                    logPrefix, existingValue, newValue);

            if (Math.abs(existingValue - newValue) < 0.01) {
                log.info("{}_ЗНАЧЕНИЕ_НЕ_ИЗМЕНИЛОСЬ: используем существующее значение",
                        logPrefix);
            } else {
                log.info("{}_ЗНАЧЕНИЕ_ОБНОВЛЕНО: с {} кг на {} кг",
                        logPrefix, existingValue, newValue);
            }
        } else {
            log.info("{}_НОВОЕ_ЗНАЧЕНИЕ: пользователь ранее не указывал жим лежа",
                    logPrefix);
        }
    }

    /**
     * Получает стратегию по идентификатору цикла
     *
     * @param cycleId идентификатор цикла
     * @return стратегия генерации плана
     * @throws GenerateTrainingPlanException если стратегия не найдена
     */
    private TrainingPlanGenerationStrategy getStrategyById(String cycleId) {
        final TrainingPlanGenerationStrategy strategy = strategies.get(cycleId);

        if (strategy == null) {
            log.error("{}_ЦИКЛ_НЕ_НАЙДЕН: идентификатор '{}' не найден", "EXCEL_ТРЕНИРОВОЧНЫЙ_ПЛАН", cycleId);
            throw new GenerateTrainingPlanException(MessageConstants.TRAINING_CYCLE_NOT_FOUND);
        }

        final TrainingCycleInfo cycleInfo = strategy.getCycleInfo();
        log.info("{}_ЦИКЛ_НАЙДЕН: '{}' ({}), автор: {}",
                "EXCEL_ТРЕНИРОВОЧНЫЙ_ПЛАН", cycleInfo.getDisplayName(), cycleInfo.getId(), cycleInfo.getAuthor());

        return strategy;
    }

    /** {@inheritDoc} */
    @Override
    public List<TrainingCycleInfo> getAvailableTrainingCycles() {
        log.debug("EXCEL_TRAINING_SERVICE_ЦИКЛЫ_ПОЛУЧЕНИЕ: запрошен список доступных циклов");
        return Collections.unmodifiableList(availableCycles);
    }

    /** {@inheritDoc} */
    @Override
    public TrainingCycleInfo getTrainingCycleInfo(String cycleId) {
        log.debug("EXCEL_TRAINING_SERVICE_ЦИКЛ_ИНФО_ПОЛУЧЕНИЕ: запрошена информация о цикле '{}'", cycleId);

        final TrainingPlanGenerationStrategy strategy = strategies.get(cycleId);
        if (strategy == null) {
            log.error("EXCEL_TRAINING_SERVICE_ЦИКЛ_НЕ_НАЙДЕН: идентификатор '{}' не найден", cycleId);
            throw new GenerateTrainingPlanException(MessageConstants.TRAINING_CYCLE_NOT_FOUND);
        }

        return strategy.getCycleInfo();
    }
}