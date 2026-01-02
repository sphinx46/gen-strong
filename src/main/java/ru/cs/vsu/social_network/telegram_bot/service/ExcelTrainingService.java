package ru.cs.vsu.social_network.telegram_bot.service;

import ru.cs.vsu.social_network.telegram_bot.dto.request.UserBenchPressRequest;

import java.io.File;
import java.util.UUID;

/**
 * Сервис для генерации Excel файлов с тренировочными программами.
 * Загружает шаблон Excel, подставляет данные пользователя и сохраняет результат.
 */
public interface ExcelTrainingService {

    /**
     * Генерирует Excel файл с индивидуальной программой тренировок.
     * Загружает шаблон из ресурсов, подставляет максимальный жим лежа пользователя,
     * пересчитывает формулы и сохраняет результат в файл.
     *
     * @param userId идентификатор пользователя
     * @param userBenchPressRequest запрос с максимальным жимом лежа
     * @return сгенерированный Excel файл с программой тренировок
     * @throws ru.cs.vsu.social_network.telegram_bot.exception.GenerateTrainingPlanException если не удалось сгенерировать программу
     */
    File generateTrainingPlan(UUID userId, UserBenchPressRequest userBenchPressRequest);
}