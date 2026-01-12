package ru.cs.vsu.social_network.telegram_bot.service.serviceImpl.image;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.cs.vsu.social_network.telegram_bot.exception.GenerateTrainingPlanException;
import ru.cs.vsu.social_network.telegram_bot.service.image.ExcelToImageConverter;
import ru.cs.vsu.social_network.telegram_bot.utils.ExcelUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.ref.SoftReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Реализация конвертера Excel файлов в изображения с использованием Apache POI Cell.
 * Оптимизирован для работы с большими таблицами (30x30+) и HD качеством для Telegram.
 */
@Slf4j
@Service
public class ExcelToImageConverterImpl implements ExcelToImageConverter {

    @Value("${training.image.max.rows:100}")
    private int maxRows;

    @Value("${training.image.max.columns:50}")
    private int maxColumns;

    @Value("${training.image.chunk.size:20}")
    private int chunkSize;

    @Value("${training.image.temp.dir:temp_images}")
    private String tempImageDir;

    @Value("${training.image.sample.rows:30}")
    private int sampleRows;

    @Value("${training.image.enable.hd.scaling:true}")
    private boolean enableHDScaling;

    @Value("${training.image.cache.enabled:true}")
    private boolean cacheEnabled;

    private final ImageRendererImpl imageRenderer;
    private final Map<String, SoftReference<BufferedImage>> imageCache = new ConcurrentHashMap<>();
    private final Map<Integer, Integer> columnWidthCache = new ConcurrentHashMap<>();

    private final static int BASE_COLUMN_WIDTH = 80;
    private static final int ANALYSIS_BUFFER_MULTIPLIER = 2;

    /**
     * Конструктор для внедрения зависимости рендерера изображений.
     *
     * @param imageRenderer рендерер для отрисовки изображений
     */
    public ExcelToImageConverterImpl(ImageRendererImpl imageRenderer) {
        this.imageRenderer = imageRenderer;
    }

