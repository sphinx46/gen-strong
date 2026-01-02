package ru.cs.vsu.social_network.telegram_bot.service.serviceImpl;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.cs.vsu.social_network.telegram_bot.dto.request.UserBenchPressRequest;
import ru.cs.vsu.social_network.telegram_bot.exception.GenerateTrainingPlanException;
import ru.cs.vsu.social_network.telegram_bot.service.ExcelTrainingService;
import ru.cs.vsu.social_network.telegram_bot.service.ImageTrainingService;
import ru.cs.vsu.social_network.telegram_bot.utils.MessageConstants;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Service
public class ImageTrainingServiceImpl implements ImageTrainingService {

    @Value("${training.image.output.dir:generatedTrainingImages}")
    private String imageOutputDirPath;

    @Value("${training.image.format:png}")
    private String defaultImageFormat;

    @Value("${training.image.width:2650}")
    private int defaultImageWidth;

    @Value("${training.image.cell.height:95}")
    private int cellHeight;

    @Value("${training.image.header.height:120}")
    private int headerHeight;

    @Value("${training.image.footer.height:80}")
    private int footerHeight;

    @Value("${training.image.padding:60}")
    private int padding;

    @Value("${training.image.font.size.title:36}")
    private int titleFontSize;

    @Value("${training.image.font.size.header:20}")
    private int headerFontSize;

    @Value("${training.image.font.size.cell:18}")
    private int cellFontSize;

    @Value("${training.image.min.column.width:180}")
    private int minColumnWidth;

    @Value("${training.image.max.columns:25}")
    private int maxColumns;

    private final ExcelTrainingService excelTrainingService;
    private final Color backgroundColor;
    private final Color headerColor;
    private final Color tableHeaderColor;
    private final Color cellBorderColor;
    private final Color oddRowColor;
    private final Color evenRowColor;
    private final Color textColor;
    private final Color footerTextColor;

    public ImageTrainingServiceImpl(ExcelTrainingService excelTrainingService) {
        this.excelTrainingService = excelTrainingService;
        this.backgroundColor = new Color(250, 250, 250);
        this.headerColor = new Color(52, 152, 219);
        this.tableHeaderColor = new Color(41, 128, 185);
        this.cellBorderColor = new Color(220, 220, 220);
        this.oddRowColor = new Color(255, 255, 255);
        this.evenRowColor = new Color(245, 245, 245);
        this.textColor = new Color(235, 9, 9);
        this.footerTextColor = new Color(100, 100, 100);
    }

