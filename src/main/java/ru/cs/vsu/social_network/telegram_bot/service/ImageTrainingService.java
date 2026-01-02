package ru.cs.vsu.social_network.telegram_bot.service;

import ru.cs.vsu.social_network.telegram_bot.dto.request.UserBenchPressRequest;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.UUID;

/**
 * Сервис для генерации изображений тренировочных планов из Excel файлов.
 */
public interface ImageTrainingService {

    /**
     * Генерирует изображение тренировочного плана на основе данных пользователя.
     *
     * @param userId идентификатор пользователя
     * @param userBenchPressRequest данные о жиме лежа пользователя
     * @return файл с изображением тренировочного плана
     */
    File generateTrainingPlanImage(UUID userId, UserBenchPressRequest userBenchPressRequest);

    /**
     * Конвертирует Excel файл в изображение.
     *
     * @param excelFile Excel файл для конвертации
     * @param outputFormat формат выходного изображения
     * @return изображение с содержимым Excel файла
     */
    BufferedImage convertExcelToImage(File excelFile, String outputFormat);
}