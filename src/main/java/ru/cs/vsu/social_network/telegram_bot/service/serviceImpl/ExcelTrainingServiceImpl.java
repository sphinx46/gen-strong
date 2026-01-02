package ru.cs.vsu.social_network.telegram_bot.service.serviceImpl;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import ru.cs.vsu.social_network.telegram_bot.dto.request.UserBenchPressRequest;
import ru.cs.vsu.social_network.telegram_bot.exception.GenerateTrainingPlanException;
import ru.cs.vsu.social_network.telegram_bot.provider.UserTrainingEntityProvider;
import ru.cs.vsu.social_network.telegram_bot.service.ExcelTrainingService;
import ru.cs.vsu.social_network.telegram_bot.utils.ExcelUtils;
import ru.cs.vsu.social_network.telegram_bot.utils.MessageConstants;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

/**
 * Реализация сервиса для генерации Excel файлов тренировочных планов.
 * Использует шаблоны Excel для создания персонализированных планов тренировок.
 */
@Slf4j
@Service
public class ExcelTrainingServiceImpl implements ExcelTrainingService {

    @Value("${training.template.path:training_cycles/gusenica_cycle.xlsx}")
    private String templatePath;

    @Value("${training.output.dir:generatedTrainingPlans}")
    private String outputDirPath;

    private final UserTrainingEntityProvider userTrainingEntityProvider;

    /**
     * Конструктор для внедрения зависимости провайдера данных пользователя.
     *
     * @param userTrainingEntityProvider провайдер данных тренировок пользователя
     */
    public ExcelTrainingServiceImpl(UserTrainingEntityProvider userTrainingEntityProvider) {
        this.userTrainingEntityProvider = userTrainingEntityProvider;
    }

