package ru.cs.vsu.social_network.telegram_bot.service;

import ru.cs.vsu.social_network.telegram_bot.dto.request.UserBenchPressRequest;

import java.io.File;
import java.util.UUID;

/**
 * Сервис для генерации изображений тренировочных планов.
 */
public interface ImageTrainingService {

    /**
     * Генерирует изображение тренировочного плана в заданном формате
     *
     * @param userId идентификатор пользователя
     * @param userBenchPressRequest запрос с данными пользователя
     * @param cycleId идентификатор тренировочного цикла
     * @return сгенерированный файл изображения
     */
    File generateTrainingPlanImage(UUID userId, UserBenchPressRequest userBenchPressRequest, String cycleId);

    /**
     * Генерирует изображение тренировочного плана (устаревший метод)
     *
     * @param userId идентификатор пользователя
     * @param userBenchPressRequest запрос с данными пользователя
     * @return сгенерированный файл изображения
     */
    File generateTrainingPlanImage(UUID userId, UserBenchPressRequest userBenchPressRequest);
}