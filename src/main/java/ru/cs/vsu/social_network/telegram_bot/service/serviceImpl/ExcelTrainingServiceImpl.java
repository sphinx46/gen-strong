package ru.cs.vsu.social_network.telegram_bot.service.serviceImpl;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import ru.cs.vsu.social_network.telegram_bot.dto.request.UserBenchPressRequest;
import ru.cs.vsu.social_network.telegram_bot.exception.GenerateTrainingPlanException;
import ru.cs.vsu.social_network.telegram_bot.provider.UserTrainingEntityProvider;
import ru.cs.vsu.social_network.telegram_bot.service.ExcelTrainingService;
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

@Slf4j
@Service
public class ExcelTrainingServiceImpl implements ExcelTrainingService {

    @Value("${training.template.path:training_cycles/gusenica_cycle.xlsx}")
    private String templatePath;

    @Value("${training.output.dir:generatedTrainingPlans}")
    private String outputDirPath;

    private final UserTrainingEntityProvider userTrainingEntityProvider;

    public ExcelTrainingServiceImpl(UserTrainingEntityProvider userTrainingEntityProvider) {
        this.userTrainingEntityProvider = userTrainingEntityProvider;
    }

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

            log.info("{}_ШАБЛОН_СУЩЕСТВУЕТ: {}", logPrefix, resource.exists());
            log.info("{}_ШАБЛОН_ПУТЬ: {}", logPrefix, resource.getPath());
            log.info("{}_ШАБЛОН_ОПИСАНИЕ: {}", logPrefix, resource.getDescription());

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

            log.info("{}_ДИРЕКТОРИЯ_СОЗДАНИЕ: {}", logPrefix, outputDirPath);
            Path outputDir = Paths.get(outputDirPath);
            if (!Files.exists(outputDir)) {
                Files.createDirectories(outputDir);
                log.info("{}_ДИРЕКТОРИЯ_СОЗДАНА: {}", logPrefix, outputDir.toAbsolutePath());
            }

            log.info("{}_EXCEL_ОТКРЫТИЕ: начало чтения шаблона", logPrefix);

            try (InputStream inputStream = resource.getInputStream()) {

                log.info("{}_EXCEL_ТИП_ФАЙЛА: определение типа файла по расширению '{}'",
                        logPrefix, templatePath);

                Workbook workbook;
                if (templatePath.toLowerCase().endsWith(".xlsx")) {
                    log.info("{}_EXCEL_ФОРМАТ: XLSX (Excel 2007+)", logPrefix);
                    workbook = new XSSFWorkbook(inputStream);
                } else if (templatePath.toLowerCase().endsWith(".xls")) {
                    log.info("{}_EXCEL_ФОРМАТ: XLS (Excel 97-2003)", logPrefix);
                    workbook = new HSSFWorkbook(inputStream);
                } else {
                    log.error("{}_EXCEL_НЕПОДДЕРЖИВАЕМЫЙ_ФОРМАТ: {}", logPrefix, templatePath);
                    throw new GenerateTrainingPlanException(
                            "Неподдерживаемый формат файла. Используйте .xls или .xlsx");
                }

                log.info("{}_EXCEL_ШАБЛОН_ПРОЧИТАН: листов в книге {}", logPrefix,
                        workbook.getNumberOfSheets());

                if (workbook.getNumberOfSheets() == 0) {
                    log.error("{}_EXCEL_ПУСТАЯ_КНИГА: нет ни одного листа", logPrefix);
                    workbook.close();
                    throw new GenerateTrainingPlanException("Excel файл не содержит листов");
                }

                Sheet sheet = workbook.getSheetAt(0);
                if (sheet == null) {
                    log.error("{}_EXCEL_ПЕРВЫЙ_ЛИСТ_НЕ_НАЙДЕН", logPrefix);
                    workbook.close();
                    throw new GenerateTrainingPlanException("Первый лист не найден в Excel файле");
                }

                log.info("{}_ДАННЫЕ_ЗАПИСЬ: максимальный жим лежа {} кг в ячейку B2",
                        logPrefix, userBenchPressRequest.getMaxBenchPress());

                int baseRow = 2;
                Row row = sheet.getRow(baseRow);
                if (row == null) {
                    row = sheet.createRow(baseRow);
                    log.info("{}_СОЗДАНА_НОВАЯ_СТРОКА: {}", logPrefix, baseRow);
                }

                int baseColumn = 1;
                Cell cell = row.getCell(baseColumn);
                if (cell == null) {
                    cell = row.createCell(baseColumn);
                    log.info("{}_СОЗДАНА_НОВАЯ_ЯЧЕЙКА: строка {}, колонка {}",
                            logPrefix, baseRow, baseColumn);
                }

                cell.setCellValue(userBenchPressRequest.getMaxBenchPress());
                log.info("{}_ДАННЫЕ_ЗАПИСАНЫ: значение {} кг записано в ячейку B{}",
                        logPrefix, userBenchPressRequest.getMaxBenchPress(), baseRow + 1);

                log.info("{}_ФОРМУЛЫ_ПЕРЕСЧЕТ: начало вычисления формул", logPrefix);
                FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
                evaluator.evaluateAll();
                log.info("{}_ФОРМУЛЫ_ПЕРЕСЧЕТ_ЗАВЕРШЕН", logPrefix);

                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                String filename = String.format("training_plan_%s_%s.xlsx", userId, timestamp);
                Path outputPath = outputDir.resolve(filename);

                log.info("{}_ФАЙЛ_СОЗДАНИЕ: {}", logPrefix, outputPath.toAbsolutePath());

                try (FileOutputStream fileOut = new FileOutputStream(outputPath.toFile())) {
                    workbook.write(fileOut);
                }

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
}