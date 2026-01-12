package ru.cs.vsu.social_network.telegram_bot.service.serviceImpl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.cs.vsu.social_network.telegram_bot.dto.request.UserBenchPressRequest;
import ru.cs.vsu.social_network.telegram_bot.exception.GenerateTrainingPlanException;
import ru.cs.vsu.social_network.telegram_bot.service.ExcelTrainingService;
import ru.cs.vsu.social_network.telegram_bot.service.ImageTrainingService;
import ru.cs.vsu.social_network.telegram_bot.service.cache.ImageCacheService;
import ru.cs.vsu.social_network.telegram_bot.service.image.ExcelToImageConverter;
import ru.cs.vsu.social_network.telegram_bot.utils.ExcelUtils;
import ru.cs.vsu.social_network.telegram_bot.utils.MessageConstants;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Реализация сервиса для генерации изображений тренировочных планов.
 * Координирует работу ExcelTrainingService и ExcelToImageConverter,
 * управляет кэшированием изображений.
 */
@Slf4j
@Service
public class ImageTrainingServiceImpl implements ImageTrainingService {

    @Value("${training.image.output.dir:generatedTrainingImages}")
    private String imageOutputDirPath;

    @Value("${training.image.format:png}")
    private String defaultImageFormat;

    @Value("${training.template.path:training_cycles/gusenica_cycle.xlsx}")
    private String templatePath;

    private final ExcelTrainingService excelTrainingService;
    private final ExcelToImageConverter excelToImageConverter;
    private final ImageCacheService imageCacheService;

    /**
     * Конструктор с внедрением зависимостей.
     */
    public ImageTrainingServiceImpl(ExcelTrainingService excelTrainingService,
                                    ExcelToImageConverter excelToImageConverter,
                                    ImageCacheService imageCacheService) {
        this.excelTrainingService = excelTrainingService;
        this.excelToImageConverter = excelToImageConverter;
        this.imageCacheService = imageCacheService;
    }

    /** {@inheritDoc} */
    @Override
    public File generateTrainingPlanImage(UUID userId, UserBenchPressRequest userBenchPressRequest) {
        log.info("ИЗОБРАЖЕНИЕ_ТРЕНИРОВОЧНЫЙ_ПЛАН_ГЕНЕРАЦИЯ_НАЧАЛО: пользователь {}, жим лежа {} кг",
                userId, userBenchPressRequest.getMaxBenchPress());

        File excelFile = null;
        BufferedImage image = null;

        try {
            String cacheKey = imageCacheService.generateSimpleCacheKey(
                    userBenchPressRequest.getMaxBenchPress(),
                    templatePath
            );

            if (imageCacheService.isCacheEnabled()) {
                File cachedFile = imageCacheService.getImagePathFromCache(cacheKey);
                if (cachedFile != null && cachedFile.exists() && cachedFile.length() > 0) {
                    log.info("ИЗОБРАЖЕНИЕ_ТРЕНИРОВОЧНЫЙ_ПЛАН_КЕШ_ПОПАДАНИЕ: файл загружен из кеша, ключ: {}",
                            cacheKey);

                    File resultFile = createUniqueOutputFile(userId);
                    Files.copy(cachedFile.toPath(), resultFile.toPath());

                    log.info("ИЗОБРАЖЕНИЕ_ТРЕНИРОВОЧНЫЙ_ПЛАН_ИЗ_КЕША_СОЗДАНО: файл {}",
                            resultFile.getAbsolutePath());

                    return resultFile;
                } else {
                    log.info("ИЗОБРАЖЕНИЕ_ТРЕНИРОВОЧНЫЙ_ПЛАН_КЕШ_ПРОМАХ: ключ {} не найден в кеше",
                            cacheKey);
                }
            }

            log.info("ИЗОБРАЖЕНИЕ_ТРЕНИРОВОЧНЫЙ_ПЛАН_EXCEL_ГЕНЕРАЦИЯ: создание Excel файла для пользователя {}", userId);

            excelFile = excelTrainingService.generateTrainingPlan(userId, userBenchPressRequest);
            validateGeneratedExcelFile(excelFile);

            File resultFile = createUniqueOutputFile(userId);

            log.info("ИЗОБРАЖЕНИЕ_ТРЕНИРОВОЧНЫЙ_ПЛАН_КОНВЕРТАЦИЯ_В_ИЗОБРАЖЕНИЕ: формат {}", defaultImageFormat);

            image = excelToImageConverter.convertExcelToImage(excelFile, defaultImageFormat);
            validateGeneratedImage(image);

            log.info("ИЗОБРАЖЕНИЕ_ТРЕНИРОВОЧНЫЙ_ПЛАН_СОХРАНЕНИЕ_ИЗОБРАЖЕНИЯ {}", resultFile.getAbsolutePath());

            ExcelUtils.saveImageWithCompression(image, defaultImageFormat, resultFile);

            long fileSize = Files.size(resultFile.toPath());
            log.info("ИЗОБРАЖЕНИЕ_ТРЕНИРОВОЧНЫЙ_ПЛАН_ИЗОБРАЖЕНИЕ_СОХРАНЕНО: размер {} байт путь {}",
                    fileSize, resultFile.getAbsolutePath());

            if (imageCacheService.isCacheEnabled()) {
                imageCacheService.cacheImagePath(
                        cacheKey,
                        resultFile,
                        image.getWidth(),
                        image.getHeight()
                );

                log.info("ИЗОБРАЖЕНИЕ_ТРЕНИРОВОЧНЫЙ_ПЛАН_КЕШ_СОХРАНЕНО: ключ {}", cacheKey);
            }

            log.info("ИЗОБРАЖЕНИЕ_ТРЕНИРОВОЧНЫЙ_ПЛАН_ГЕНЕРАЦИЯ_УСПЕХ: пользователь {} файл {}",
                    userId, resultFile.getAbsolutePath());

            return resultFile;

        } catch (GenerateTrainingPlanException e) {
            log.error("ИЗОБРАЖЕНИЕ_ТРЕНИРОВОЧНЫЙ_ПЛАН_ГЕНЕРАЦИЯ_ОШИБКА: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("ИЗОБРАЖЕНИЕ_ТРЕНИРОВОЧНЫЙ_ПЛАН_ГЕНЕРАЦИЯ_ОШИБКА: пользователь {} ошибка {}",
                    userId, e.getMessage(), e);
            throw new GenerateTrainingPlanException(MessageConstants.GENERATE_PLAN_FAILURE);
        } finally {
            cleanupResources(excelFile, image);
        }
    }