    /** {@inheritDoc} */
    @Override
    public File generateTrainingPlan(UUID userId, UserBenchPressRequest userBenchPressRequest) {
        log.info("EXCEL_ТРЕНИРОВОЧНЫЙ_ПЛАН_ГЕНЕРАЦИЯ_НАЧАЛО: пользователь {}, жим лежа: {} кг",
                userId, userBenchPressRequest.getMaxBenchPress());

        String logPrefix = "EXCEL_ТРЕНИРОВОЧНЫЙ_ПЛАН";

        try {
            log.info("{}_ПРОВЕРКА_ДАННЫХ_ПОЛЬЗОВАТЕЛЯ: пользователь {}", logPrefix, userId);

            Optional<Double> existingBenchPress = userTrainingEntityProvider.getMaxBenchPressByUserId(userId);

            if (existingBenchPress.isPresent()) {
                double existingValue = existingBenchPress.get();
                double newValue = userBenchPressRequest.getMaxBenchPress();

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

            log.info("{}_ШАБЛОН_ЗАГРУЗКА: путь '{}'", logPrefix, templatePath);

            ClassPathResource resource = new ClassPathResource(templatePath);

            validateTemplateResource(resource, logPrefix);

            log.info("{}_ДИРЕКТОРИЯ_СОЗДАНИЕ: {}", logPrefix, outputDirPath);
            Path outputDir = createOutputDirectory(outputDirPath, logPrefix);

            log.info("{}_EXCEL_ОТКРЫТИЕ: начало чтения шаблона", logPrefix);

            try (InputStream inputStream = resource.getInputStream()) {
                Workbook workbook = ExcelUtils.createWorkbook(new File(templatePath), inputStream);

                log.info("{}_EXCEL_ШАБЛОН_ПРОЧИТАН: листов в книге {}", logPrefix,
                        workbook.getNumberOfSheets());

                Sheet sheet = ExcelUtils.getFirstSheet(workbook);

                writeBenchPressValue(sheet, userBenchPressRequest.getMaxBenchPress(), logPrefix);

                recalculateFormulas(workbook, logPrefix);

                Path outputPath = createOutputFilePath(userId, outputDir, logPrefix);

                saveWorkbookToFile(workbook, outputPath, logPrefix);

                workbook.close();

                long fileSize = Files.size(outputPath);
                log.info("{}_ФАЙЛ_УСПЕШНО_СОЗДАН: размер {} байт, путь: {}",
                        logPrefix, fileSize, outputPath.toAbsolutePath());

                File resultFile = outputPath.toFile();
                log.info("EXCEL_ТРЕНИРОВОЧНЫЙ_ПЛАН_ГЕНЕРАЦИЯ_УСПЕХ: пользователь {}, файл: {}",
                        userId, resultFile.getAbsolutePath());

                return resultFile;

            } catch (Exception e) {
                log.error("{}_ОШИБКА_ОБРАБОТКИ_EXCEL: {}", logPrefix, e.getMessage(), e);
                throw new GenerateTrainingPlanException("Ошибка обработки Excel файла: " + e.getMessage());
            }

        } catch (GenerateTrainingPlanException e) {
            log.error("{}_ГЕНЕРАЦИЯ_ОШИБКА: {}", logPrefix, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("EXCEL_ТРЕНИРОВОЧНЫЙ_ПЛАН_ГЕНЕРАЦИЯ_ОШИБКА: пользователь {}, ошибка: {}",
                    userId, e.getMessage(), e);
            throw new GenerateTrainingPlanException(MessageConstants.GENERATE_PLAN_FAILURE);
        }
    }

    /**
     * Проверяет доступность ресурса шаблона.
     *
     * @param resource ресурс шаблона
     * @param logPrefix префикс для логирования
     * @throws GenerateTrainingPlanException если ресурс недоступен
     */
    private void validateTemplateResource(ClassPathResource resource, String logPrefix) {
        if (!resource.exists()) {
            log.error("{}_ШАБЛОН_НЕ_НАЙДЕН: {}", logPrefix, templatePath);
            try {
                log.error("{}_ШАБЛОН_ПОЛНЫЙ_ПУТЬ: {}", logPrefix, resource.getURL());
            } catch (Exception urlEx) {
                log.error("{}_ШАБЛОН_URL_ОШИБКА: {}", logPrefix, urlEx.getMessage());
            }
            throw new GenerateTrainingPlanException("Шаблон тренировочного плана не найден: " + templatePath);
        }

        try {
            long fileSize = resource.contentLength();
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
     * Создает выходную директорию если она не существует.
     *
     * @param outputDirPath путь к директории
     * @param logPrefix префикс для логирования
     * @return путь к созданной директории
     * @throws Exception если не удалось создать директорию
     */
    private Path createOutputDirectory(String outputDirPath, String logPrefix) throws Exception {
        Path outputDir = Paths.get(outputDirPath);
        if (!Files.exists(outputDir)) {
            Files.createDirectories(outputDir);
            log.info("{}_ДИРЕКТОРИЯ_СОЗДАНА: {}", logPrefix, outputDir.toAbsolutePath());
        }
        return outputDir;
    }

    /**
     * Записывает значение жима лежа в ячейку шаблона.
     *
     * @param sheet лист Excel
     * @param benchPressValue значение жима лежа
     * @param logPrefix префикс для логирования
     */
    private void writeBenchPressValue(Sheet sheet, double benchPressValue, String logPrefix) {
        log.info("{}_ДАННЫЕ_ЗАПИСЬ: максимальный жим лежа {} кг в ячейку B2",
                logPrefix, benchPressValue);

        int baseRow = 2;
        int baseColumn = 1;

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
     * Пересчитывает формулы в книге Excel.
     *
     * @param workbook книга Excel
     * @param logPrefix префикс для логирования
     */
    private void recalculateFormulas(Workbook workbook, String logPrefix) {
        log.info("{}_ФОРМУЛЫ_ПЕРЕСЧЕТ: начало вычисления формул", logPrefix);
        FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
        evaluator.evaluateAll();
        log.info("{}_ФОРМУЛЫ_ПЕРЕСЧЕТ_ЗАВЕРШЕН", logPrefix);
    }

    /**
     * Создает путь для выходного файла.
     *
     * @param userId идентификатор пользователя
     * @param outputDir выходная директория
     * @param logPrefix префикс для логирования
     * @return путь к выходному файлу
     */
    private Path createOutputFilePath(UUID userId, Path outputDir, String logPrefix) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = String.format("training_plan_%s_%s.xlsx", userId, timestamp);
        Path outputPath = outputDir.resolve(filename);

        log.info("{}_ФАЙЛ_СОЗДАНИЕ: {}", logPrefix, outputPath.toAbsolutePath());
        return outputPath;
    }

    /**
     * Сохраняет книгу Excel в файл.
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