    /** {@inheritDoc} */
    @Override
    public BufferedImage convertExcelToImage(File excelFile, String outputFormat) {
        log.info("EXCEL_В_ИЗОБРАЖЕНИЕ_КОНВЕРТАЦИЯ_НАЧАЛО: файл {} формат {}", excelFile.getName(), outputFormat);

        ExcelUtils.validateExcelFile(excelFile);

        String cacheKey = generateCacheKey(excelFile);
        if (cacheEnabled) {
            SoftReference<BufferedImage> cachedRef = imageCache.get(cacheKey);
            BufferedImage cachedImage = cachedRef != null ? cachedRef.get() : null;
            if (cachedImage != null) {
                log.info("EXCEL_КОНВЕРТАЦИЯ_КЭШ_ИЗОБРАЖЕНИЯ: используем кэшированное изображение");
                return cachedImage;
            }
        }

        try (InputStream inputStream = new FileInputStream(excelFile)) {
            Workbook workbook = ExcelUtils.createWorkbook(excelFile, inputStream);

            try {
                log.info("EXCEL_КОНВЕРТАЦИЯ_EXCEL_ОБРАБОТКА: листов в книге {}", workbook.getNumberOfSheets());

                Sheet sheet = ExcelUtils.getFirstSheet(workbook);
                BufferedImage result = renderSheetToImageOptimized(sheet);

                if (cacheEnabled) {
                    imageCache.put(cacheKey, new SoftReference<>(result));
                    log.info("EXCEL_КОНВЕРТАЦИЯ_КЭШ_СОХРАНЕНИЕ: изображение сохранено в кэш, ключ: {}", cacheKey);
                }

                return result;

            } finally {
                workbook.close();
            }

        } catch (OutOfMemoryError e) {
            log.error("EXCEL_КОНВЕРТАЦИЯ_ПЕРЕПОЛНЕНИЕ_ПАМЯТИ: {} запуск очистки памяти", e.getMessage());

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

    /** {@inheritDoc} */
    @Override
    public BufferedImage renderSheetToImage(Sheet sheet) {
        return renderSheetToImageOptimized(sheet);
    }

    /**
     * Генерирует ключ для кэширования изображения.
     *
     * @param excelFile файл Excel
     * @return ключ кэша
     */
    private String generateCacheKey(File excelFile) {
        return excelFile.getAbsolutePath() + "_" + excelFile.lastModified();
    }

    /**
     * Оптимизированный метод рендеринга листа в изображение.
     *
     * @param sheet лист Excel для рендеринга
     * @return изображение таблицы
     */
    private BufferedImage renderSheetToImageOptimized(Sheet sheet) {
        SheetAnalysisResult analysis = analyzeSheetWithOptimization(sheet);
        int actualRows = analysis.actualRows;
        int actualColumns = analysis.actualColumns;
        List<Integer> nonEmptyColumnIndices = analysis.nonEmptyColumnIndices;
        List<Integer> nonEmptyRowIndices = analysis.nonEmptyRowIndices;

        log.info("EXCEL_КОНВЕРТАЦИЯ_ТАБЛИЦА_РАЗМЕР: строк {} колонок {} непустых строк {} непустых колонок {}",
                actualRows, actualColumns, nonEmptyRowIndices.size(), nonEmptyColumnIndices.size());

        if (actualRows > maxRows) {
            log.warn("EXCEL_КОНВЕРТАЦИЯ_ТАБЛИЦА_СЛИШКОМ_БОЛЬШАЯ: строк {} > {} будет обрезано", actualRows, maxRows);
            actualRows = Math.min(actualRows, maxRows);
            nonEmptyRowIndices = nonEmptyRowIndices.subList(0, Math.min(actualRows, nonEmptyRowIndices.size()));
        }

        if (actualColumns > maxColumns) {
            log.warn("EXCEL_КОНВЕРТАЦИЯ_ТАБЛИЦА_СЛИШКОМ_ШИРОКАЯ: колонок {} > {} будет обрезано", actualColumns, maxColumns);
            actualColumns = Math.min(actualColumns, maxColumns);
            nonEmptyColumnIndices = nonEmptyColumnIndices.subList(0, Math.min(actualColumns, nonEmptyColumnIndices.size()));
        }

        int columnWidth = calculateOptimalColumnWidthCached(sheet, nonEmptyColumnIndices);
        int tableWidth = columnWidth * actualColumns;
        int tableHeight = imageRenderer.getHeaderHeight() + (actualRows * imageRenderer.getCellHeight()) +
                imageRenderer.getFooterHeight() + (2 * imageRenderer.getPadding());

        if (enableHDScaling) {
            Dimension hdSize = imageRenderer.calculateHDSize(tableWidth, tableHeight);
            double widthScale = (double) hdSize.width / tableWidth;
            double heightScale = (double) hdSize.height / tableHeight;
            double scale = Math.min(widthScale, heightScale);

            if (scale < 1.0) {
                columnWidth = (int) (columnWidth * scale);
                tableWidth = columnWidth * actualColumns;
                log.info("EXCEL_КОНВЕРТАЦИЯ_HD_МАСШТАБИРОВАНИЕ: коэффициент {} новая ширина столбца {}px", scale, columnWidth);
            }
        }

        int imageWidth = tableWidth + (2 * imageRenderer.getPadding());
        int imageHeight = tableHeight;

        log.info("EXCEL_КОНВЕРТАЦИЯ_РАСЧЕТ_РАЗМЕРОВ: изображение {}x{} таблица {}px столбец {}px",
                imageWidth, imageHeight, tableWidth, columnWidth);

        if (imageWidth * imageHeight > 50_000_000) {
            log.info("EXCEL_КОНВЕРТАЦИЯ_РЕЖИМ_ФАЙЛОВОГО_РЕНДЕРИНГА: использование рендеринга напрямую в файл");
            try {
                return renderToFileAndLoad(sheet, actualColumns, actualRows, imageWidth, imageHeight,
                        columnWidth, nonEmptyColumnIndices, nonEmptyRowIndices);
            } catch (IOException e) {
                log.warn("EXCEL_КОНВЕРТАЦИЯ_ФАЙЛОВЫЙ_РЕНДЕРИНГ_ОШИБКА: возвращаемся к чанковому рендерингу: {}", e.getMessage());
                return renderImageChunked(sheet, actualColumns, actualRows, imageWidth, imageHeight,
                        columnWidth, nonEmptyColumnIndices, nonEmptyRowIndices);
            }
        }

        return renderImageDirect(sheet, actualColumns, actualRows, imageWidth, imageHeight,
                columnWidth, nonEmptyColumnIndices, nonEmptyRowIndices);
    }

    /**
     * Прямой рендеринг в память.
     *
     * @param sheet лист Excel
     * @param columnCount количество столбцов
     * @param rowCount количество строк
     * @param imageWidth ширина изображения
     * @param imageHeight высота изображения
     * @param columnWidth ширина столбца
     * @param columnIndices индексы столбцов
     * @param rowIndices индексы строк
     * @return изображение таблицы
     */
    private BufferedImage renderImageDirect(Sheet sheet, int columnCount, int rowCount,
                                            int imageWidth, int imageHeight, int columnWidth,
                                            List<Integer> columnIndices, List<Integer> rowIndices) {
        BufferedImage image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();

        try {
            imageRenderer.drawImageContent(graphics, sheet, columnCount, rowCount, imageHeight, imageWidth,
                    columnWidth, columnIndices, rowIndices);
        } finally {
            graphics.dispose();
        }

        log.info("EXCEL_КОНВЕРТАЦИЯ_ИЗОБРАЖЕНИЕ_СОЗДАНО: размер {}x{}", image.getWidth(), image.getHeight());
        return image;
    }

    /**
     * Рендерит изображение чанками для экономии памяти.
     *
     * @param sheet лист Excel
     * @param columnCount количество столбцов
     * @param rowCount количество строк
     * @param imageWidth ширина изображения
     * @param imageHeight высота изображения
     * @param columnWidth ширина столбца
     * @param columnIndices индексы столбцов
     * @param rowIndices индексы строк
     * @return изображение таблицы
     */
    private BufferedImage renderImageChunked(Sheet sheet, int columnCount, int rowCount,
                                             int imageWidth, int imageHeight, int columnWidth,
                                             List<Integer> columnIndices, List<Integer> rowIndices) {
        log.info("ЧАНКОВЫЙ_РЕНДЕРИНГ_НАЧАЛО: {}x{} строк {} колонок {}", imageWidth, imageHeight, rowCount, columnCount);

        BufferedImage image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();

        try {
            ExcelUtils.configureGraphicsQuality(graphics);
            graphics.setColor(imageRenderer.getBackgroundColor());
            graphics.fillRect(0, 0, imageWidth, imageHeight);

            imageRenderer.drawHeader(graphics, imageWidth);

            int tableWidth = columnWidth * columnCount;
            int tableStartY = imageRenderer.getHeaderHeight() + imageRenderer.getPadding();
            int tableStartX = imageRenderer.getPadding();

            if (tableWidth + (2 * imageRenderer.getPadding()) > imageWidth) {
                tableStartX = (imageWidth - tableWidth) / 2;
            }

            imageRenderer.drawTableHeader(graphics, sheet, tableWidth, columnWidth, tableStartY,
                    columnCount, tableStartX, columnIndices);

            int actualChunkSize = Math.min(chunkSize, rowCount);

            for (int chunkStart = 0; chunkStart < rowCount; chunkStart += actualChunkSize) {
                int chunkEnd = Math.min(chunkStart + actualChunkSize, rowCount);
                List<Integer> chunkRowIndices = rowIndices.subList(chunkStart, chunkEnd);

                imageRenderer.drawTableRows(graphics, sheet, tableWidth, columnWidth, tableStartY,
                        chunkEnd - chunkStart, columnCount, tableStartX,
                        columnIndices, chunkRowIndices);

                if (chunkStart > 0 && chunkStart % 50 == 0) {
                    System.gc();
                    log.debug("ЧАНКОВЫЙ_РЕНДЕРИНГ_ПРОГРЕСС: обработано {} строк из {}", chunkStart, rowCount);
                }
            }

            imageRenderer.drawFooter(graphics, imageHeight, imageWidth);

        } finally {
            graphics.dispose();
        }

        log.info("ЧАНКОВЫЙ_РЕНДЕРИНГ_УСПЕХ: изображение создано");
        return image;
    }

    /**
     * Рендерит напрямую в файл для очень больших изображений.
     *
     * @param sheet лист Excel
     * @param columnCount количество столбцов
     * @param rowCount количество строк
     * @param imageWidth ширина изображения
     * @param imageHeight высота изображения
     * @param columnWidth ширина столбца
     * @param columnIndices индексы столбцов
     * @param rowIndices индексы строк
     * @return изображение таблицы
     */
    private BufferedImage renderToFileAndLoad(Sheet sheet, int columnCount, int rowCount,
                                              int imageWidth, int imageHeight, int columnWidth,
                                              List<Integer> columnIndices, List<Integer> rowIndices)
            throws IOException {
        log.info("ФАЙЛОВЫЙ_РЕНДЕРИНГ_НАЧАЛО: {}x{} строк {} колонок", imageWidth, imageHeight, rowCount);

        Path tempDir = Paths.get(tempImageDir);
        if (!Files.exists(tempDir)) {
            Files.createDirectories(tempDir);
        }

        Path tempFile = tempDir.resolve("temp_render_" + System.currentTimeMillis() + ".png");

        try {
            renderToFile(sheet, columnCount, rowCount, imageWidth, imageHeight,
                    columnWidth, columnIndices, rowIndices, tempFile);

            long fileSize = Files.size(tempFile);
            log.info("ФАЙЛОВЫЙ_РЕНДЕРИНГ_СОХРАНЕНИЕ: файл создан {} размер {} байт",
                    tempFile, fileSize);

            BufferedImage image = ImageIO.read(tempFile.toFile());

            if (image != null) {
                log.info("ФАЙЛОВЫЙ_РЕНДЕРИНГ_ЗАГРУЗКА: изображение загружено {}x{}",
                        image.getWidth(), image.getHeight());
            } else {
                log.error("ФАЙЛОВЫЙ_РЕНДЕРИНГ_ОШИБКА: не удалось загрузить изображение из файла");
                throw new IOException("Не удалось загрузить изображение из временного файла");
            }

            return image;

        } finally {
            cleanupTempFileAsync(tempFile);
        }
    }

    /**
     * Выполняет рендеринг напрямую в файл.
     *
     * @param sheet лист Excel
     * @param columnCount количество столбцов
     * @param rowCount количество строк
     * @param imageWidth ширина изображения
     * @param imageHeight высота изображения
     * @param columnWidth ширина столбца
     * @param columnIndices индексы столбцов
     * @param rowIndices индексы строк
     * @param outputFile файл для сохранения
     */
    private void renderToFile(Sheet sheet, int columnCount, int rowCount,
                              int imageWidth, int imageHeight, int columnWidth,
                              List<Integer> columnIndices, List<Integer> rowIndices,
                              Path outputFile) throws IOException {
        BufferedImage fullImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = fullImage.createGraphics();

        try {
            imageRenderer.drawImageContent(graphics, sheet, columnCount, rowCount, imageHeight, imageWidth,
                    columnWidth, columnIndices, rowIndices);
        } finally {
            graphics.dispose();
        }

        try (FileOutputStream fos = new FileOutputStream(outputFile.toFile())) {
            boolean written = ImageIO.write(fullImage, "png", fos);
            if (!written) {
                throw new IOException("Не удалось сохранить изображение в формате PNG");
            }
        }
    }

    /**
     * Анализирует лист Excel с оптимизацией пустых столбцов и строк.
     * Возвращает только непустые строки и столбцы.
     *
     * @param sheet лист Excel для анализа
     * @return результат анализа листа
     */
    private SheetAnalysisResult analyzeSheetWithOptimization(Sheet sheet) {
        int totalRows = Math.min(sheet.getLastRowNum() + 1, maxRows * ANALYSIS_BUFFER_MULTIPLIER);
        List<Integer> nonEmptyRowIndices = new ArrayList<>();
        Set<Integer> nonEmptyColumnSet = new HashSet<>();
        int firstDataColumn = Integer.MAX_VALUE;
        int lastDataColumn = -1;

        for (int i = 0; i < totalRows; i++) {
            Row row = sheet.getRow(i);
            if (row != null) {
                boolean rowHasData = false;
                int lastCellNum = Math.min(row.getLastCellNum(), maxColumns * 2);
                for (int j = 0; j < lastCellNum; j++) {
                    Cell cell = row.getCell(j);
                    if (ExcelUtils.hasCellContent(cell)) {
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

        for (int i = firstDataColumn; i <= lastDataColumn && i <= maxColumns * 2; i++) {
            nonEmptyColumnIndices.add(i);
        }

        int actualRows = Math.min(nonEmptyRowIndices.size(), maxRows);
        int actualColumns = Math.min(nonEmptyColumnIndices.size(), maxColumns);

        return new SheetAnalysisResult(actualRows, actualColumns, nonEmptyColumnIndices, nonEmptyRowIndices);
    }

    /**
     * Вычисляет оптимальную ширину столбцов с кэшированием.
     *
     * @param sheet лист Excel
     * @param columnIndices индексы столбцов
     * @return средняя ширина столбца
     */
    private int calculateOptimalColumnWidthCached(Sheet sheet, List<Integer> columnIndices) {
        int totalWidth = 0;
        int columnCount = columnIndices.size();

        if (columnCount == 0) {
            return BASE_COLUMN_WIDTH;
        }

        for (int colIndex : columnIndices) {
            totalWidth += columnWidthCache.computeIfAbsent(colIndex,
                    idx -> imageRenderer.calculateOptimalColumnWidth(sheet, Collections.singletonList(idx), sampleRows));
        }
        return totalWidth / columnCount;
    }

    /**
     * Асинхронная очистка временного файла.
     *
     * @param tempFile временный файл
     */
    private void cleanupTempFileAsync(Path tempFile) {
        new Thread(() -> {
            try {
                Thread.sleep(5000);
                Files.deleteIfExists(tempFile);
                log.debug("ФАЙЛОВЫЙ_РЕНДЕРИНГ_ОЧИСТКА: временный файл удален");
            } catch (IOException e) {
                log.warn("ФАЙЛОВЫЙ_РЕНДЕРИНГ_ОЧИСТКА_ОШИБКА: не удалось удалить временный файл: {}", e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    /**
     * Результат анализа листа Excel.
     */
    private record SheetAnalysisResult(int actualRows, int actualColumns, List<Integer> nonEmptyColumnIndices,
                                       List<Integer> nonEmptyRowIndices) {
    }
}