    @Override
    public File generateTrainingPlanImage(UUID userId, UserBenchPressRequest userBenchPressRequest) {
        log.info("ИЗОБРАЖЕНИЕ_ТРЕНИРОВОЧНЫЙ_ПЛАН_ГЕНЕРАЦИЯ_НАЧАЛО: пользователь {}, жим лежа: {} кг",
                userId, userBenchPressRequest.getMaxBenchPress());

        final String logPrefix = "ИЗОБРАЖЕНИЕ_ТРЕНИРОВОЧНЫЙ_ПЛАН";

        try {
            log.info("{}_EXCEL_ГЕНЕРАЦИЯ: создание Excel файла для пользователя {}", logPrefix, userId);

            File excelFile = excelTrainingService.generateTrainingPlan(userId, userBenchPressRequest);

            if (excelFile == null || !excelFile.exists()) {
                log.error("{}_EXCEL_ФАЙЛ_НЕ_СОЗДАН: не удалось создать Excel файл", logPrefix);
                throw new GenerateTrainingPlanException("Не удалось создать Excel файл для конвертации");
            }

            log.info("{}_EXCEL_ФАЙЛ_СОЗДАН: размер {} байт, путь: {}",
                    logPrefix, excelFile.length(), excelFile.getAbsolutePath());

            log.info("{}_КОНВЕРТАЦИЯ_В_ИЗОБРАЖЕНИЕ: формат {}", logPrefix, defaultImageFormat);

            BufferedImage image = convertExcelToImage(excelFile, defaultImageFormat);

            log.info("{}_ИЗОБРАЖЕНИЕ_СОЗДАНО: размер {}x{} пикселей",
                    logPrefix, image.getWidth(), image.getHeight());

            Path outputPath = createOutputPath(userId);
            log.info("{}_СОХРАНЕНИЕ_ИЗОБРАЖЕНИЯ: {}", logPrefix, outputPath.toAbsolutePath());

            saveImageToFile(image, outputPath);
            long fileSize = Files.size(outputPath);
            log.info("{}_ИЗОБРАЖЕНИЕ_СОХРАНЕНО: размер {} байт, путь: {}",
                    logPrefix, fileSize, outputPath.toAbsolutePath());

            File resultImage = outputPath.toFile();

            deleteTempExcelFile(logPrefix, excelFile);

            log.info("ИЗОБРАЖЕНИЕ_ТРЕНИРОВОЧНЫЙ_ПЛАН_ГЕНЕРАЦИЯ_УСПЕХ: пользователь {}, файл: {}",
                    userId, resultImage.getAbsolutePath());

            return resultImage;

        } catch (GenerateTrainingPlanException e) {
            log.error("{}_ГЕНЕРАЦИЯ_ОШИБКА: {}", logPrefix, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("ИЗОБРАЖЕНИЕ_ТРЕНИРОВОЧНЫЙ_ПЛАН_ГЕНЕРАЦИЯ_ОШИБКА: пользователь {}, ошибка: {}",
                    userId, e.getMessage(), e);
            throw new GenerateTrainingPlanException(MessageConstants.GENERATE_PLAN_FAILURE);
        }
    }

    @Override
    public BufferedImage convertExcelToImage(File excelFile, String outputFormat) {
        log.info("EXCEL_В_ИЗОБРАЖЕНИЕ_КОНВЕРТАЦИЯ_НАЧАЛО: файл {}, формат {}",
                excelFile.getName(), outputFormat);

        final String logPrefix = "EXCEL_КОНВЕРТАЦИЯ";

        validateExcelFile(excelFile, logPrefix);

        try (InputStream inputStream = new FileInputStream(excelFile)) {
            Workbook workbook = createWorkbook(excelFile, inputStream, logPrefix);

            try {
                log.info("{}_EXCEL_ОБРАБОТКА: листов в книге {}", logPrefix,
                        workbook.getNumberOfSheets());

                Sheet sheet = getFirstSheet(workbook, logPrefix);

                int rowCount = sheet.getLastRowNum() + 1;
                int columnCount = calculateColumnCount(sheet, rowCount);

                log.info("{}_ТАБЛИЦА_РАЗМЕР: строк {}, колонок {}", logPrefix, rowCount, columnCount);

                int maxRowsToDisplay = Math.min(rowCount, 45);
                int imageHeight = headerHeight + (maxRowsToDisplay * cellHeight) + footerHeight + 200;

                BufferedImage image = new BufferedImage(defaultImageWidth, imageHeight,
                        BufferedImage.TYPE_INT_RGB);
                Graphics2D graphics = image.createGraphics();

                try {
                    configureGraphicsQuality(graphics);
                    drawImageContent(graphics, sheet, columnCount, maxRowsToDisplay, imageHeight);
                } finally {
                    graphics.dispose();
                }

                log.info("{}_ИЗОБРАЖЕНИЕ_СОЗДАНО: размер {}x{}", logPrefix,
                        image.getWidth(), image.getHeight());

                return image;

            } finally {
                workbook.close();
            }

        } catch (Exception e) {
            log.error("{}_КОНВЕРТАЦИЯ_ОШИБКА: {}", logPrefix, e.getMessage(), e);
            throw new GenerateTrainingPlanException("Ошибка конвертации Excel в изображение: " + e.getMessage());
        }
    }

    @Override
    public File generateImageFromData(UUID userId, Double maxBenchPress, String trainingCycleName) {
        log.info("ИЗОБРАЖЕНИЕ_ИЗ_ДАННЫХ_ГЕНЕРАЦИЯ_НАЧАЛО: пользователь {}, жим лежа: {} кг, цикл: {}",
                userId, maxBenchPress, trainingCycleName);

        final String logPrefix = "ИЗОБРАЖЕНИЕ_ИЗ_ДАННЫХ";

        try {
            Path outputPath = createOutputPath(userId);

            BufferedImage image = new BufferedImage(defaultImageWidth, 500,
                    BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();

            try {
                configureGraphicsQuality(graphics);
                drawSimpleImageContent(graphics);
            } finally {
                graphics.dispose();
            }

            saveImageToFile(image, outputPath);
            long fileSize = Files.size(outputPath);
            log.info("{}_ИЗОБРАЖЕНИЕ_СОХРАНЕНО: размер {} байт", logPrefix, fileSize);

            File resultImage = outputPath.toFile();
            log.info("ИЗОБРАЖЕНИЕ_ИЗ_ДАННЫХ_ГЕНЕРАЦИЯ_УСПЕХ: пользователь {}, файл: {}",
                    userId, resultImage.getAbsolutePath());

            return resultImage;

        } catch (Exception e) {
            log.error("{}_ГЕНЕРАЦИЯ_ОШИБКА: {}", logPrefix, e.getMessage(), e);
            throw new GenerateTrainingPlanException("Ошибка генерации изображения из данных: " + e.getMessage());
        }
    }

    private Path createOutputPath(UUID userId) throws IOException {
        Path imageOutputDir = Paths.get(imageOutputDirPath);
        if (!Files.exists(imageOutputDir)) {
            Files.createDirectories(imageOutputDir);
            log.info("ДИРЕКТОРИЯ_СОЗДАНИЕ: {}", imageOutputDir.toAbsolutePath());
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = String.format("training_plan_%s_%s.%s", userId, timestamp, defaultImageFormat);
        return imageOutputDir.resolve(filename);
    }

    private void saveImageToFile(BufferedImage image, Path outputPath) throws IOException {
        try (FileOutputStream imageOut = new FileOutputStream(outputPath.toFile())) {
            boolean written = ImageIO.write(image, defaultImageFormat, imageOut);

            if (!written) {
                throw new GenerateTrainingPlanException("Формат изображения не поддерживается: " + defaultImageFormat);
            }
        }
    }

    private void deleteTempExcelFile(String logPrefix, File excelFile) {
        log.info("{}_ВРЕМЕННЫЙ_ФАЙЛ_УДАЛЕНИЕ: Excel файл {}", logPrefix, excelFile.getAbsolutePath());
        if (excelFile.delete()) {
            log.info("{}_ВРЕМЕННЫЙ_ФАЙЛ_УДАЛЕН", logPrefix);
        } else {
            log.warn("{}_ВРЕМЕННЫЙ_ФАЙЛ_НЕ_УДАЛЕН: требуется ручное удаление", logPrefix);
        }
    }

    private void validateExcelFile(File excelFile, String logPrefix) {
        if (!excelFile.exists() || !excelFile.canRead()) {
            log.error("{}_ФАЙЛ_НЕДОСТУПЕН: {}", logPrefix, excelFile.getAbsolutePath());
            throw new GenerateTrainingPlanException("Excel файл недоступен для чтения");
        }
        log.info("{}_EXCEL_ЧТЕНИЕ: размер файла {} байт", logPrefix, excelFile.length());
    }

    private Workbook createWorkbook(File excelFile, InputStream inputStream, String logPrefix) throws IOException {
        if (excelFile.getName().toLowerCase().endsWith(".xlsx")) {
            log.info("{}_EXCEL_ФОРМАТ: XLSX", logPrefix);
            return new XSSFWorkbook(inputStream);
        } else if (excelFile.getName().toLowerCase().endsWith(".xls")) {
            log.info("{}_EXCEL_ФОРМАТ: XLS", logPrefix);
            return new HSSFWorkbook(inputStream);
        } else {
            log.error("{}_НЕПОДДЕРЖИВАЕМЫЙ_ФОРМАТ: {}", logPrefix, excelFile.getName());
            throw new GenerateTrainingPlanException("Неподдерживаемый формат Excel файла");
        }
    }

    private Sheet getFirstSheet(Workbook workbook, String logPrefix) {
        if (workbook.getNumberOfSheets() == 0) {
            log.error("{}_ПУСТАЯ_КНИГА: нет листов", logPrefix);
            throw new GenerateTrainingPlanException("Excel файл не содержит листов");
        }

        Sheet sheet = workbook.getSheetAt(0);
        if (sheet == null) {
            log.error("{}_ЛИСТ_НЕ_НАЙДЕН", logPrefix);
            throw new GenerateTrainingPlanException("Первый лист не найден");
        }
        return sheet;
    }

    private int calculateColumnCount(Sheet sheet, int rowCount) {
        int columnCount = 0;
        for (int i = 0; i < rowCount; i++) {
            Row row = sheet.getRow(i);
            if (row != null) {
                columnCount = Math.max(columnCount, row.getLastCellNum());
            }
        }
        return Math.min(columnCount, maxColumns);
    }

    private void configureGraphicsQuality(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
    }

    private void drawImageContent(Graphics2D graphics, Sheet sheet, int columnCount,
                                  int maxRowsToDisplay, int imageHeight) {
        graphics.setColor(backgroundColor);
        graphics.fillRect(0, 0, defaultImageWidth, imageHeight);

        drawHeader(graphics);
        drawTable(graphics, sheet, columnCount, maxRowsToDisplay);
        drawFooter(graphics, imageHeight);
    }

    private void drawHeader(Graphics2D graphics) {
        graphics.setColor(headerColor);
        graphics.fillRect(0, 0, defaultImageWidth, headerHeight);

        Font titleFont = new Font("Arial", Font.BOLD, titleFontSize);
        graphics.setFont(titleFont);
        graphics.setColor(Color.WHITE);

        String title = "ТРЕНИРОВОЧНЫЙ ПЛАН";
        FontMetrics titleMetrics = graphics.getFontMetrics(titleFont);
        int titleX = (defaultImageWidth - titleMetrics.stringWidth(title)) / 2;
        graphics.drawString(title, titleX, headerHeight - 45);
    }

    private void drawTable(Graphics2D graphics, Sheet sheet, int columnCount, int maxRowsToDisplay) {
        int tableWidth = defaultImageWidth - (2 * padding);
        int colWidth = Math.max(minColumnWidth, tableWidth / Math.max(columnCount, 1));
        tableWidth = colWidth * Math.min(columnCount, maxColumns);

        int tableStartY = headerHeight + padding;

        drawTableHeader(graphics, sheet, tableWidth, colWidth, tableStartY, columnCount);
        drawTableRows(graphics, sheet, tableWidth, colWidth, tableStartY, maxRowsToDisplay, columnCount);
    }

    private void drawTableHeader(Graphics2D graphics, Sheet sheet, int tableWidth, int colWidth,
                                 int tableStartY, int columnCount) {
        graphics.setColor(tableHeaderColor);
        graphics.fillRect(padding, tableStartY, tableWidth, cellHeight);

        graphics.setColor(Color.WHITE);
        Font headerFont = new Font("Arial", Font.BOLD, headerFontSize);
        graphics.setFont(headerFont);

        Row headerRow = sheet.getRow(0);
        if (headerRow != null) {
            for (int j = 0; j < columnCount && j < maxColumns; j++) {
                Cell cell = headerRow.getCell(j);
                int x = padding + j * colWidth;
                int y = tableStartY + cellHeight - 20;

                String cellValue = getCellValueAsString(cell);
                if (cellValue != null && !cellValue.isEmpty()) {
                    FontMetrics metrics = graphics.getFontMetrics();
                    String trimmedValue = trimTextToFit(cellValue, metrics, colWidth - 30);
                    graphics.drawString(trimmedValue, x + 15, y);
                }
            }
        }

        graphics.setColor(cellBorderColor);
        graphics.drawRect(padding, tableStartY, tableWidth, cellHeight);
    }

    private void drawTableRows(Graphics2D graphics, Sheet sheet, int tableWidth, int colWidth,
                               int tableStartY, int maxRowsToDisplay, int columnCount) {
        Font cellFont = new Font("Arial", Font.PLAIN, cellFontSize);

        for (int i = 1; i < maxRowsToDisplay; i++) {
            Row row = sheet.getRow(i);
            int y = tableStartY + i * cellHeight;

            Color rowColor = (i % 2 == 0) ? evenRowColor : oddRowColor;
            graphics.setColor(rowColor);
            graphics.fillRect(padding, y, tableWidth, cellHeight);

            graphics.setColor(cellBorderColor);
            graphics.drawRect(padding, y, tableWidth, cellHeight);

            if (row != null) {
                graphics.setColor(textColor);
                graphics.setFont(cellFont);

                for (int j = 0; j < columnCount && j < maxColumns; j++) {
                    Cell cell = row.getCell(j);
                    int x = padding + j * colWidth;
                    int textY = y + cellHeight - 20;

                    String cellValue = getCellValueAsString(cell);
                    if (cellValue != null && !cellValue.isEmpty()) {
                        FontMetrics metrics = graphics.getFontMetrics();
                        String trimmedValue = trimTextToFit(cellValue, metrics, colWidth - 30);
                        graphics.drawString(trimmedValue, x + 15, textY);
                    }

                    graphics.setColor(cellBorderColor);
                    graphics.drawLine(x, y, x, y + cellHeight);
                }
            }

            for (int j = 0; j <= Math.min(columnCount, maxColumns); j++) {
                int x = padding + j * colWidth;
                graphics.drawLine(x, tableStartY, x, tableStartY + maxRowsToDisplay * cellHeight);
            }
        }
    }

    private void drawFooter(Graphics2D graphics, int imageHeight) {
        graphics.setColor(footerTextColor);
        Font footerFont = new Font("Arial", Font.ITALIC, 16);
        graphics.setFont(footerFont);

        String footerText = "Сгенерировано Gen Strong ботом • " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));

        FontMetrics footerMetrics = graphics.getFontMetrics();
        int footerX = (defaultImageWidth - footerMetrics.stringWidth(footerText)) / 2;
        int footerY = imageHeight - 30;

        graphics.drawString(footerText, footerX, footerY);
    }

    private void drawSimpleImageContent(Graphics2D graphics) {
        graphics.setColor(backgroundColor);
        graphics.fillRect(0, 0, defaultImageWidth, 500);

        graphics.setColor(headerColor);
        graphics.fillRect(0, 0, defaultImageWidth, 100);

        Font titleFont = new Font("Arial", Font.BOLD, 28);
        graphics.setFont(titleFont);
        graphics.setColor(Color.WHITE);

        String title = "ТРЕНИРОВОЧНЫЙ ПЛАН";
        FontMetrics titleMetrics = graphics.getFontMetrics(titleFont);
        int titleX = (defaultImageWidth - titleMetrics.stringWidth(title)) / 2;
        graphics.drawString(title, titleX, 65);

        graphics.setColor(textColor);
        graphics.setFont(new Font("Arial", Font.PLAIN, 18));
        graphics.drawString("Персональная программа тренировок", padding, 160);

        graphics.setFont(new Font("Arial", Font.BOLD, 22));
        graphics.drawString("Gen Strong", padding, 200);

        graphics.setColor(footerTextColor);
        graphics.setFont(new Font("Arial", Font.ITALIC, 16));

        String footerText = "Сгенерировано Gen Strong ботом • " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));

