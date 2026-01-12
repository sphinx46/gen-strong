package ru.cs.vsu.social_network.telegram_bot.service;

import ru.cs.vsu.social_network.telegram_bot.dto.request.UserBenchPressRequest;
import ru.cs.vsu.social_network.telegram_bot.dto.response.TrainingCycleInfo;
import ru.cs.vsu.social_network.telegram_bot.exception.GenerateTrainingPlanException;

import java.io.File;
import java.util.List;
import java.util.UUID;

/**
 * Сервис для генерации Excel файлов тренировочных планов.
 */
public interface ExcelTrainingService {

    /**
     * Генерирует тренировочный план для указанного цикла
     *
     * @param userId идентификатор пользователя
     * @param userBenchPressRequest запрос с данными пользователя
     * @param cycleId идентификатор тренировочного цикла
     * @return сгенерированный файл тренировочного плана
     * @throws GenerateTrainingPlanException если не удалось сгенерировать план
     */
    File generateTrainingPlan(UUID userId, UserBenchPressRequest userBenchPressRequest, String cycleId);

    /**
     * Получает список доступных тренировочных циклов
     *
     * @return список информации о тренировочных циклах
     */
    List<TrainingCycleInfo> getAvailableTrainingCycles();

    /**
     * Получает информацию о конкретном тренировочном цикле
     *
     * @param cycleId идентификатор цикла
     * @return информация о цикле
     * @throws GenerateTrainingPlanException если цикл не найден
     */
    TrainingCycleInfo getTrainingCycleInfo(String cycleId);
}