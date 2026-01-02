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
        log.info("ИЗОБРАЖЕНИЕ_ТРЕНИРОВОЧНЫЙ_ПЛАН_ГЕНЕРАЦИЯ_НАЧАЛО: пользователь {} жим лежа {} кг",
                userId, userBenchPressRequest.getMaxBenchPress());

        try {
            if (imageCacheService.isCacheEnabled()) {
                String simpleCacheKey = imageCacheService.generateSimpleCacheKey(
                        userBenchPressRequest.getMaxBenchPress(),
                        templatePath
                );

                BufferedImage cachedImage = imageCacheService.getImageFromCache(simpleCacheKey);

                if (cachedImage != null) {
                    log.info("ИЗОБРАЖЕНИЕ_ТРЕНИРОВОЧНЫЙ_ПЛАН_КЕШ_ПОПАДАНИЕ: изображение загружено из кеша ключ {}",
                            simpleCacheKey);

                    Path outputPath = createOutputPath(userId);
                    saveImageToFile(cachedImage, outputPath);

                    log.info("ИЗОБРАЖЕНИЕ_ТРЕНИРОВОЧНЫЙ_ПЛАН_ИЗ_КЕША_СОЗДАНО: файл {} размер {}x{}",
                            outputPath.toAbsolutePath(), cachedImage.getWidth(), cachedImage.getHeight());

                    return outputPath.toFile();
                } else {
                    log.info("ИЗОБРАЖЕНИЕ_ТРЕНИРОВОЧНЫЙ_ПЛАН_КЕШ_ПРОМАХ: ключ {} не найден в кеше",
                            simpleCacheKey);
                }
            }

            log.info("ИЗОБРАЖЕНИЕ_ТРЕНИРОВОЧНЫЙ_ПЛАН_EXCEL_ГЕНЕРАЦИЯ: создание Excel файла для пользователя {}", userId);

            File excelFile = excelTrainingService.generateTrainingPlan(userId, userBenchPressRequest);
            validateGeneratedExcelFile(excelFile);

            log.info("ИЗОБРАЖЕНИЕ_ТРЕНИРОВОЧНЫЙ_ПЛАН_КОНВЕРТАЦИЯ_В_ИЗОБРАЖЕНИЕ: формат {}", defaultImageFormat);

            BufferedImage image = excelToImageConverter.convertExcelToImage(excelFile, defaultImageFormat);
            validateGeneratedImage(image);

            Path outputPath = createOutputPath(userId);
            log.info("ИЗОБРАЖЕНИЕ_ТРЕНИРОВОЧНЫЙ_ПЛАН_СОХРАНЕНИЕ_ИЗОБРАЖЕНИЯ {}", outputPath.toAbsolutePath());

            saveImageToFile(image, outputPath);

            long fileSize = Files.size(outputPath);
            log.info("ИЗОБРАЖЕНИЕ_ТРЕНИРОВОЧНЫЙ_ПЛАН_ИЗОБРАЖЕНИЕ_СОХРАНЕНО: размер {} байт путь {}",
                    fileSize, outputPath.toAbsolutePath());

            File resultImage = outputPath.toFile();

            if (imageCacheService.isCacheEnabled()) {
                String simpleCacheKey = imageCacheService.generateSimpleCacheKey(
                        userBenchPressRequest.getMaxBenchPress(),
                        templatePath
                );

                imageCacheService.cacheImage(
                        simpleCacheKey,
                        image,
                        outputPath.toString(),
                        image.getWidth(),
                        image.getHeight()
                );

                log.info("ИЗОБРАЖЕНИЕ_ТРЕНИРОВОЧНЫЙ_ПЛАН_КЕШ_СОХРАНЕНО: ключ {}", simpleCacheKey);
            }

            ExcelUtils.deleteTempFile(excelFile);

            log.info("ИЗОБРАЖЕНИЕ_ТРЕНИРОВОЧНЫЙ_ПЛАН_ГЕНЕРАЦИЯ_УСПЕХ: пользователь {} файл {}",
                    userId, resultImage.getAbsolutePath());

            return resultImage;

        } catch (GenerateTrainingPlanException e) {
            log.error("ИЗОБРАЖЕНИЕ_ТРЕНИРОВОЧНЫЙ_ПЛАН_ГЕНЕРАЦИЯ_ОШИБКА: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("ИЗОБРАЖЕНИЕ_ТРЕНИРОВОЧНЫЙ_ПЛАН_ГЕНЕРАЦИЯ_ОШИБКА: пользователь {} ошибка {}", userId, e.getMessage(), e);
            throw new GenerateTrainingPlanException(MessageConstants.GENERATE_PLAN_FAILURE);
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

        log.info("ИЗОБРАЖЕНИЕ_ТРЕНИРОВОЧНЫЙ_ПЛАН_EXCEL_ФАЙЛ_СОЗДАН: размер {} байт путь {}",
                excelFile.length(), excelFile.getAbsolutePath());
    }

    /**
     * Проверяет сгенерированное изображение.
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
     * Создает путь для сохранения изображения.
     */
    private Path createOutputPath(UUID userId) throws Exception {
        Path imageOutputDir = Paths.get(imageOutputDirPath);
        if (!Files.exists(imageOutputDir)) {
            Files.createDirectories(imageOutputDir);
            log.info("ИЗОБРАЖЕНИЕ_ТРЕНИРОВОЧНЫЙ_ПЛАН_ДИРЕКТОРИЯ_СОЗДАНИЕ {}", imageOutputDir.toAbsolutePath());
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = String.format("training_plan_%s_%s.%s", userId, timestamp, defaultImageFormat);
        return imageOutputDir.resolve(filename);
    }

    /**
     * Сохраняет изображение в файл.
     */
    private void saveImageToFile(BufferedImage image, Path outputPath) throws Exception {
        ExcelUtils.saveImageWithCompression(image, defaultImageFormat, outputPath.toFile());
    }
}