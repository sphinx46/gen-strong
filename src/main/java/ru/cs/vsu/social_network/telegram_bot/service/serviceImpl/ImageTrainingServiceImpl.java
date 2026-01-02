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
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ImageTrainingServiceImpl implements ImageTrainingService {

    @Value("${training.image.output.dir:generatedTrainingImages}")
    private String imageOutputDirPath;

    @Value("${training.image.format:png}")
    private String defaultImageFormat;

    @Value("${training.image.cell.height:80}")
    private int cellHeight;

    @Value("${training.image.header.height:100}")
    private int headerHeight;

    @Value("${training.image.footer.height:50}")
    private int footerHeight;

    @Value("${training.image.padding:40}")
    private int padding;

    @Value("${training.image.font.size.title:36}")
    private int titleFontSize;

    @Value("${training.image.font.size.header:24}")
    private int headerFontSize;

    @Value("${training.image.font.size.cell:18}")
    private int cellFontSize;

    @Value("${training.image.min.column.width:120}")
    private int minColumnWidth;

    @Value("${training.image.max.columns:50}")
    private int maxColumns;

    @Value("${training.image.max.rows:200}")
    private int maxRows;

    @Value("${training.image.cache.enabled:true}")
    private boolean cacheEnabled;

    @Value("${training.image.cache.ttl.minutes:60}")
    private long cacheTTLMinutes;

    @Value("${training.image.cache.max.size:50}")
    private int cacheMaxSize;

    @Value("${training.image.optimize.empty.columns:true}")
    private boolean optimizeEmptyColumns;

    @Value("${training.image.chunk.size:10}")
    private int chunkSize;

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

    private static class CachedImage {
        final byte[] compressedImageData;
        final long creationTime;
        final String filePath;
        final int width;
        final int height;
        final int actualRows;
        final int actualColumns;

        CachedImage(byte[] compressedImageData, String filePath, int width, int height, int actualRows, int actualColumns) {
            this.compressedImageData = compressedImageData;
            this.creationTime = System.currentTimeMillis();
            this.filePath = filePath;
            this.width = width;
            this.height = height;
            this.actualRows = actualRows;
            this.actualColumns = actualColumns;
        }

        boolean isExpired(long ttlMillis) {
            return System.currentTimeMillis() - creationTime > ttlMillis;
        }
    }

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

    @Override
    public File generateTrainingPlanImage(UUID userId, UserBenchPressRequest userBenchPressRequest) {
        log.info("ИЗОБРАЖЕНИЕ_ТРЕНИРОВОЧНЫЙ_ПЛАН_ГЕНЕРАЦИЯ_НАЧАЛО пользователь {} жим лежа {} кг", userId, userBenchPressRequest.getMaxBenchPress());

        try {
            log.info("ИЗОБРАЖЕНИЕ_ТРЕНИРОВОЧНЫЙ_ПЛАН_EXCEL_ГЕНЕРАЦИЯ создание Excel файла для пользователя {}", userId);

            File excelFile = excelTrainingService.generateTrainingPlan(userId, userBenchPressRequest);

            if (excelFile == null || !excelFile.exists()) {
                log.error("ИЗОБРАЖЕНИЕ_ТРЕНИРОВОЧНЫЙ_ПЛАН_EXCEL_ФАЙЛ_НЕ_СОЗДАН не удалось создать Excel файл");
                throw new GenerateTrainingPlanException("Не удалось создать Excel файл для конвертации");
            }

            log.info("ИЗОБРАЖЕНИЕ_ТРЕНИРОВОЧНЫЙ_ПЛАН_EXCEL_ФАЙЛ_СОЗДАН размер {} байт путь {}", excelFile.length(), excelFile.getAbsolutePath());

            if (cacheEnabled) {
                String cacheKey = generateCacheKey(excelFile, userBenchPressRequest.getMaxBenchPress());
                CachedImage cached = imageCache.get(cacheKey);

                if (cached != null && !cached.isExpired(TimeUnit.MINUTES.toMillis(cacheTTLMinutes))) {
                    log.info("ИЗОБРАЖЕНИЕ_ТРЕНИРОВОЧНЫЙ_ПЛАН_КЕШ_ПОПАДАНИЕ изображение загружено из кеша ключ {} размер {}x{} строк {} колонок {}",
                            cacheKey, cached.width, cached.height, cached.actualRows, cached.actualColumns);

                    Path outputPath = Paths.get(cached.filePath);
                    if (Files.exists(outputPath)) {
                        log.info("ИЗОБРАЖЕНИЕ_ТРЕНИРОВОЧНЫЙ_ПЛАН_КЕШ_ФАЙЛ_СУЩЕСТВУЕТ используется существующий файл");
                        return outputPath.toFile();
                    } else {
                        BufferedImage restoredImage = restoreImageFromCache(cached);
                        if (restoredImage != null) {
                            saveImageWithCompression(restoredImage, outputPath);
                            log.info("ИЗОБРАЖЕНИЕ_ТРЕНИРОВОЧНЫЙ_ПЛАН_КЕШ_ФАЙЛ_ВОССТАНОВЛЕН файл пересоздан");
                            return outputPath.toFile();
                        }
                    }
                }
            }

            log.info("ИЗОБРАЖЕНИЕ_ТРЕНИРОВОЧНЫЙ_ПЛАН_КОНВЕРТАЦИЯ_В_ИЗОБРАЖЕНИЕ формат {}", defaultImageFormat);

            BufferedImage image = convertExcelToImage(excelFile, defaultImageFormat);

            if (image == null) {
                log.error("ИЗОБРАЖЕНИЕ_ТРЕНИРОВОЧНЫЙ_ПЛАН_ИЗОБРАЖЕНИЕ_НЕ_СОЗДАНО ошибка при создании изображения");
                throw new GenerateTrainingPlanException("Не удалось создать изображение");
            }

            log.info("ИЗОБРАЖЕНИЕ_ТРЕНИРОВОЧНЫЙ_ПЛАН_ИЗОБРАЖЕНИЕ_СОЗДАНО размер {}x{} пикселей", image.getWidth(), image.getHeight());

            Path outputPath = createOutputPath(userId);
            log.info("ИЗОБРАЖЕНИЕ_ТРЕНИРОВОЧНЫЙ_ПЛАН_СОХРАНЕНИЕ_ИЗОБРАЖЕНИЯ {}", outputPath.toAbsolutePath());

            saveImageWithCompression(image, outputPath);
            long fileSize = Files.size(outputPath);
            log.info("ИЗОБРАЖЕНИЕ_ТРЕНИРОВОЧНЫЙ_ПЛАН_ИЗОБРАЖЕНИЕ_СОХРАНЕНО размер {} байт путь {}", fileSize, outputPath.toAbsolutePath());

            File resultImage = outputPath.toFile();

            if (cacheEnabled) {
                cacheImage(image, excelFile, userBenchPressRequest.getMaxBenchPress(), outputPath.toString());
            }

            deleteTempExcelFile(excelFile);

            if (imageCache.size() > cacheMaxSize) {
                cleanupCache();
            }

            log.info("ИЗОБРАЖЕНИЕ_ТРЕНИРОВОЧНЫЙ_ПЛАН_ГЕНЕРАЦИЯ_УСПЕХ пользователь {} файл {}", userId, resultImage.getAbsolutePath());

            return resultImage;

        } catch (GenerateTrainingPlanException e) {
            log.error("ИЗОБРАЖЕНИЕ_ТРЕНИРОВОЧНЫЙ_ПЛАН_ГЕНЕРАЦИЯ_ОШИБКА {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("ИЗОБРАЖЕНИЕ_ТРЕНИРОВОЧНЫЙ_ПЛАН_ГЕНЕРАЦИЯ_ОШИБКА пользователь {} ошибка {}", userId, e.getMessage(), e);
            throw new GenerateTrainingPlanException(MessageConstants.GENERATE_PLAN_FAILURE);
        }
    }

    @Override
    public BufferedImage convertExcelToImage(File excelFile, String outputFormat) {
        log.info("EXCEL_В_ИЗОБРАЖЕНИЕ_КОНВЕРТАЦИЯ_НАЧАЛО файл {} формат {}", excelFile.getName(), outputFormat);

        validateExcelFile(excelFile);

        try (InputStream inputStream = new FileInputStream(excelFile)) {
            Workbook workbook = createWorkbook(excelFile, inputStream);

            try {
                log.info("EXCEL_КОНВЕРТАЦИЯ_EXCEL_ОБРАБОТКА листов в книге {}", workbook.getNumberOfSheets());

                Sheet sheet = getFirstSheet(workbook);

                SheetAnalysisResult analysis = analyzeSheetWithOptimization(sheet);
                int actualRows = analysis.actualRows;
                int actualColumns = analysis.actualColumns;
                List<Integer> nonEmptyColumnIndices = analysis.nonEmptyColumnIndices;
                List<Integer> nonEmptyRowIndices = analysis.nonEmptyRowIndices;

                log.info("EXCEL_КОНВЕРТАЦИЯ_ТАБЛИЦА_РАЗМЕР строк {} колонок {} непустых строк {} непустых колонок {}",
                        actualRows, actualColumns, nonEmptyRowIndices.size(), nonEmptyColumnIndices.size());

                if (actualRows > maxRows) {
                    log.warn("EXCEL_КОНВЕРТАЦИЯ_ТАБЛИЦА_СЛИШКОМ_БОЛЬШАЯ строк {} > {} будет обрезано", actualRows, maxRows);
                    actualRows = Math.min(actualRows, maxRows);
                    nonEmptyRowIndices = nonEmptyRowIndices.subList(0, actualRows);
                }

                if (actualColumns > maxColumns) {
                    log.warn("EXCEL_КОНВЕРТАЦИЯ_ТАБЛИЦА_СЛИШКОМ_ШИРОКАЯ колонок {} > {} будет обрезано", actualColumns, maxColumns);
                    actualColumns = Math.min(actualColumns, maxColumns);
                    nonEmptyColumnIndices = nonEmptyColumnIndices.subList(0, actualColumns);
                }

                checkMemoryRequirements(actualRows, actualColumns);

                int columnWidth = calculateOptimalColumnWidth(sheet, nonEmptyColumnIndices, nonEmptyRowIndices);
                int tableWidth = columnWidth * actualColumns;

                if (tableWidth > 8000) {
                    log.warn("EXCEL_КОНВЕРТАЦИЯ_ТАБЛИЦА_СЛИШКОМ_ШИРОКАЯ {}px применяем масштабирование", tableWidth);
                    double scale = 8000.0 / tableWidth;
                    columnWidth = (int) (columnWidth * scale);
                    tableWidth = columnWidth * actualColumns;
                    log.info("EXCEL_КОНВЕРТАЦИЯ_МАСШТАБИРОВАНИЕ_ШИРИНЫ коэффициент {} новая ширина столбца {}px", scale, columnWidth);
                }

                int imageWidth = Math.min(tableWidth + (2 * padding), 9000);
                int imageHeight = headerHeight + (actualRows * cellHeight) + footerHeight + (2 * padding);

                long estimatedMemory = (long) imageWidth * imageHeight * 4L;
                log.info("EXCEL_КОНВЕРТАЦИЯ_РАСЧЕТ_ПАМЯТИ требуется примерно {} байт для {}x{}", estimatedMemory, imageWidth, imageHeight);

                if (estimatedMemory > 80_000_000L) {
                    log.info("EXCEL_КОНВЕРТАЦИЯ_БЕЗОПАСНЫЙ_РЕЖИМ использование чанковой отрисовки");
                    return renderImageChunked(sheet, actualColumns, actualRows, imageWidth, imageHeight,
                            columnWidth, nonEmptyColumnIndices, nonEmptyRowIndices);
                }

                log.info("EXCEL_КОНВЕРТАЦИЯ_РАСЧЕТ_РАЗМЕРОВ изображение {}x{} таблица {}px столбец {}px",
                        imageWidth, imageHeight, tableWidth, columnWidth);

                BufferedImage image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
                Graphics2D graphics = image.createGraphics();

                try {
                    configureGraphicsQuality(graphics);
                    drawImageContent(graphics, sheet, actualColumns, actualRows, imageHeight, imageWidth,
                            columnWidth, nonEmptyColumnIndices, nonEmptyRowIndices);
                } finally {
                    graphics.dispose();
                }

                log.info("EXCEL_КОНВЕРТАЦИЯ_ИЗОБРАЖЕНИЕ_СОЗДАНО размер {}x{}", image.getWidth(), image.getHeight());

                return image;

            } finally {
                workbook.close();
            }

        } catch (OutOfMemoryError e) {
            log.error("EXCEL_КОНВЕРТАЦИЯ_ПЕРЕПОЛНЕНИЕ_ПАМЯТИ {} запуск очистки памяти", e.getMessage());

            System.gc();
            try {
                Thread.sleep(500);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }

            throw new GenerateTrainingPlanException("Недостаточно памяти для создания изображения. Попробуйте уменьшить размер таблицы.");
        } catch (Exception e) {
            log.error("EXCEL_КОНВЕРТАЦИЯ_КОНВЕРТАЦИЯ_ОШИБКА {}", e.getMessage(), e);
            throw new GenerateTrainingPlanException("Ошибка конвертации Excel в изображение: " + e.getMessage());
        }
    }

    private SheetAnalysisResult analyzeSheetWithOptimization(Sheet sheet) {
        int totalRows = Math.min(sheet.getLastRowNum() + 1, maxRows);
        List<Integer> nonEmptyRowIndices = new ArrayList<>();
        Set<Integer> nonEmptyColumnSet = new HashSet<>();
        int firstDataColumn = Integer.MAX_VALUE;
        int lastDataColumn = -1;

        for (int i = 0; i < totalRows; i++) {
            Row row = sheet.getRow(i);
            if (row != null) {
                boolean rowHasData = false;
                int lastCellNum = row.getLastCellNum();
                for (int j = 0; j < lastCellNum; j++) {
                    Cell cell = row.getCell(j);
                    if (hasCellContent(cell)) {
                        rowHasData = true;
                        nonEmptyColumnSet.add(j);
                        if (j < firstDataColumn) firstDataColumn = j;
                        if (j > lastDataColumn) lastDataColumn = j;
                    }
                }
                if (rowHasData) {
                    nonEmptyRowIndices.add(i);
                }
            }
        }

        List<Integer> nonEmptyColumnIndices = new ArrayList<>();

        if (firstDataColumn == Integer.MAX_VALUE) {
            firstDataColumn = 0;
        }
        if (lastDataColumn == -1) {
            lastDataColumn = Math.max(0, firstDataColumn);
        }

        if (optimizeEmptyColumns) {
            for (int i = firstDataColumn; i <= lastDataColumn && i <= maxColumns; i++) {
                nonEmptyColumnIndices.add(i);
            }
        } else {
            for (int i = 0; i <= Math.min(lastDataColumn, maxColumns); i++) {
                nonEmptyColumnIndices.add(i);
            }
        }

        int actualRows = Math.min(nonEmptyRowIndices.size(), maxRows);
        int actualColumns = Math.min(nonEmptyColumnIndices.size(), maxColumns);

        return new SheetAnalysisResult(actualRows, actualColumns, nonEmptyColumnIndices, nonEmptyRowIndices);
    }

    private boolean hasCellContent(Cell cell) {
        if (cell == null) return false;

        switch (cell.getCellType()) {
            case STRING:
                String stringValue = cell.getStringCellValue();
                return stringValue != null && !stringValue.trim().isEmpty();
            case NUMERIC:
                return true;
            case BOOLEAN:
                return true;
            case FORMULA:
                return true;
            case BLANK:
            default:
                return false;
        }
    }

    private void checkMemoryRequirements(int rows, int columns) {
        long estimatedMemory = (long) rows * columns * cellHeight * 120 * 4L;
        if (estimatedMemory > 200_000_000L) {
            log.warn("ПРОВЕРКА_ПАМЯТИ оценка памяти {} байт для {} строк {} колонок", estimatedMemory, rows, columns);
            System.gc();
        }
    }

    private int calculateOptimalColumnWidth(Sheet sheet, List<Integer> columnIndices, List<Integer> rowIndices) {
        int maxCellWidth = minColumnWidth;
        Font tempFont = new Font("Arial", Font.BOLD, cellFontSize);

        try {
            BufferedImage tempImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
            Graphics2D tempGraphics = tempImage.createGraphics();
            tempGraphics.setFont(tempFont);
            FontMetrics metrics = tempGraphics.getFontMetrics();

            int rowsToAnalyze = Math.min(rowIndices.size(), 30);
            int columnsToAnalyze = Math.min(columnIndices.size(), 30);

            for (int i = 0; i < rowsToAnalyze && i < rowIndices.size(); i++) {
                int rowIndex = rowIndices.get(i);
                Row row = sheet.getRow(rowIndex);
                if (row != null) {
                    for (int j = 0; j < columnsToAnalyze && j < columnIndices.size(); j++) {
                        int colIndex = columnIndices.get(j);
                        Cell cell = row.getCell(colIndex);
                        String cellValue = getCellValueAsString(cell);
                        if (cellValue != null && !cellValue.isEmpty()) {
                            int textWidth = metrics.stringWidth(cellValue);
                            maxCellWidth = Math.max(maxCellWidth, textWidth + 30);
                        }
                    }
                }
            }

            tempGraphics.dispose();
        } catch (Exception e) {
            log.warn("ОПТИМАЛЬНАЯ_ШИРИНА_СТОЛБЦА_ОШИБКА {}", e.getMessage());
            return minColumnWidth;
        }

        return Math.min(maxCellWidth, 350);
    }

    private void drawImageContent(Graphics2D graphics, Sheet sheet, int columnCount, int rowCount,
                                  int imageHeight, int imageWidth, int columnWidth,
                                  List<Integer> columnIndices, List<Integer> rowIndices) {
        graphics.setColor(backgroundColor);
        graphics.fillRect(0, 0, imageWidth, imageHeight);

        drawHeader(graphics, imageWidth);
        drawTable(graphics, sheet, columnCount, rowCount, imageWidth, columnWidth, columnIndices, rowIndices);
        drawFooter(graphics, imageHeight, imageWidth);
    }

    private void drawHeader(Graphics2D graphics, int imageWidth) {
        graphics.setColor(headerColor);
        graphics.fillRect(0, 0, imageWidth, headerHeight);

        Font titleFont = new Font("Arial", Font.BOLD, titleFontSize);
        graphics.setFont(titleFont);
        graphics.setColor(Color.WHITE);

        String title = "ТРЕНИРОВОЧНЫЙ ПЛАН";
        FontMetrics titleMetrics = graphics.getFontMetrics(titleFont);
        int titleX = (imageWidth - titleMetrics.stringWidth(title)) / 2;
        graphics.drawString(title, titleX, headerHeight - 30);
    }

    private void drawTable(Graphics2D graphics, Sheet sheet, int columnCount, int rowCount,
                           int imageWidth, int columnWidth, List<Integer> columnIndices, List<Integer> rowIndices) {
        int tableWidth = columnWidth * columnCount;
        int tableStartY = headerHeight + padding;
        int tableStartX = padding;

        if (tableWidth + (2 * padding) > imageWidth) {
            tableStartX = (imageWidth - tableWidth) / 2;
        }

        drawTableHeader(graphics, sheet, tableWidth, columnWidth, tableStartY, columnCount, tableStartX, columnIndices);
        drawTableRows(graphics, sheet, tableWidth, columnWidth, tableStartY, rowCount, columnCount, tableStartX, columnIndices, rowIndices);
    }

    private void drawTableHeader(Graphics2D graphics, Sheet sheet, int tableWidth, int colWidth,
                                 int tableStartY, int columnCount, int tableStartX, List<Integer> columnIndices) {
        graphics.setColor(tableHeaderColor);
        graphics.fillRect(tableStartX, tableStartY, tableWidth, cellHeight);

        graphics.setColor(Color.WHITE);
        Font headerFont = new Font("Arial", Font.BOLD, headerFontSize);
        graphics.setFont(headerFont);

        Row headerRow = sheet.getRow(0);
        if (headerRow != null) {
            for (int j = 0; j < columnCount && j < columnIndices.size(); j++) {
                int actualColumn = columnIndices.get(j);
                Cell cell = headerRow.getCell(actualColumn);
                int x = tableStartX + j * colWidth;
                int y = tableStartY + cellHeight - 15;

                String cellValue = getCellValueAsString(cell);
                if (cellValue != null && !cellValue.isEmpty()) {
                    FontMetrics metrics = graphics.getFontMetrics();
                    String trimmedValue = trimTextToFit(cellValue, metrics, colWidth - 20);
                    int textX = x + (colWidth - metrics.stringWidth(trimmedValue)) / 2;
                    graphics.drawString(trimmedValue, textX, y);
                }
            }
        }

        graphics.setColor(cellBorderColor);
        graphics.drawRect(tableStartX, tableStartY, tableWidth, cellHeight);
    }

    private void drawTableRows(Graphics2D graphics, Sheet sheet, int tableWidth, int colWidth,
                               int tableStartY, int rowCount, int columnCount, int tableStartX,
                               List<Integer> columnIndices, List<Integer> rowIndices) {
        Font cellFont = new Font("Arial", Font.PLAIN, cellFontSize);
        graphics.setFont(cellFont);

        for (int i = 0; i < rowCount && i < rowIndices.size(); i++) {
            int rowIndex = rowIndices.get(i);
            Row row = sheet.getRow(rowIndex);
            int y = tableStartY + (i + 1) * cellHeight;

            Color rowColor = (i % 2 == 0) ? oddRowColor : evenRowColor;
            graphics.setColor(rowColor);
            graphics.fillRect(tableStartX, y, tableWidth, cellHeight);

            graphics.setColor(cellBorderColor);
            graphics.drawRect(tableStartX, y, tableWidth, cellHeight);

            if (row != null) {
                for (int j = 0; j < columnCount && j < columnIndices.size(); j++) {
                    int actualColumn = columnIndices.get(j);
                    Cell cell = row.getCell(actualColumn);
                    int x = tableStartX + j * colWidth;
                    int textY = y + cellHeight - 15;

                    String cellValue = getCellValueAsString(cell);
                    if (cellValue != null && !cellValue.isEmpty()) {
                        FontMetrics metrics = graphics.getFontMetrics(cellFont);

                        if (j == 0) {
                            graphics.setColor(verticalTextColor);
                        } else {
                            graphics.setColor(horizontalTextColor);
                        }

                        String trimmedValue = trimTextToFit(cellValue, metrics, colWidth - 20);
                        int textX = x + (colWidth - metrics.stringWidth(trimmedValue)) / 2;
                        graphics.drawString(trimmedValue, textX, textY);
                    }

                    graphics.setColor(cellBorderColor);
                    graphics.drawLine(x, y, x, y + cellHeight);
                }
            }

            graphics.setColor(cellBorderColor);
            graphics.drawLine(tableStartX, y + cellHeight, tableStartX + tableWidth, y + cellHeight);
        }

        for (int j = 0; j <= columnCount && j <= columnIndices.size(); j++) {
            int x = tableStartX + j * colWidth;
            graphics.drawLine(x, tableStartY, x, tableStartY + (rowCount + 1) * cellHeight);
        }
    }

    private void drawFooter(Graphics2D graphics, int imageHeight, int imageWidth) {
        graphics.setColor(footerTextColor);
        Font footerFont = new Font("Arial", Font.ITALIC, 14);
        graphics.setFont(footerFont);

        String footerText = "Сгенерировано Gen Strong ботом • " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));

        FontMetrics footerMetrics = graphics.getFontMetrics();
        int footerX = (imageWidth - footerMetrics.stringWidth(footerText)) / 2;
        int footerY = imageHeight - 20;

        graphics.drawString(footerText, footerX, footerY);
    }

    private BufferedImage renderImageChunked(Sheet sheet, int columnCount, int rowCount,
                                             int imageWidth, int imageHeight, int columnWidth,
                                             List<Integer> columnIndices, List<Integer> rowIndices) {
        log.info("ОПТИМИЗИРОВАННАЯ_ОТРИСОВКА_НАЧАЛО {}x{} строк {} колонок {}", imageWidth, imageHeight, rowCount, columnCount);

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

            drawTableHeader(graphics, sheet, tableWidth, columnWidth, tableStartY, columnCount, tableStartX, columnIndices);

            Font cellFont = new Font("Arial", Font.PLAIN, cellFontSize);
            graphics.setFont(cellFont);

            int actualChunkSize = Math.min(chunkSize, rowCount);

            for (int chunkStart = 0; chunkStart < rowCount; chunkStart += actualChunkSize) {
                int chunkEnd = Math.min(chunkStart + actualChunkSize, rowCount);

                for (int i = chunkStart; i < chunkEnd && i < rowIndices.size(); i++) {
                    int rowIndex = rowIndices.get(i);
                    Row row = sheet.getRow(rowIndex);
                    int y = tableStartY + (i + 1) * cellHeight;

                    Color rowColor = (i % 2 == 0) ? oddRowColor : evenRowColor;
                    graphics.setColor(rowColor);
                    graphics.fillRect(tableStartX, y, tableWidth, cellHeight);

                    graphics.setColor(cellBorderColor);
                    graphics.drawRect(tableStartX, y, tableWidth, cellHeight);

                    if (row != null) {
                        for (int j = 0; j < columnCount && j < columnIndices.size(); j++) {
                            int actualColumn = columnIndices.get(j);
                            Cell cell = row.getCell(actualColumn);
                            int x = tableStartX + j * columnWidth;
                            int textY = y + cellHeight - 15;

                            String cellValue = getCellValueAsString(cell);
                            if (cellValue != null && !cellValue.isEmpty()) {
                                FontMetrics metrics = graphics.getFontMetrics(cellFont);

                                if (j == 0) {
                                    graphics.setColor(verticalTextColor);
                                } else {
                                    graphics.setColor(horizontalTextColor);
                                }

                                String trimmedValue = trimTextToFit(cellValue, metrics, columnWidth - 20);
                                int textX = x + (columnWidth - metrics.stringWidth(trimmedValue)) / 2;
                                graphics.drawString(trimmedValue, textX, textY);
                            }
                        }
                    }

                    if (i % 50 == 0 && i > 0) {
                        System.gc();
                    }
                }

                if (chunkStart > 0 && chunkStart % 100 == 0) {
                    log.debug("ОПТИМИЗИРОВАННАЯ_ОТРИСОВКА_ПРОГРЕСС обработано {} строк из {}", chunkStart, rowCount);
                }
            }

            for (int j = 0; j <= columnCount && j <= columnIndices.size(); j++) {
                int x = tableStartX + j * columnWidth;
                graphics.drawLine(x, tableStartY, x, tableStartY + (rowCount + 1) * cellHeight);
            }

            for (int i = 0; i <= rowCount && i <= rowIndices.size(); i++) {
                int y = tableStartY + (i + 1) * cellHeight;
                graphics.drawLine(tableStartX, y, tableStartX + tableWidth, y);
            }

            drawFooter(graphics, imageHeight, imageWidth);

        } finally {
            graphics.dispose();
        }

        log.info("ОПТИМИЗИРОВАННАЯ_ОТРИСОВКА_УСПЕХ изображение создано");
        return image;
    }

    private Path createOutputPath(UUID userId) throws IOException {
        Path imageOutputDir = Paths.get(imageOutputDirPath);
        if (!Files.exists(imageOutputDir)) {
            Files.createDirectories(imageOutputDir);
            log.info("ДИРЕКТОРИЯ_СОЗДАНИЕ {}", imageOutputDir.toAbsolutePath());
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = String.format("training_plan_%s_%s.%s", userId, timestamp, defaultImageFormat);
        return imageOutputDir.resolve(filename);
    }

    private void saveImageWithCompression(BufferedImage image, Path outputPath) throws IOException {
        try (FileOutputStream imageOut = new FileOutputStream(outputPath.toFile())) {
            if ("jpg".equalsIgnoreCase(defaultImageFormat) || "jpeg".equalsIgnoreCase(defaultImageFormat)) {
                BufferedImage rgbImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
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

    private void deleteTempExcelFile(File excelFile) {
        log.info("ИЗОБРАЖЕНИЕ_ТРЕНИРОВОЧНЫЙ_ПЛАН_ВРЕМЕННЫЙ_ФАЙЛ_УДАЛЕНИЕ Excel файл {}", excelFile.getAbsolutePath());
        if (excelFile.delete()) {
            log.info("ИЗОБРАЖЕНИЕ_ТРЕНИРОВОЧНЫЙ_ПЛАН_ВРЕМЕННЫЙ_ФАЙЛ_УДАЛЕН");
        } else {
            log.warn("ИЗОБРАЖЕНИЕ_ТРЕНИРОВОЧНЫЙ_ПЛАН_ВРЕМЕННЫЙ_ФАЙЛ_НЕ_УДАЛЕН требуется ручное удаление");
        }
    }

    private void validateExcelFile(File excelFile) {
        if (!excelFile.exists() || !excelFile.canRead()) {
            log.error("EXCEL_КОНВЕРТАЦИЯ_ФАЙЛ_НЕДОСТУПЕН {}", excelFile.getAbsolutePath());
            throw new GenerateTrainingPlanException("Excel файл недоступен для чтения");
        }
        log.info("EXCEL_КОНВЕРТАЦИЯ_EXCEL_ЧТЕНИЕ размер файла {} байт", excelFile.length());
    }

    private Workbook createWorkbook(File excelFile, InputStream inputStream) throws IOException {
        if (excelFile.getName().toLowerCase().endsWith(".xlsx")) {
            log.info("EXCEL_КОНВЕРТАЦИЯ_EXCEL_ФОРМАТ XLSX");
            return new XSSFWorkbook(inputStream);
        } else if (excelFile.getName().toLowerCase().endsWith(".xls")) {
            log.info("EXCEL_КОНВЕРТАЦИЯ_EXCEL_ФОРМАТ XLS");
            return new HSSFWorkbook(inputStream);
        } else {
            log.error("EXCEL_КОНВЕРТАЦИЯ_НЕПОДДЕРЖИВАЕМЫЙ_ФОРМАТ {}", excelFile.getName());
            throw new GenerateTrainingPlanException("Неподдерживаемый формат Excel файла");
        }
    }

    private Sheet getFirstSheet(Workbook workbook) {
        if (workbook.getNumberOfSheets() == 0) {
            log.error("EXCEL_КОНВЕРТАЦИЯ_ПУСТАЯ_КНИГА нет листов");
            throw new GenerateTrainingPlanException("Excel файл не содержит листов");
        }

        Sheet sheet = workbook.getSheetAt(0);
        if (sheet == null) {
            log.error("EXCEL_КОНВЕРТАЦИЯ_ЛИСТ_НЕ_НАЙДЕН");
            throw new GenerateTrainingPlanException("Первый лист не найден");
        }
        return sheet;
    }

    private void configureGraphicsQuality(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        try {
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        } catch (Exception e) {
            log.warn("СГЛАЖИВАНИЕ_ТЕКСТА_ОШИБКА {}", e.getMessage());
        }

        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
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
                    log.warn("ФОРМУЛА_ВЫЧИСЛЕНИЕ_ОШИБКА {}", e.getMessage());
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

    private void cacheImage(BufferedImage image, File excelFile, Double maxBenchPress, String filePath) {
        try {
            String cacheKey = generateCacheKey(excelFile, maxBenchPress);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            byte[] compressedData = baos.toByteArray();

            imageCache.put(cacheKey, new CachedImage(compressedData, filePath, image.getWidth(), image.getHeight(), 0, 0));
            log.info("ИЗОБРАЖЕНИЕ_ТРЕНИРОВОЧНЫЙ_ПЛАН_ИЗОБРАЖЕНИЕ_ЗАКЕШИРОВАНО ключ {} размер данных {} байт", cacheKey, compressedData.length);

            if (imageCache.size() > cacheMaxSize) {
                cleanupCache();
            }
        } catch (Exception e) {
            log.warn("ИЗОБРАЖЕНИЕ_ТРЕНИРОВОЧНЫЙ_ПЛАН_КЕШИРОВАНИЕ_ОШИБКА {}", e.getMessage());
        }
    }

    private BufferedImage restoreImageFromCache(CachedImage cached) {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(cached.compressedImageData);
            return ImageIO.read(bais);
        } catch (Exception e) {
            log.warn("ИЗОБРАЖЕНИЕ_ТРЕНИРОВОЧНЫЙ_ПЛАН_ВОССТАНОВЛЕНИЕ_ИЗ_КЕША_ОШИБКА {}", e.getMessage());
            return null;
        }
    }

    public void cleanupCache() {
        if (!cacheEnabled) {
            return;
        }

        long ttlMillis = TimeUnit.MINUTES.toMillis(cacheTTLMinutes);
        int initialSize = imageCache.size();

        imageCache.entrySet().removeIf(entry -> entry.getValue().isExpired(ttlMillis));

        int removedCount = initialSize - imageCache.size();
        if (removedCount > 0) {
            log.info("КЕШ_ОЧИСТКА удалено {} просроченных записей", removedCount);
        }

        if (imageCache.size() > cacheMaxSize) {
            int toRemove = imageCache.size() - cacheMaxSize;
            imageCache.entrySet().stream()
                    .sorted((e1, e2) -> Long.compare(e1.getValue().creationTime, e2.getValue().creationTime))
                    .limit(toRemove)
                    .forEach(entry -> imageCache.remove(entry.getKey()));
            log.info("КЕШ_ОЧИСТКА удалено {} старых записей для ограничения размера", toRemove);
        }

        System.gc();
    }

    private static class SheetAnalysisResult {
        final int actualRows;
        final int actualColumns;
        final List<Integer> nonEmptyColumnIndices;
        final List<Integer> nonEmptyRowIndices;

        SheetAnalysisResult(int actualRows, int actualColumns, List<Integer> nonEmptyColumnIndices, List<Integer> nonEmptyRowIndices) {
            this.actualRows = actualRows;
            this.actualColumns = actualColumns;
            this.nonEmptyColumnIndices = nonEmptyColumnIndices;
            this.nonEmptyRowIndices = nonEmptyRowIndices;
        }
    }
}