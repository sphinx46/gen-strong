package ru.cs.vsu.social_network.telegram_bot.service.training.strategy.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import ru.cs.vsu.social_network.telegram_bot.dto.request.UserBenchPressRequest;
import ru.cs.vsu.social_network.telegram_bot.dto.response.TrainingCycleInfo;
import ru.cs.vsu.social_network.telegram_bot.exception.GenerateTrainingPlanException;
import ru.cs.vsu.social_network.telegram_bot.service.training.strategy.TrainingPlanGenerationStrategy;
import ru.cs.vsu.social_network.telegram_bot.utils.excel.ExcelUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Абстрактный базовый класс для стратегий генерации тренировочных планов
 */
@Slf4j
public abstract class AbstractTrainingPlanGenerationStrategy implements TrainingPlanGenerationStrategy {

    @Value("${training.output.dir:generatedTrainingPlans}")
    private String outputDirPath;

    /**
     * {@inheritDoc}
     */
    @Override
    public File generateTrainingPlan(UUID userId, UserBenchPressRequest userBenchPressRequest) {
        final TrainingCycleInfo cycleInfo = getCycleInfo();
        final String logPrefix = String.format("EXCEL_ТРЕНИРОВОЧНЫЙ_ПЛАН_%s",
                cycleInfo.getId().toUpperCase());

        log.info("{}_ГЕНЕРАЦИЯ_НАЧАЛО: пользователь {}, жим лежа: {} кг, цикл: {}",
                logPrefix, userId, userBenchPressRequest.getMaxBenchPress(), cycleInfo.getDisplayName());

        try {
            log.info("{}_ШАБЛОН_ЗАГРУЗКА: путь '{}'", logPrefix, cycleInfo.getTemplatePath());

            final ClassPathResource resource = new ClassPathResource(cycleInfo.getTemplatePath());
            validateTemplateResource(resource, logPrefix, cycleInfo);

            log.info("{}_ДИРЕКТОРИЯ_СОЗДАНИЕ: {}", logPrefix, outputDirPath);
            final Path outputDir = createOutputDirectory(outputDirPath, logPrefix);

            log.info("{}_EXCEL_ОТКРЫТИЕ: начало чтения шаблона", logPrefix);

            try (InputStream inputStream = resource.getInputStream()) {
                final Workbook workbook = ExcelUtils.createWorkbook(new File(cycleInfo.getTemplatePath()), inputStream);

                log.info("{}_EXCEL_ШАБЛОН_ПРОЧИТАН: листов в книге {}", logPrefix,
                        workbook.getNumberOfSheets());

                final Sheet sheet = ExcelUtils.getFirstSheet(workbook);
                writeBenchPressValue(sheet, userBenchPressRequest.getMaxBenchPress(), logPrefix);
                recalculateFormulas(workbook, logPrefix);

                final Path outputPath = createOutputFilePath(userId, cycleInfo, outputDir, logPrefix);
                saveWorkbookToFile(workbook, outputPath, logPrefix);
                workbook.close();

                final long fileSize = Files.size(outputPath);
                log.info("{}_ФАЙЛ_УСПЕШНО_СОЗДАН: размер {} байт, путь: {}",
                        logPrefix, fileSize, outputPath.toAbsolutePath());

                final File resultFile = outputPath.toFile();
                log.info("{}_ГЕНЕРАЦИЯ_УСПЕХ: пользователь {}, файл: {}",
                        logPrefix, userId, resultFile.getAbsolutePath());

                return resultFile;

            } catch (Exception e) {
                log.error("{}_ОШИБКА_ОБРАБОТКИ_EXCEL: {}", logPrefix, e.getMessage(), e);
                throw new GenerateTrainingPlanException("Ошибка обработки Excel файла: " + e.getMessage());
            }

        } catch (GenerateTrainingPlanException e) {
            log.error("{}_ГЕНЕРАЦИЯ_ОШИБКА: {}", logPrefix, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("{}_ГЕНЕРАЦИЯ_ОШИБКА_НЕИЗВЕСТНАЯ: пользователь {}, ошибка: {}",
                    logPrefix, userId, e.getMessage(), e);
            throw new GenerateTrainingPlanException(ru.cs.vsu.social_network.telegram_bot.utils.MessageConstants.GENERATE_PLAN_FAILURE);
        }
    }

    /**
     * Проверяет доступность ресурса шаблона
     *
     * @param resource ресурс шаблона
     * @param logPrefix префикс для логирования
     * @param cycleInfo информация о цикле
     * @throws GenerateTrainingPlanException если ресурс недоступен
     */
    private void validateTemplateResource(ClassPathResource resource, String logPrefix, TrainingCycleInfo cycleInfo) {
        if (!resource.exists()) {
            log.error("{}_ШАБЛОН_НЕ_НАЙДЕН: {}", logPrefix, cycleInfo.getTemplatePath());
            try {
                log.error("{}_ШАБЛОН_ПОЛНЫЙ_ПУТЬ: {}", logPrefix, resource.getURL());
            } catch (Exception urlEx) {
                log.error("{}_ШАБЛОН_URL_ОШИБКА: {}", logPrefix, urlEx.getMessage());
            }
            throw new GenerateTrainingPlanException(
                    ru.cs.vsu.social_network.telegram_bot.utils.MessageConstants.TRAINING_CYCLE_TEMPLATE_NOT_FOUND + ": " + cycleInfo.getTemplatePath());
        }

        try {
            final long fileSize = resource.contentLength();
            log.info("{}_ШАБЛОН_РАЗМЕР: {} байт", logPrefix, fileSize);

            if (fileSize == 0) {
                log.error("{}_ШАБЛОН_ПУСТОЙ: файл имеет размер 0 байт", logPrefix);
                throw new GenerateTrainingPlanException("Файл шаблона пустой");
            }
        } catch (Exception sizeEx) {
            log.warn("{}_ШАБЛОН_РАЗМЕР_ОШИБКА: не удалось определить размер файла: {}",
                    logPrefix, sizeEx.getMessage());
        }
    }

    /**
     * Создает выходную директорию если она не существует
     *
     * @param outputDirPath путь к директории
     * @param logPrefix префикс для логирования
     * @return путь к созданной директории
     * @throws Exception если не удалось создать директорию
     */
    private Path createOutputDirectory(String outputDirPath, String logPrefix) throws Exception {
        final Path outputDir = Paths.get(outputDirPath);
        if (!Files.exists(outputDir)) {
            Files.createDirectories(outputDir);
            log.info("{}_ДИРЕКТОРИЯ_СОЗДАНА: {}", logPrefix, outputDir.toAbsolutePath());
        }
        return outputDir;
    }

    /**
     * Записывает значение жима лежа в ячейку шаблона
     *
     * @param sheet лист Excel
     * @param benchPressValue значение жима лежа
     * @param logPrefix префикс для логирования
     */
    private void writeBenchPressValue(Sheet sheet, double benchPressValue, String logPrefix) {
        log.info("{}_ДАННЫЕ_ЗАПИСЬ: максимальный жим лежа {} кг в ячейку B2",
                logPrefix, benchPressValue);

        final int baseRow = 2;
        final int baseColumn = 1;

        Row row = sheet.getRow(baseRow);
        if (row == null) {
            row = sheet.createRow(baseRow);
            log.info("{}_СОЗДАНА_НОВАЯ_СТРОКА: {}", logPrefix, baseRow);
        }

        Cell cell = row.getCell(baseColumn);
        if (cell == null) {
            cell = row.createCell(baseColumn);
            log.info("{}_СОЗДАНА_НОВАЯ_ЯЧЕЙКА: строка {}, колонка {}",
                    logPrefix, baseRow, baseColumn);
        }

        cell.setCellValue(benchPressValue);
        log.info("{}_ДАННЫЕ_ЗАПИСАНЫ: значение {} кг записано в ячейку B{}",
                logPrefix, benchPressValue, baseRow + 1);
    }

    /**
     * Пересчитывает формулы в книге Excel
     *
     * @param workbook книга Excel
     * @param logPrefix префикс для логирования
     */
    private void recalculateFormulas(Workbook workbook, String logPrefix) {
        log.info("{}_ФОРМУЛЫ_ПЕРЕСЧЕТ: начало вычисления формул", logPrefix);
        final FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
        evaluator.evaluateAll();
        log.info("{}_ФОРМУЛЫ_ПЕРЕСЧЕТ_ЗАВЕРШЕН", logPrefix);
    }

    /**
     * Создает путь для выходного файла
     *
     * @param userId идентификатор пользователя
     * @param cycleInfo информация о тренировочном цикле
     * @param outputDir выходная директория
     * @param logPrefix префикс для логирования
     * @return путь к выходному файлу
     */
    private Path createOutputFilePath(UUID userId, TrainingCycleInfo cycleInfo, Path outputDir, String logPrefix) {
        final String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        final String filename = String.format("training_plan_%s_%s_%s.xlsx",
                cycleInfo.getId(), userId, timestamp);
        final Path outputPath = outputDir.resolve(filename);

        log.info("{}_ФАЙЛ_СОЗДАНИЕ: {}", logPrefix, outputPath.toAbsolutePath());
        return outputPath;
    }

    /**
     * Сохраняет книгу Excel в файл
     *
     * @param workbook книга Excel
     * @param outputPath путь к выходному файлу
     * @param logPrefix префикс для логирования
     * @throws Exception если не удалось сохранить файл
     */
    private void saveWorkbookToFile(Workbook workbook, Path outputPath, String logPrefix) throws Exception {
        try (FileOutputStream fileOut = new FileOutputStream(outputPath.toFile())) {
            workbook.write(fileOut);
            log.info("{}_ФАЙЛ_СОХРАНЕН: {}", logPrefix, outputPath.toAbsolutePath());
        }
    }
}