package ru.cs.vsu.social_network.telegram_bot.service.training.strategy;

import ru.cs.vsu.social_network.telegram_bot.dto.request.UserBenchPressRequest;
import ru.cs.vsu.social_network.telegram_bot.dto.response.TrainingCycleInfo;
import ru.cs.vsu.social_network.telegram_bot.exception.GenerateTrainingPlanException;

import java.io.File;
import java.util.UUID;

/**
 * Интерфейс стратегии для генерации тренировочного плана
 */
public interface TrainingPlanGenerationStrategy {

    /**
     * Генерирует тренировочный план
     *
     * @param userId идентификатор пользователя
     * @param userBenchPressRequest запрос с данными пользователя
     * @return сгенерированный файл тренировочного плана
     * @throws GenerateTrainingPlanException если не удалось сгенерировать план
     */
    File generateTrainingPlan(UUID userId, UserBenchPressRequest userBenchPressRequest);

    /**
     * Возвращает информацию о тренировочном цикле
     *
     * @return информация о цикле
     */
    TrainingCycleInfo getCycleInfo();
}