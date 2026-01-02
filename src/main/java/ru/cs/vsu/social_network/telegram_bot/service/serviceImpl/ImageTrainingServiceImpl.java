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

    @Value("${training.image.width:1200}")
    private int defaultImageWidth;

    @Value("${training.image.cell.height:40}")
    private int cellHeight;

    @Value("${training.image.header.height:60}")
    private int headerHeight;

    @Value("${training.image.footer.height:40}")
    private int footerHeight;

    @Value("${training.image.padding:30}")
    private int padding;

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
        this.textColor = new Color(50, 50, 50);
        this.footerTextColor = new Color(150, 150, 150);
    }

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
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

                int maxRowsToDisplay = Math.min(rowCount, 20);
                int imageHeight = headerHeight + (maxRowsToDisplay * cellHeight) + footerHeight;

                BufferedImage image = new BufferedImage(defaultImageWidth, imageHeight,
                        BufferedImage.TYPE_INT_RGB);
                Graphics2D graphics = image.createGraphics();

                try {
                    configureGraphicsQuality(graphics);
                    drawImageContent(graphics, sheet, columnCount, maxRowsToDisplay, imageHeight);
                    graphics.dispose();
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

    /**
     * {@inheritDoc}
     */
    @Override
    public File generateImageFromData(UUID userId, Double maxBenchPress, String trainingCycleName) {
        log.info("ИЗОБРАЖЕНИЕ_ИЗ_ДАННЫХ_ГЕНЕРАЦИЯ_НАЧАЛО: пользователь {}, жим лежа: {} кг, цикл: {}",
                userId, maxBenchPress, trainingCycleName);

        final String logPrefix = "ИЗОБРАЖЕНИЕ_ИЗ_ДАННЫХ";

        try {
            Path outputPath = createOutputPath(userId);

            BufferedImage image = new BufferedImage(defaultImageWidth, 400,
                    BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();

            try {
                configureGraphicsQuality(graphics);
                drawSimpleImageContent(graphics);
                graphics.dispose();
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

    /**
     * Создает путь для сохранения изображения.
     *
     * @param userId идентификатор пользователя
     * @return путь к файлу изображения
     * @throws IOException если не удалось создать директорию
     */
    private Path createOutputPath(UUID userId) throws IOException {
        Path imageOutputDir = Paths.get(imageOutputDirPath);
        if (!Files.exists(imageOutputDir)) {
            Files.createDirectories(imageOutputDir);
            log.info("ДИРЕКТОРИЯ_СОЗДАНА: {}", imageOutputDir.toAbsolutePath());
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = String.format("training_plan_%s_%s.%s", userId, timestamp, defaultImageFormat);
        return imageOutputDir.resolve(filename);
    }

    /**
     * Сохраняет изображение в файл.
     *
     * @param image изображение для сохранения
     * @param outputPath путь для сохранения
     * @throws IOException если произошла ошибка ввода-вывода
     */
    private void saveImageToFile(BufferedImage image, Path outputPath) throws IOException {
        try (FileOutputStream imageOut = new FileOutputStream(outputPath.toFile())) {
            boolean written = ImageIO.write(image, defaultImageFormat, imageOut);

            if (!written) {
                throw new GenerateTrainingPlanException("Формат изображения не поддерживается: " + defaultImageFormat);
            }
        }
    }

    /**
     * Удаляет временный Excel файл.
     *
     * @param logPrefix префикс для логов
     * @param excelFile файл для удаления
     */
    private void deleteTempExcelFile(String logPrefix, File excelFile) {
        log.info("{}_ВРЕМЕННЫЙ_ФАЙЛ_УДАЛЕНИЕ: Excel файл {}", logPrefix, excelFile.getAbsolutePath());
        if (excelFile.delete()) {
            log.info("{}_ВРЕМЕННЫЙ_ФАЙЛ_УДАЛЕН", logPrefix);
        } else {
            log.warn("{}_ВРЕМЕННЫЙ_ФАЙЛ_НЕ_УДАЛЕН: требуется ручное удаление", logPrefix);
        }
    }

    /**
     * Проверяет доступность Excel файла.
     *
     * @param excelFile файл для проверки
     * @param logPrefix префикс для логов
     */
    private void validateExcelFile(File excelFile, String logPrefix) {
        if (!excelFile.exists() || !excelFile.canRead()) {
            log.error("{}_ФАЙЛ_НЕДОСТУПЕН: {}", logPrefix, excelFile.getAbsolutePath());
            throw new GenerateTrainingPlanException("Excel файл недоступен для чтения");
        }
        log.info("{}_EXCEL_ЧТЕНИЕ: размер файла {} байт", logPrefix, excelFile.length());
    }

    /**
     * Создает Workbook из файла Excel.
     *
     * @param excelFile файл Excel
     * @param inputStream входной поток
     * @param logPrefix префикс для логов
     * @return Workbook
     * @throws IOException если произошла ошибка ввода-вывода
     */
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

    /**
     * Получает первый лист из Workbook.
     *
     * @param workbook книга Excel
     * @param logPrefix префикс для логов
     * @return первый лист
     */
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

    /**
     * Вычисляет количество колонок в листе.
     *
     * @param sheet лист Excel
     * @param rowCount количество строк
     * @return количество колонок
     */
    private int calculateColumnCount(Sheet sheet, int rowCount) {
        int columnCount = 0;
        for (int i = 0; i < rowCount; i++) {
            Row row = sheet.getRow(i);
            if (row != null) {
                columnCount = Math.max(columnCount, row.getLastCellNum());
            }
        }
        return columnCount;
    }

    /**
     * Настраивает качество рендеринга для Graphics2D.
     *
     * @param graphics объект Graphics2D
     */
    private void configureGraphicsQuality(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    /**
     * Рисует содержимое изображения из данных Excel.
     *
     * @param graphics объект Graphics2D
     * @param sheet лист Excel
     * @param columnCount количество колонок
     * @param maxRowsToDisplay максимальное количество строк для отображения
     * @param imageHeight высота изображения
     */
    private void drawImageContent(Graphics2D graphics, Sheet sheet, int columnCount,
                                  int maxRowsToDisplay, int imageHeight) {
        graphics.setColor(backgroundColor);
        graphics.fillRect(0, 0, defaultImageWidth, imageHeight);

        drawHeader(graphics);
        drawTable(graphics, sheet, columnCount, maxRowsToDisplay);
        drawFooter(graphics, imageHeight);
    }

    /**
     * Рисует заголовок изображения.
     *
     * @param graphics объект Graphics2D
     */
    private void drawHeader(Graphics2D graphics) {
        graphics.setColor(headerColor);
        graphics.fillRect(0, 0, defaultImageWidth, headerHeight);

        Font titleFont = new Font("Arial", Font.BOLD, 24);
        graphics.setFont(titleFont);
        graphics.setColor(Color.WHITE);

        String title = "ТРЕНИРОВОЧНЫЙ ПЛАН";
        FontMetrics titleMetrics = graphics.getFontMetrics(titleFont);
        int titleX = (defaultImageWidth - titleMetrics.stringWidth(title)) / 2;
        graphics.drawString(title, titleX, headerHeight - 20);
    }

    /**
     * Рисует таблицу с данными.
     *
     * @param graphics объект Graphics2D
     * @param sheet лист Excel
     * @param columnCount количество колонок
     * @param maxRowsToDisplay максимальное количество строк для отображения
     */
    private void drawTable(Graphics2D graphics, Sheet sheet, int columnCount, int maxRowsToDisplay) {
        int tableWidth = defaultImageWidth - (2 * padding);
        int colWidth = tableWidth / Math.max(columnCount, 1);
        int tableStartY = headerHeight + padding;

        drawTableHeader(graphics, sheet, tableWidth, colWidth, tableStartY, columnCount);
        drawTableRows(graphics, sheet, tableWidth, colWidth, tableStartY, maxRowsToDisplay, columnCount);
    }

    /**
     * Рисует заголовок таблицы.
     *
     * @param graphics объект Graphics2D
     * @param sheet лист Excel
     * @param tableWidth ширина таблицы
     * @param colWidth ширина колонки
     * @param tableStartY начальная координата Y таблицы
     * @param columnCount количество колонок
     */
    private void drawTableHeader(Graphics2D graphics, Sheet sheet, int tableWidth, int colWidth,
                                 int tableStartY, int columnCount) {
        graphics.setColor(tableHeaderColor);
        graphics.fillRect(padding, tableStartY, tableWidth, cellHeight);

        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font("Arial", Font.BOLD, 14));

        Row headerRow = sheet.getRow(0);
        if (headerRow != null) {
            for (int j = 0; j < columnCount && j < 10; j++) {
                Cell cell = headerRow.getCell(j);
                int x = padding + j * colWidth;
                int y = tableStartY + cellHeight - 10;

                String cellValue = getCellValueAsString(cell);
                if (cellValue != null && !cellValue.isEmpty()) {
                    FontMetrics metrics = graphics.getFontMetrics();
                    String trimmedValue = trimTextToFit(cellValue, metrics, colWidth - 10);
                    graphics.drawString(trimmedValue, x + 5, y);
                }
            }
        }

        graphics.setColor(cellBorderColor);
        graphics.drawRect(padding, tableStartY, tableWidth, cellHeight);
    }

    /**
     * Рисует строки таблицы.
     *
     * @param graphics объект Graphics2D
     * @param sheet лист Excel
     * @param tableWidth ширина таблицы
     * @param colWidth ширина колонки
     * @param tableStartY начальная координата Y таблицы
     * @param maxRowsToDisplay максимальное количество строк для отображения
     * @param columnCount количество колонок
     */
    private void drawTableRows(Graphics2D graphics, Sheet sheet, int tableWidth, int colWidth,
                               int tableStartY, int maxRowsToDisplay, int columnCount) {
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
                graphics.setFont(new Font("Arial", Font.PLAIN, 12));

                for (int j = 0; j < columnCount && j < 10; j++) {
                    Cell cell = row.getCell(j);
                    int x = padding + j * colWidth;
                    int textY = y + cellHeight - 10;

                    String cellValue = getCellValueAsString(cell);
                    if (cellValue != null && !cellValue.isEmpty()) {
                        FontMetrics metrics = graphics.getFontMetrics();
                        String trimmedValue = trimTextToFit(cellValue, metrics, colWidth - 10);
                        graphics.drawString(trimmedValue, x + 5, textY);
                    }

                    graphics.setColor(cellBorderColor);
                    graphics.drawLine(x, y, x, y + cellHeight);
                }
            }

            for (int j = 0; j <= columnCount; j++) {
                int x = padding + j * colWidth;
                graphics.drawLine(x, tableStartY, x, tableStartY + maxRowsToDisplay * cellHeight);
            }
        }
    }

    /**
     * Рисует подвал изображения.
     *
     * @param graphics объект Graphics2D
     * @param imageHeight высота изображения
     */
    private void drawFooter(Graphics2D graphics, int imageHeight) {
        graphics.setColor(footerTextColor);
        graphics.setFont(new Font("Arial", Font.ITALIC, 12));

        String footerText = "Сгенерировано Gen Strong ботом • " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));

        FontMetrics footerMetrics = graphics.getFontMetrics();
        int footerX = (defaultImageWidth - footerMetrics.stringWidth(footerText)) / 2;
        int footerY = imageHeight - 15;

        graphics.drawString(footerText, footerX, footerY);
    }

    /**
     * Рисует простое содержимое изображения (для метода generateImageFromData).
     *
     * @param graphics объект Graphics2D
     */
    private void drawSimpleImageContent(Graphics2D graphics) {
        graphics.setColor(backgroundColor);
        graphics.fillRect(0, 0, defaultImageWidth, 400);

        graphics.setColor(headerColor);
        graphics.fillRect(0, 0, defaultImageWidth, 80);

        Font titleFont = new Font("Arial", Font.BOLD, 24);
        graphics.setFont(titleFont);
        graphics.setColor(Color.WHITE);

        String title = "ТРЕНИРОВОЧНЫЙ ПЛАН";
        FontMetrics titleMetrics = graphics.getFontMetrics(titleFont);
        int titleX = (defaultImageWidth - titleMetrics.stringWidth(title)) / 2;
        graphics.drawString(title, titleX, 50);

        graphics.setColor(textColor);
        graphics.setFont(new Font("Arial", Font.PLAIN, 14));
        graphics.drawString("Персональная программа тренировок", padding, 120);

        graphics.setFont(new Font("Arial", Font.BOLD, 16));
        graphics.drawString("Gen Strong", padding, 160);

        graphics.setColor(footerTextColor);
        graphics.setFont(new Font("Arial", Font.ITALIC, 12));

        String footerText = "Сгенерировано Gen Strong ботом • " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));

        FontMetrics footerMetrics = graphics.getFontMetrics();
        int footerX = (defaultImageWidth - footerMetrics.stringWidth(footerText)) / 2;
        int footerY = 380;

        graphics.drawString(footerText, footerX, footerY);
    }

    /**
     * Преобразует значение ячейки в строку.
     *
     * @param cell ячейка Excel
     * @return строковое значение ячейки
     */
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
                        return String.format("%.2f", value);
                    }
                }
            case BOOLEAN:
                return Boolean.toString(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return String.format("%.2f", cell.getNumericCellValue());
                } catch (Exception e) {
                    return cell.getCellFormula();
                }
            default:
                return "";
        }
    }

    /**
     * Обрезает текст чтобы он поместился в заданную ширину.
     *
     * @param text исходный текст
     * @param metrics метрики шрифта
     * @param maxWidth максимальная ширина
     * @return обрезанный текст с многоточием если необходимо
     */
    private String trimTextToFit(String text, FontMetrics metrics, int maxWidth) {
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