        FontMetrics footerMetrics = graphics.getFontMetrics();
        int footerX = (defaultImageWidth - footerMetrics.stringWidth(footerText)) / 2;
        int footerY = 450;

        graphics.drawString(footerText, footerX, footerY);
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    double value = cell.getNumericCellValue();
                    if (value == Math.floor(value)) {
                        return String.format("%.0f", value);
                    } else {
                        return String.format("%.1f", value);
                    }
                }
            case BOOLEAN:
                return Boolean.toString(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    Workbook workbook = cell.getSheet().getWorkbook();
                    FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
                    CellValue cellValue = evaluator.evaluate(cell);

                    switch (cellValue.getCellType()) {
                        case NUMERIC:
                            double value = cellValue.getNumberValue();
                            if (value == Math.floor(value)) {
                                return String.format("%.0f", value);
                            } else {
                                return String.format("%.1f", value);
                            }
                        case STRING:
                            return cellValue.getStringValue();
                        default:
                            return cell.getCellFormula();
                    }
                } catch (Exception e) {
                    log.warn("Не удалось вычислить формулу в ячейке: {}", e.getMessage());
                    return cell.getCellFormula();
                }
            default:
                return "";
        }
    }

    private String trimTextToFit(String text, FontMetrics metrics, int maxWidth) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        if (metrics.stringWidth(text) <= maxWidth) {
            return text;
        }

        String ellipsis = "...";
        int ellipsisWidth = metrics.stringWidth(ellipsis);

        for (int i = text.length() - 1; i > 0; i--) {
            String substring = text.substring(0, i);
            if (metrics.stringWidth(substring) + ellipsisWidth <= maxWidth) {
                return substring + ellipsis;
            }
        }

        return ellipsis;
    }
}