    /** {@inheritDoc} */
    @Override
    public BufferedImage convertExcelToImage(File excelFile, String outputFormat) {
        return excelToImageConverter.convertExcelToImage(excelFile, outputFormat);
    }

    /**
     * Проверяет сгенерированный Excel файл.
     */
    private void validateGeneratedExcelFile(File excelFile) {
        if (excelFile == null || !excelFile.exists()) {
            log.error("ИЗОБРАЖЕНИЕ_ТРЕНИРОВОЧНЫЙ_ПЛАН_EXCEL_ФАЙЛ_НЕ_СОЗДАН: не удалось создать Excel файл");
            throw new GenerateTrainingPlanException("Не удалось создать Excel файл для конвертации");
        }

        if (excelFile.length() == 0) {
            log.error("ИЗОБРАЖЕНИЕ_ТРЕНИРОВОЧНЫЙ_ПЛАН_EXCEL_ФАЙЛ_ПУСТОЙ: файл имеет размер 0 байт");
            throw new GenerateTrainingPlanException("Excel файл пустой");
        }

        log.info("ИЗОБРАЖЕНИЕ_ТРЕНИРОВОЧНЫЙ_ПЛАН_EXCEL_ФАЙЛ_СОЗДАН: размер {} байт, путь {}",
                excelFile.length(), excelFile.getAbsolutePath());
    }

    /**
     * Проверяет сгенерированное изображение в памяти.
     */
    private void validateGeneratedImage(BufferedImage image) {
        if (image == null) {
            log.error("ИЗОБРАЖЕНИЕ_ТРЕНИРОВОЧНЫЙ_ПЛАН_ИЗОБРАЖЕНИЕ_НЕ_СОЗДАНО: ошибка при создании изображения");
            throw new GenerateTrainingPlanException("Не удалось создать изображение");
        }

        log.info("ИЗОБРАЖЕНИЕ_ТРЕНИРОВОЧНЫЙ_ПЛАН_ИЗОБРАЖЕНИЕ_СОЗДАНО: размер {}x{} пикселей",
                image.getWidth(), image.getHeight());
    }

    /**
     * Создает уникальный выходной файл для изображения.
     */
    private File createUniqueOutputFile(UUID userId) throws Exception {
        Path outputDir = Paths.get(imageOutputDirPath);
        if (!Files.exists(outputDir)) {
            Files.createDirectories(outputDir);
            log.info("ИЗОБРАЖЕНИЕ_ТРЕНИРОВОЧНЫЙ_ПЛАН_ДИРЕКТОРИЯ_СОЗДАНИЕ: {}", outputDir.toAbsolutePath());
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = String.format("training_plan_%s_%s.%s", userId, timestamp, defaultImageFormat);
        Path filePath = outputDir.resolve(filename);

        log.info("ИЗОБРАЖЕНИЕ_ТРЕНИРОВОЧНЫЙ_ПЛАН_ФАЙЛ_СОЗДАНИЕ: {}", filePath.toAbsolutePath());

        return filePath.toFile();
    }

    /**
     * Очищает ресурсы после завершения работы.
     */
    private void cleanupResources(File excelFile, BufferedImage image) {
        if (excelFile != null && excelFile.exists()) {
            try {
                Files.deleteIfExists(excelFile.toPath());
                log.debug("ИЗОБРАЖЕНИЕ_ТРЕНИРОВОЧНЫЙ_ПЛАН_ОЧИСТКА: Excel файл удален");
            } catch (Exception e) {
                log.warn("ИЗОБРАЖЕНИЕ_ТРЕНИРОВОЧНЫЙ_ПЛАН_ОЧИСТКА_EXCEL_ОШИБКА: {}", e.getMessage());
            }
        }

        if (image != null) {
            image.flush();
        }
    }
}