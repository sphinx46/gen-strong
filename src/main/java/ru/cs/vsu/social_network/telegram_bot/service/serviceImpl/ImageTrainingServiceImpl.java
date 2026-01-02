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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Реализация сервиса для генерации изображений тренировочных планов.
 * Конвертирует Excel таблицы тренировочных планов в изображения.
 * Оптимизирован для больших таблиц 50x50 с кешированием.
 */
@Slf4j
@Service
public class ImageTrainingServiceImpl implements ImageTrainingService {

    @Value("${training.image.output.dir:generatedTrainingImages}")
    private String imageOutputDirPath;

    @Value("${training.image.format:png}")
    private String defaultImageFormat;

    @Value("${training.image.compression.quality:0.85}")
    private float compressionQuality;

    @Value("${training.image.cell.height:60}")
    private int cellHeight;

    @Value("${training.image.header.height:70}")
    private int headerHeight;

    @Value("${training.image.footer.height:40}")
    private int footerHeight;

    @Value("${training.image.padding:30}")
    private int padding;

    @Value("${training.image.font.size.title:24}")
    private int titleFontSize;

    @Value("${training.image.font.size.header:14}")
    private int headerFontSize;

    @Value("${training.image.font.size.cell:10}")
    private int cellFontSize;

    @Value("${training.image.min.column.width:80}")
    private int minColumnWidth;

    @Value("${training.image.max.columns:50}")
    private int maxColumns;

    @Value("${training.image.max.rows:50}")
    private int maxRows;

    @Value("${training.image.cache.enabled:true}")
    private boolean cacheEnabled;

    @Value("${training.image.cache.ttl.minutes:60}")
    private long cacheTTLMinutes;

    private final ExcelTrainingService excelTrainingService;
    private final Color backgroundColor;
    private final Color headerColor;
    private final Color tableHeaderColor;
    private final Color cellBorderColor;
    private final Color oddRowColor;
    private final Color evenRowColor;
    private final Color horizontalTextColor;
    private final Color verticalTextColor;
    private final Color footerTextColor;

    private final ConcurrentHashMap<String, CachedImage> imageCache;

    /**
     * Класс для хранения кешированных изображений с временем создания.
     */
    private static class CachedImage {
        final BufferedImage image;
        final long creationTime;
        final String filePath;

        CachedImage(BufferedImage image, String filePath) {
            this.image = image;
            this.creationTime = System.currentTimeMillis();
            this.filePath = filePath;
        }

        boolean isExpired(long ttlMillis) {
            return System.currentTimeMillis() - creationTime > ttlMillis;
        }
    }

