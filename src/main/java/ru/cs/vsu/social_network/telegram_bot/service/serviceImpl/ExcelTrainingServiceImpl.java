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

    /**
     * {@inheritDoc}
     */
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

            log.info("{}_ШАБЛОН_ЗАГРУЗКА: путь {}", logPrefix, templatePath);

            ClassPathResource resource = new ClassPathResource(templatePath);
            if (!resource.exists()) {
                log.error("{}_ШАБЛОН_НЕ_НАЙДЕН: {}", logPrefix, templatePath);
                throw new GenerateTrainingPlanException("Шаблон тренировочного плана не найден: " + templatePath);
            }

            log.info("{}_ДИРЕКТОРИЯ_СОЗДАНИЕ: {}", logPrefix, outputDirPath);
            Path outputDir = Paths.get(outputDirPath);
            if (!Files.exists(outputDir)) {
                Files.createDirectories(outputDir);
                log.info("{}_ДИРЕКТОРИЯ_СОЗДАНА: {}", logPrefix, outputDir.toAbsolutePath());
            }

            log.info("{}_EXCEL_ОТКРЫТИЕ: начало чтения шаблона", logPrefix);
            try (InputStream inputStream = resource.getInputStream();
                 Workbook workbook = WorkbookFactory.create(inputStream)) {

                log.info("{}_EXCEL_ШАБЛОН_ПРОЧИТАН: листов в книге {}", logPrefix,
                        workbook.getNumberOfSheets());

                Sheet sheet = workbook.getSheetAt(0);

                log.info("{}_ДАННЫЕ_ЗАПИСЬ: максимальный жим лежа {} кг в ячейку B2",
                        logPrefix, userBenchPressRequest.getMaxBenchPress());

                int baseRow = 2;
                Row row = sheet.getRow(baseRow);
                if (row == null) {
                    row = sheet.createRow(baseRow);
                }

                int baseColumn = 1;
                Cell cell = row.getCell(baseColumn);
                if (cell == null) {
                    cell = row.createCell(baseColumn);
                }

                cell.setCellValue(userBenchPressRequest.getMaxBenchPress());

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

        } catch (Exception e) {
            log.error("EXCEL_ТРЕНИРОВОЧНЫЙ_ПЛАН_ГЕНЕРАЦИЯ_ОШИБКА: пользователь {}, ошибка: {}",
                    userId, e.getMessage(), e);
            throw new GenerateTrainingPlanException(MessageConstants.GENERATE_PLAN_FAILURE);
        }
    }
}