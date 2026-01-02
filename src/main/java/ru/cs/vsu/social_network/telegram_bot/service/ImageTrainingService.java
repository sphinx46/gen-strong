package ru.cs.vsu.social_network.telegram_bot.service;

import ru.cs.vsu.social_network.telegram_bot.dto.request.UserBenchPressRequest;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.UUID;

/**
 * Сервис для генерации изображений с тренировочными программами.
 * Конвертирует Excel файлы в изображения (альтернатива Excel документам).
 */
public interface ImageTrainingService {

    /**
     * Генерирует изображение с индивидуальной программой тренировок.
     * Создает Excel файл на основе шаблона, конвертирует его в изображение
     * и сохраняет результат в файл.
     *
     * @param userId идентификатор пользователя
     * @param userBenchPressRequest запрос с максимальным жимом лежа
     * @return сгенерированное изображение с программой тренировок
     * @throws ru.cs.vsu.social_network.telegram_bot.exception.GenerateTrainingPlanException если не удалось сгенерировать изображение
     */
    File generateTrainingPlanImage(UUID userId, UserBenchPressRequest userBenchPressRequest);

    /**
     * Конвертирует Excel файл в изображение.
     * Поддерживает различные форматы вывода (PNG, JPEG).
     *
     * @param excelFile исходный Excel файл
     * @param outputFormat формат выходного изображения (PNG, JPEG)
     * @return объект BufferedImage с изображением
     * @throws ru.cs.vsu.social_network.telegram_bot.exception.GenerateTrainingPlanException если не удалось конвертировать файл
     */
    BufferedImage convertExcelToImage(File excelFile, String outputFormat);
}