    /**
     * Конструктор сервиса генерации изображений.
     *
     * @param excelTrainingService сервис для генерации Excel файлов
     */
    public ImageTrainingServiceImpl(ExcelTrainingService excelTrainingService) {
        this.excelTrainingService = excelTrainingService;
        this.backgroundColor = new Color(250, 250, 250);
        this.headerColor = new Color(52, 152, 219);
        this.tableHeaderColor = new Color(41, 128, 185);
        this.cellBorderColor = new Color(220, 220, 220);
        this.oddRowColor = new Color(255, 255, 255);
        this.evenRowColor = new Color(245, 245, 245);
        this.horizontalTextColor = new Color(220, 0, 0);
        this.verticalTextColor = Color.BLACK;
        this.footerTextColor = new Color(100, 100, 100);
        this.imageCache = new ConcurrentHashMap<>();
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

            if (image == null) {
                log.error("{}_ИЗОБРАЖЕНИЕ_НЕ_СОЗДАНО: ошибка при создании изображения", logPrefix);
                throw new GenerateTrainingPlanException("Не удалось создать изображение");
            }

            log.info("{}_ИЗОБРАЖЕНИЕ_СОЗДАНО: размер {}x{} пикселей",
                    logPrefix, image.getWidth(), image.getHeight());

            Path outputPath = createOutputPath(userId);
            log.info("{}_СОХРАНЕНИЕ_ИЗОБРАЖЕНИЯ: {}", logPrefix, outputPath.toAbsolutePath());

            saveImageWithCompression(image, outputPath);
            long fileSize = Files.size(outputPath);
            log.info("{}_ИЗОБРАЖЕНИЕ_СОХРАНЕНО: размер {} байт, путь: {}",
                    logPrefix, fileSize, outputPath.toAbsolutePath());

            File resultImage = outputPath.toFile();

            if (cacheEnabled) {
                String cacheKey = generateCacheKey(excelFile, userBenchPressRequest.getMaxBenchPress());
                imageCache.put(cacheKey, new CachedImage(image, outputPath.toString()));
                log.info("{}_ИЗОБРАЖЕНИЕ_ЗАКЕШИРОВАНО: ключ {}", logPrefix, cacheKey);
            }

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

        if (cacheEnabled) {
            String cacheKey = generateCacheKey(excelFile, null);
            CachedImage cached = imageCache.get(cacheKey);

            if (cached != null && !cached.isExpired(TimeUnit.MINUTES.toMillis(cacheTTLMinutes))) {
                log.info("{}_КЕШ_ПОПАДАНИЕ: изображение загружено из кеша", logPrefix);
                return cached.image;
            }
        }

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

                if (rowCount > maxRows) {
                    log.warn("{}_ТАБЛИЦА_СЛИШКОМ_БОЛЬШАЯ: строк {} > {}, будет обрезано",
                            logPrefix, rowCount, maxRows);
                }

                if (columnCount > maxColumns) {
                    log.warn("{}_ТАБЛИЦА_СЛИШКОМ_ШИРОКАЯ: колонок {} > {}, будет обрезано",
                            logPrefix, columnCount, maxColumns);
                }

                int actualRows = Math.min(rowCount, maxRows);
                int actualColumns = Math.min(columnCount, maxColumns);

                int columnWidth = calculateOptimalColumnWidth(sheet, actualColumns, actualRows);
                int tableWidth = columnWidth * actualColumns;

                if (tableWidth > 10000) {
                    log.warn("{}_ТАБЛИЦА_СЛИШКОМ_ШИРОКАЯ: {}px, применяем масштабирование",
                            logPrefix, tableWidth);
                    double scale = 10000.0 / tableWidth;
                    columnWidth = (int) (columnWidth * scale);
                    tableWidth = columnWidth * actualColumns;
                    log.info("{}_МАСШТАБИРОВАНИЕ_ШИРИНЫ: коэффициент {}, новая ширина столбца {}px",
                            logPrefix, scale, columnWidth);
                }

                int imageWidth = Math.min(tableWidth + (2 * padding), 12000);
                int imageHeight = headerHeight + (actualRows * cellHeight) + footerHeight + (2 * padding);

                if (imageWidth * imageHeight > 40000000) {
                    log.warn("{}_ИЗОБРАЖЕНИЕ_СЛИШКОМ_БОЛЬШОЕ: {}x{} = {}px, применяем многоуровневую отрисовку",
                            logPrefix, imageWidth, imageHeight, imageWidth * imageHeight);

                    BufferedImage image = renderLargeImageOptimized(sheet, actualColumns, actualRows,
                            imageWidth, imageHeight, columnWidth);

                    if (image != null) {
                        log.info("{}_МНОГОУРОВНЕВАЯ_ОТРИСОВКА_УСПЕХ: изображение создано {}x{}",
                                logPrefix, image.getWidth(), image.getHeight());
                        return image;
                    }
                }

                log.info("{}_РАСЧЕТ_РАЗМЕРОВ: изображение {}x{}, таблица {}px, столбец {}px",
                        logPrefix, imageWidth, imageHeight, tableWidth, columnWidth);

                BufferedImage image = new BufferedImage(imageWidth, imageHeight,
                        BufferedImage.TYPE_INT_RGB);
                Graphics2D graphics = image.createGraphics();

                try {
                    configureGraphicsQuality(graphics);
                    drawImageContent(graphics, sheet, actualColumns, actualRows,
                            imageHeight, imageWidth, columnWidth);
                } finally {
                    graphics.dispose();
                }

                log.info("{}_ИЗОБРАЖЕНИЕ_СОЗДАНО: размер {}x{}", logPrefix,
                        image.getWidth(), image.getHeight());

                return image;

            } finally {
                workbook.close();
            }

        } catch (OutOfMemoryError e) {
            log.error("{}_ПЕРЕПОЛНЕНИЕ_ПАМЯТИ: {}", logPrefix, e.getMessage());

            System.gc();
            try {
                Thread.sleep(100);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }

            throw new GenerateTrainingPlanException("Недостаточно памяти для создания изображения. Таблица слишком большая.");
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

            int imageWidth = 800;
            int imageHeight = 400;

            BufferedImage image = new BufferedImage(imageWidth, imageHeight,
                    BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();

            try {
                configureGraphicsQuality(graphics);
                drawSimpleImageContent(graphics, imageWidth, imageHeight);
            } finally {
                graphics.dispose();
            }

            saveImageWithCompression(image, outputPath);
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
     * Создает путь для сохранения выходного изображения.
     *
     * @param userId идентификатор пользователя
     * @return путь к файлу изображения
     * @throws IOException если не удается создать директорию
     */
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

    /**
     * Сохраняет изображение с компрессией.
     *
     * @param image      буферизированное изображение
     * @param outputPath путь для сохранения
     * @throws IOException если возникает ошибка ввода-вывода
     */
    private void saveImageWithCompression(BufferedImage image, Path outputPath) throws IOException {
        try (FileOutputStream imageOut = new FileOutputStream(outputPath.toFile())) {
            if ("jpg".equalsIgnoreCase(defaultImageFormat) || "jpeg".equalsIgnoreCase(defaultImageFormat)) {
                BufferedImage rgbImage = new BufferedImage(image.getWidth(), image.getHeight(),
                        BufferedImage.TYPE_INT_RGB);
                Graphics2D g = rgbImage.createGraphics();
                g.drawImage(image, 0, 0, null);
                g.dispose();

                ImageIO.write(rgbImage, "jpg", imageOut);
            } else {
                boolean written = ImageIO.write(image, defaultImageFormat, imageOut);
                if (!written) {
                    throw new GenerateTrainingPlanException("Формат изображения не поддерживается: " + defaultImageFormat);
                }
            }
        }
    }

    /**
     * Удаляет временный Excel файл.
     *
     * @param logPrefix  префикс для логов
     * @param excelFile  файл для удаления
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
     * @param excelFile Excel файл
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
     * Создает рабочую книгу на основе файла Excel.
     *
     * @param excelFile    Excel файл
     * @param inputStream  входной поток
     * @param logPrefix    префикс для логов
     * @return рабочая книга Excel
     * @throws IOException если возникает ошибка ввода-вывода
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
     * Получает первый лист из рабочей книги.
     *
     * @param workbook  рабочая книга Excel
     * @param logPrefix префикс для логов
     * @return первый лист книги
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
     * Вычисляет количество столбцов в таблице.
     *
     * @param sheet     лист Excel
     * @param rowCount  количество строк
     * @return количество столбцов
     */
    private int calculateColumnCount(Sheet sheet, int rowCount) {
        int columnCount = 0;
        int rowsToCheck = Math.min(rowCount, 10);

        for (int i = 0; i < rowsToCheck; i++) {
            Row row = sheet.getRow(i);
            if (row != null) {
                columnCount = Math.max(columnCount, row.getLastCellNum());
            }
        }
        return columnCount;
    }

    /**
     * Вычисляет оптимальную ширину столбцов на основе содержимого.
     *
     * @param sheet       лист Excel
     * @param columnCount количество столбцов
     * @param rowCount    количество строк для анализа
     * @return оптимальная ширина столбца в пикселях
     */
    private int calculateOptimalColumnWidth(Sheet sheet, int columnCount, int rowCount) {
        int maxCellWidth = minColumnWidth;
        Font tempFont = new Font("Arial", Font.PLAIN, cellFontSize);

        try {
            BufferedImage tempImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
            Graphics2D tempGraphics = tempImage.createGraphics();
            tempGraphics.setFont(tempFont);
            FontMetrics metrics = tempGraphics.getFontMetrics();

            int rowsToAnalyze = Math.min(rowCount, 10);

            for (int i = 0; i < rowsToAnalyze; i++) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    for (int j = 0; j < columnCount; j++) {
                        Cell cell = row.getCell(j);
                        String cellValue = getCellValueAsString(cell);
                        if (cellValue != null && !cellValue.isEmpty()) {
                            int textWidth = metrics.stringWidth(cellValue);
                            maxCellWidth = Math.max(maxCellWidth, textWidth + 20);
                        }
                    }
                }
            }

            tempGraphics.dispose();
        } catch (Exception e) {
            log.warn("Не удалось рассчитать оптимальную ширину столбцов, используется значение по умолчанию: {}",
                    e.getMessage());
            return minColumnWidth;
        }

        return Math.min(maxCellWidth, 150);
    }

    /**
     * Настраивает качество графики.
     *
     * @param graphics объект Graphics2D
     */
    private void configureGraphicsQuality(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        try {
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        } catch (Exception e) {
            log.warn("Не удалось установить сглаживание текста: {}", e.getMessage());
        }

        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        try {
            graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        } catch (Exception e) {
            log.warn("Не удалось установить дробные метрики: {}", e.getMessage());
        }
    }

    /**
     * Отрисовывает основное содержимое изображения.
     *
     * @param graphics          объект Graphics2D
     * @param sheet             лист Excel
     * @param columnCount       количество столбцов
     * @param maxRowsToDisplay  максимальное количество строк для отображения
     * @param imageHeight       высота изображения
     * @param imageWidth        ширина изображения
     * @param columnWidth       ширина столбца
     */
    private void drawImageContent(Graphics2D graphics, Sheet sheet, int columnCount,
                                  int maxRowsToDisplay, int imageHeight, int imageWidth, int columnWidth) {
        graphics.setColor(backgroundColor);
        graphics.fillRect(0, 0, imageWidth, imageHeight);

        drawHeader(graphics, imageWidth);
        drawTable(graphics, sheet, columnCount, maxRowsToDisplay, imageWidth, columnWidth);
        drawFooter(graphics, imageHeight, imageWidth);
    }

    /**
     * Отрисовывает заголовок изображения.
     *
     * @param graphics   объект Graphics2D
     * @param imageWidth ширина изображения
     */
    private void drawHeader(Graphics2D graphics, int imageWidth) {
        graphics.setColor(headerColor);
        graphics.fillRect(0, 0, imageWidth, headerHeight);

        Font titleFont = new Font("Arial", Font.BOLD, titleFontSize);
        graphics.setFont(titleFont);
        graphics.setColor(Color.WHITE);

        String title = "ТРЕНИРОВОЧНЫЙ ПЛАН";
        FontMetrics titleMetrics = graphics.getFontMetrics(titleFont);
        int titleX = (imageWidth - titleMetrics.stringWidth(title)) / 2;
        graphics.drawString(title, titleX, headerHeight - 20);
    }

    /**
     * Отрисовывает таблицу на изображении.
     *
     * @param graphics          объект Graphics2D
     * @param sheet             лист Excel
     * @param columnCount       количество столбцов
     * @param maxRowsToDisplay  максимальное количество строк для отображения
     * @param imageWidth        ширина изображения
     * @param columnWidth       ширина столбца
     */
    private void drawTable(Graphics2D graphics, Sheet sheet, int columnCount,
                           int maxRowsToDisplay, int imageWidth, int columnWidth) {
        int tableWidth = columnWidth * columnCount;
        int tableStartY = headerHeight + padding;
        int tableStartX = padding;

        if (tableWidth + (2 * padding) > imageWidth) {
            tableStartX = (imageWidth - tableWidth) / 2;
        }

        drawTableHeader(graphics, sheet, tableWidth, columnWidth, tableStartY, columnCount, tableStartX);
        drawTableRows(graphics, sheet, tableWidth, columnWidth, tableStartY, maxRowsToDisplay, columnCount, tableStartX);
    }

    /**
     * Отрисовывает заголовок таблицы.
     *
     * @param graphics     объект Graphics2D
     * @param sheet        лист Excel
     * @param tableWidth   ширина таблицы
     * @param colWidth     ширина столбца
     * @param tableStartY  начальная позиция Y для таблицы
     * @param columnCount  количество столбцов
     * @param tableStartX  начальная позиция X для таблицы
     */
    private void drawTableHeader(Graphics2D graphics, Sheet sheet, int tableWidth, int colWidth,
                                 int tableStartY, int columnCount, int tableStartX) {
        graphics.setColor(tableHeaderColor);
        graphics.fillRect(tableStartX, tableStartY, tableWidth, cellHeight);

        graphics.setColor(Color.WHITE);
        Font headerFont = new Font("Arial", Font.BOLD, headerFontSize);
        graphics.setFont(headerFont);

        Row headerRow = sheet.getRow(0);
        if (headerRow != null) {
            for (int j = 0; j < columnCount; j++) {
                Cell cell = headerRow.getCell(j);
                int x = tableStartX + j * colWidth;
                int y = tableStartY + cellHeight - 10;

                String cellValue = getCellValueAsString(cell);
                if (cellValue != null && !cellValue.isEmpty()) {
                    FontMetrics metrics = graphics.getFontMetrics();
                    String trimmedValue = trimTextToFit(cellValue, metrics, colWidth - 15);
                    int textX = x + (colWidth - metrics.stringWidth(trimmedValue)) / 2;
                    graphics.drawString(trimmedValue, textX, y);
                }
            }
        }

        graphics.setColor(cellBorderColor);
        graphics.drawRect(tableStartX, tableStartY, tableWidth, cellHeight);
    }

    /**
     * Отрисовывает строки таблицы.
     *
     * @param graphics          объект Graphics2D
     * @param sheet             лист Excel
     * @param tableWidth        ширина таблицы
     * @param colWidth          ширина столбца
     * @param tableStartY       начальная позиция Y для таблицы
     * @param maxRowsToDisplay  максимальное количество строк для отображения
     * @param columnCount       количество столбцов
     * @param tableStartX       начальная позиция X для таблицы
     */
    private void drawTableRows(Graphics2D graphics, Sheet sheet, int tableWidth, int colWidth,
                               int tableStartY, int maxRowsToDisplay, int columnCount, int tableStartX) {
        Font cellFont = new Font("Arial", Font.PLAIN, cellFontSize);

        for (int i = 1; i < maxRowsToDisplay; i++) {
            Row row = sheet.getRow(i);
            int y = tableStartY + i * cellHeight;

            Color rowColor = (i % 2 == 0) ? evenRowColor : oddRowColor;
            graphics.setColor(rowColor);
            graphics.fillRect(tableStartX, y, tableWidth, cellHeight);

            graphics.setColor(cellBorderColor);
            graphics.drawRect(tableStartX, y, tableWidth, cellHeight);

            if (row != null) {
                for (int j = 0; j < columnCount; j++) {
                    Cell cell = row.getCell(j);
                    int x = tableStartX + j * colWidth;
                    int textY = y + cellHeight - 10;

                    String cellValue = getCellValueAsString(cell);
                    if (cellValue != null && !cellValue.isEmpty()) {
                        FontMetrics metrics = graphics.getFontMetrics(cellFont);

                        if (j == 0) {
                            graphics.setColor(verticalTextColor);
                            graphics.setFont(cellFont);
                            String trimmedValue = trimTextToFit(cellValue, metrics, colWidth - 15);
                            int textX = x + (colWidth - metrics.stringWidth(trimmedValue)) / 2;
                            graphics.drawString(trimmedValue, textX, textY);
                        } else {
                            graphics.setColor(horizontalTextColor);
                            graphics.setFont(cellFont);
                            String trimmedValue = trimTextToFit(cellValue, metrics, colWidth - 15);
                            int textX = x + (colWidth - metrics.stringWidth(trimmedValue)) / 2;
                            graphics.drawString(trimmedValue, textX, textY);
                        }
                    }

                    graphics.setColor(cellBorderColor);
                    graphics.drawLine(x, y, x, y + cellHeight);
                }
            }

            graphics.setColor(cellBorderColor);
            graphics.drawLine(tableStartX, y + cellHeight, tableStartX + tableWidth, y + cellHeight);
        }

        for (int j = 0; j <= columnCount; j++) {
            int x = tableStartX + j * colWidth;
            graphics.drawLine(x, tableStartY, x, tableStartY + maxRowsToDisplay * cellHeight);
        }
    }

    /**
     * Отрисовывает нижний колонтитул изображения.
     *
     * @param graphics    объект Graphics2D
     * @param imageHeight высота изображения
     * @param imageWidth  ширина изображения
     */
    private void drawFooter(Graphics2D graphics, int imageHeight, int imageWidth) {
        graphics.setColor(footerTextColor);
        Font footerFont = new Font("Arial", Font.ITALIC, 10);
        graphics.setFont(footerFont);

        String footerText = "Сгенерировано Gen Strong ботом • " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));

        FontMetrics footerMetrics = graphics.getFontMetrics();
        int footerX = (imageWidth - footerMetrics.stringWidth(footerText)) / 2;
        int footerY = imageHeight - 15;

        graphics.drawString(footerText, footerX, footerY);
    }

    /**
     * Отрисовывает простое изображение с данными.
     *
     * @param graphics   объект Graphics2D
     * @param imageWidth ширина изображения
     * @param imageHeight высота изображения
     */
    private void drawSimpleImageContent(Graphics2D graphics, int imageWidth, int imageHeight) {
        graphics.setColor(backgroundColor);
        graphics.fillRect(0, 0, imageWidth, imageHeight);

        graphics.setColor(headerColor);
        graphics.fillRect(0, 0, imageWidth, 80);

        Font titleFont = new Font("Arial", Font.BOLD, 24);
        graphics.setFont(titleFont);
        graphics.setColor(Color.WHITE);

        String title = "ТРЕНИРОВОЧНЫЙ ПЛАН";
        FontMetrics titleMetrics = graphics.getFontMetrics(titleFont);
        int titleX = (imageWidth - titleMetrics.stringWidth(title)) / 2;
        graphics.drawString(title, titleX, 50);

        graphics.setColor(verticalTextColor);
        graphics.setFont(new Font("Arial", Font.PLAIN, 14));
        graphics.drawString("Персональная программа тренировок", 50, 140);

        graphics.setFont(new Font("Arial", Font.BOLD, 18));
        graphics.drawString("Gen Strong", 50, 170);

        graphics.setColor(footerTextColor);
        graphics.setFont(new Font("Arial", Font.ITALIC, 12));

        String footerText = "Сгенерировано Gen Strong ботом • " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));

        FontMetrics footerMetrics = graphics.getFontMetrics();
        int footerX = (imageWidth - footerMetrics.stringWidth(footerText)) / 2;
        int footerY = 350;

        graphics.drawString(footerText, footerX, footerY);
    }

    /**
     * Преобразует значение ячейки Excel в строку.
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

    /**
     * Обрезает текст, чтобы он помещался в заданную ширину.
     *
     * @param text     исходный текст
     * @param metrics  метрики шрифта
     * @param maxWidth максимальная ширина
     * @return обрезанный текст
     */
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

    /**
     * Генерирует ключ для кеширования изображений.
     *
     * @param excelFile     файл Excel
     * @param maxBenchPress максимальный жим лежа
     * @return ключ кеша
     */
    private String generateCacheKey(File excelFile, Double maxBenchPress) {
        String fileName = excelFile.getName();
        long fileSize = excelFile.length();
        long lastModified = excelFile.lastModified();

        if (maxBenchPress != null) {
            return String.format("%s_%d_%d_%.1f", fileName, fileSize, lastModified, maxBenchPress);
        } else {
            return String.format("%s_%d_%d", fileName, fileSize, lastModified);
        }
    }

    /**
     * Очищает просроченные записи из кеша.
     */
    public void cleanupCache() {
        if (!cacheEnabled) {
            return;
        }

        long ttlMillis = TimeUnit.MINUTES.toMillis(cacheTTLMinutes);
        int initialSize = imageCache.size();

        imageCache.entrySet().removeIf(entry -> entry.getValue().isExpired(ttlMillis));

        int removedCount = initialSize - imageCache.size();
        if (removedCount > 0) {
            log.info("КЕШ_ОЧИСТКА: удалено {} просроченных записей", removedCount);
        }

        System.gc();
    }

    /**
     * Оптимизированная отрисовка больших изображений с многоуровневым подходом.
     *
     * @param sheet        лист Excel
     * @param columnCount  количество столбцов
     * @param rowCount     количество строк
     * @param imageWidth   ширина изображения
     * @param imageHeight  высота изображения
     * @param columnWidth  ширина столбца
     * @return буферизированное изображение
     */
    private BufferedImage renderLargeImageOptimized(Sheet sheet, int columnCount, int rowCount,
                                                    int imageWidth, int imageHeight, int columnWidth) {
        try {
            log.info("МНОГОУРОВНЕВАЯ_ОТРИСОВКА_НАЧАЛО: {}x{}", imageWidth, imageHeight);

            BufferedImage image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();

            try {
                configureGraphicsQuality(graphics);

                graphics.setColor(backgroundColor);
                graphics.fillRect(0, 0, imageWidth, imageHeight);

                drawHeader(graphics, imageWidth);

                int tableWidth = columnWidth * columnCount;
                int tableStartY = headerHeight + padding;
                int tableStartX = padding;

                if (tableWidth + (2 * padding) > imageWidth) {
                    tableStartX = (imageWidth - tableWidth) / 2;
                }

                graphics.setColor(tableHeaderColor);
                graphics.fillRect(tableStartX, tableStartY, tableWidth, cellHeight);

                graphics.setColor(Color.WHITE);
                Font headerFont = new Font("Arial", Font.BOLD, headerFontSize);
                graphics.setFont(headerFont);

                Row headerRow = sheet.getRow(0);
                if (headerRow != null) {
                    for (int j = 0; j < columnCount; j++) {
                        Cell cell = headerRow.getCell(j);
                        int x = tableStartX + j * columnWidth;
                        int y = tableStartY + cellHeight - 10;

                        String cellValue = getCellValueAsString(cell);
                        if (cellValue != null && !cellValue.isEmpty()) {
                            FontMetrics metrics = graphics.getFontMetrics();
                            String trimmedValue = trimTextToFit(cellValue, metrics, columnWidth - 15);
                            int textX = x + (columnWidth - metrics.stringWidth(trimmedValue)) / 2;
                            graphics.drawString(trimmedValue, textX, y);
                        }
                    }
                }

                graphics.setColor(cellBorderColor);
                graphics.drawRect(tableStartX, tableStartY, tableWidth, cellHeight);

                Font cellFont = new Font("Arial", Font.PLAIN, cellFontSize);
                graphics.setFont(cellFont);

                int chunkSize = 10;
                for (int chunkStart = 1; chunkStart < rowCount; chunkStart += chunkSize) {
                    int chunkEnd = Math.min(chunkStart + chunkSize, rowCount);

                    for (int i = chunkStart; i < chunkEnd; i++) {
                        Row row = sheet.getRow(i);
                        int y = tableStartY + i * cellHeight;

                        Color rowColor = (i % 2 == 0) ? evenRowColor : oddRowColor;
                        graphics.setColor(rowColor);
                        graphics.fillRect(tableStartX, y, tableWidth, cellHeight);

                        graphics.setColor(cellBorderColor);
                        graphics.drawRect(tableStartX, y, tableWidth, cellHeight);

                        if (row != null) {
                            for (int j = 0; j < columnCount; j++) {
                                Cell cell = row.getCell(j);
                                int x = tableStartX + j * columnWidth;
                                int textY = y + cellHeight - 10;

                                String cellValue = getCellValueAsString(cell);
                                if (cellValue != null && !cellValue.isEmpty()) {
                                    FontMetrics metrics = graphics.getFontMetrics(cellFont);

                                    if (j == 0) {
                                        graphics.setColor(verticalTextColor);
                                        graphics.setFont(cellFont);
                                        String trimmedValue = trimTextToFit(cellValue, metrics, columnWidth - 15);
                                        int textX = x + (columnWidth - metrics.stringWidth(trimmedValue)) / 2;
                                        graphics.drawString(trimmedValue, textX, textY);
                                    } else {
                                        graphics.setColor(horizontalTextColor);
                                        graphics.setFont(cellFont);
                                        String trimmedValue = trimTextToFit(cellValue, metrics, columnWidth - 15);
                                        int textX = x + (columnWidth - metrics.stringWidth(trimmedValue)) / 2;
                                        graphics.drawString(trimmedValue, textX, textY);
                                    }
                                }

                                graphics.setColor(cellBorderColor);
                                graphics.drawLine(x, y, x, y + cellHeight);
                            }
                        }

                        graphics.setColor(cellBorderColor);
                        graphics.drawLine(tableStartX, y + cellHeight, tableStartX + tableWidth, y + cellHeight);
                    }

                    if (chunkStart % 50 == 0) {
                        log.debug("МНОГОУРОВНЕВАЯ_ОТРИСОВКА_ПРОГРЕСС: обработано {} строк из {}", chunkStart, rowCount);
                    }
                }

                for (int j = 0; j <= columnCount; j++) {
                    int x = tableStartX + j * columnWidth;
                    graphics.drawLine(x, tableStartY, x, tableStartY + rowCount * cellHeight);
                }

                drawFooter(graphics, imageHeight, imageWidth);

            } finally {
                graphics.dispose();
            }

            log.info("МНОГОУРОВНЕВАЯ_ОТРИСОВКА_УСПЕХ: изображение создано");
            return image;

        } catch (Exception e) {
            log.error("МНОГОУРОВНЕВАЯ_ОТРИСОВКА_ОШИБКА: {}", e.getMessage(), e);
            return null;
        }
    }
}