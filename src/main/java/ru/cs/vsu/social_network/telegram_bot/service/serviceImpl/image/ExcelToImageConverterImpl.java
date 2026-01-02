package ru.cs.vsu.social_network.telegram_bot.service.serviceImpl.image;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.cs.vsu.social_network.telegram_bot.exception.GenerateTrainingPlanException;
import ru.cs.vsu.social_network.telegram_bot.service.image.ExcelToImageConverter;
import ru.cs.vsu.social_network.telegram_bot.utils.ExcelUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Реализация конвертера Excel файлов в изображения с использованием
 * оптимизированного алгоритма рендеринга для работы с большими таблицами.
 * Использует чанковый рендеринг и рендеринг напрямую в файл для экономии памяти.
 */
@Slf4j
@Service
public class ExcelToImageConverterImpl implements ExcelToImageConverter {

    @Value("${training.image.cell.height:80}")
    private int cellHeight;

    @Value("${training.image.header.height:100}")
    private int headerHeight;

    @Value("${training.image.footer.height:50}")
    private int footerHeight;

    @Value("${training.image.padding:40}")
    private int padding;

    @Value("${training.image.min.column.width:120}")
    private int minColumnWidth;

    @Value("${training.image.max.columns:50}")
    private int maxColumns;

    @Value("${training.image.max.rows:200}")
    private int maxRows;

    @Value("${training.image.optimize.empty.columns:true}")
    private boolean optimizeEmptyColumns;

    @Value("${training.image.chunk.size:10}")
    private int chunkSize;

    @Value("${training.image.direct.file.rendering.threshold:100}")
    private int directFileRenderingThreshold;

    @Value("${training.image.temp.dir:temp_images}")
    private String tempImageDir;

    private final ImageRendererImpl imageRenderer;

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
        log.info("EXCEL_В_ИЗОБРАЖЕНИЕ_КОНВЕРТАЦИЯ_НАЧАЛО файл {} формат {}", excelFile.getName(), outputFormat);

        ExcelUtils.validateExcelFile(excelFile);

        try (InputStream inputStream = new FileInputStream(excelFile)) {
            Workbook workbook = createWorkbook(excelFile, inputStream);

            try {
                log.info("EXCEL_КОНВЕРТАЦИЯ_EXCEL_ОБРАБОТКА листов в книге {}", workbook.getNumberOfSheets());

                Sheet sheet = getFirstSheet(workbook);
                return renderSheetToImageOptimized(sheet);

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

    /** {@inheritDoc} */
    @Override
    public BufferedImage renderSheetToImage(Sheet sheet) {
        return renderSheetToImageOptimized(sheet);
    }

    /**
     * Оптимизированный метод рендеринга листа в изображение.
     * Использует различные стратегии в зависимости от размера таблицы.
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

        if (estimatedMemory > 100_000_000L || actualRows > directFileRenderingThreshold) {
            log.info("EXCEL_КОНВЕРТАЦИЯ_РЕЖИМ_ФАЙЛОВОГО_РЕНДЕРИНГА использование рендеринга напрямую в файл");
            try {
                return renderToFileAndLoad(sheet, actualColumns, actualRows, imageWidth, imageHeight,
                        columnWidth, nonEmptyColumnIndices, nonEmptyRowIndices);
            } catch (IOException e) {
                log.warn("EXCEL_КОНВЕРТАЦИЯ_ФАЙЛОВЫЙ_РЕНДЕРИНГ_ОШИБКА возвращаемся к чанковому рендерингу: {}", e.getMessage());
            }
        }

        if (estimatedMemory > 50_000_000L) {
            log.info("EXCEL_КОНВЕРТАЦИЯ_БЕЗОПАСНЫЙ_РЕЖИМ использование чанковой отрисовки");
            return renderImageChunked(sheet, actualColumns, actualRows, imageWidth, imageHeight,
                    columnWidth, nonEmptyColumnIndices, nonEmptyRowIndices);
        }

        log.info("EXCEL_КОНВЕРТАЦИЯ_РАСЧЕТ_РАЗМЕРОВ изображение {}x{} таблица {}px столбец {}px",
                imageWidth, imageHeight, tableWidth, columnWidth);

        return renderImageDirect(sheet, actualColumns, actualRows, imageWidth, imageHeight,
                columnWidth, nonEmptyColumnIndices, nonEmptyRowIndices);
    }

    /**
     * Оптимизированный прямой рендеринг в память.
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
            ExcelUtils.configureGraphicsQuality(graphics);
            imageRenderer.drawImageContent(graphics, sheet, columnCount, rowCount, imageHeight, imageWidth,
                    columnWidth, columnIndices, rowIndices);
        } finally {
            graphics.dispose();
        }

        log.info("EXCEL_КОНВЕРТАЦИЯ_ИЗОБРАЖЕНИЕ_СОЗДАНО размер {}x{}", image.getWidth(), image.getHeight());
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
        log.info("ЧАНКОВЫЙ_РЕНДЕРИНГ_НАЧАЛО {}x{} строк {} колонок {}", imageWidth, imageHeight, rowCount, columnCount);

        BufferedImage image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();

        try {
            ExcelUtils.configureGraphicsQuality(graphics);
            graphics.setColor(imageRenderer.getBackgroundColor());
            graphics.fillRect(0, 0, imageWidth, imageHeight);

            imageRenderer.drawHeader(graphics, imageWidth);

            int tableWidth = columnWidth * columnCount;
            int tableStartY = headerHeight + padding;
            int tableStartX = padding;

            if (tableWidth + (2 * padding) > imageWidth) {
                tableStartX = (imageWidth - tableWidth) / 2;
            }

            imageRenderer.drawTableHeader(graphics, sheet, tableWidth, columnWidth, tableStartY, columnCount, tableStartX, columnIndices);

            Font cellFont = new Font("Arial", Font.PLAIN, 18);
            graphics.setFont(cellFont);

            int actualChunkSize = Math.min(chunkSize, rowCount);

            for (int chunkStart = 0; chunkStart < rowCount; chunkStart += actualChunkSize) {
                int chunkEnd = Math.min(chunkStart + actualChunkSize, rowCount);

                for (int i = chunkStart; i < chunkEnd && i < rowIndices.size(); i++) {
                    drawSingleRow(graphics, sheet, rowIndices.get(i), i, tableStartX, tableStartY,
                            tableWidth, columnWidth, columnCount, columnIndices, cellFont);
                }

                if (chunkStart > 0 && chunkStart % 50 == 0) {
                    System.gc();
                    log.debug("ЧАНКОВЫЙ_РЕНДЕРИНГ_ПРОГРЕСС обработано {} строк из {}", chunkStart, rowCount);
                }
            }

            drawTableGrid(graphics, tableStartX, tableStartY, tableWidth, columnWidth,
                    columnCount, rowCount, cellHeight);

            imageRenderer.drawFooter(graphics, imageHeight, imageWidth);

        } finally {
            graphics.dispose();
        }

        log.info("ЧАНКОВЫЙ_РЕНДЕРИНГ_УСПЕХ изображение создано");
        return image;
    }

    /**
     * Рисует одиночную строку таблицы.
     *
     * @param graphics контекст графики
     * @param sheet лист Excel
     * @param rowIndex индекс строки в Excel
     * @param displayIndex индекс отображения строки
     * @param tableStartX начальная X координата таблицы
     * @param tableStartY начальная Y координата таблицы
     * @param tableWidth ширина таблицы
     * @param columnWidth ширина столбца
     * @param columnCount количество столбцов
     * @param columnIndices индексы столбцов
     * @param cellFont шрифт для ячеек
     */
    private void drawSingleRow(Graphics2D graphics, Sheet sheet, int rowIndex, int displayIndex,
                               int tableStartX, int tableStartY, int tableWidth, int columnWidth,
                               int columnCount, List<Integer> columnIndices, Font cellFont) {
        Row row = sheet.getRow(rowIndex);
        int y = tableStartY + (displayIndex + 1) * cellHeight;

        Color rowColor = (displayIndex % 2 == 0) ? imageRenderer.getOddRowColor() : imageRenderer.getEvenRowColor();
        graphics.setColor(rowColor);
        graphics.fillRect(tableStartX, y, tableWidth, cellHeight);

        graphics.setColor(imageRenderer.getCellBorderColor());
        graphics.drawRect(tableStartX, y, tableWidth, cellHeight);

        if (row != null) {
            for (int j = 0; j < columnCount && j < columnIndices.size(); j++) {
                drawSingleCell(graphics, row, columnIndices.get(j), j, tableStartX, y, columnWidth, cellFont);
            }
        }
    }

    /**
     * Рисует одиночную ячейку таблицы.
     *
     * @param graphics контекст графики
     * @param row строка Excel
     * @param columnIndex индекс столбца в Excel
     * @param displayColumnIndex индекс отображения столбца
     * @param tableStartX начальная X координата таблицы
     * @param y Y координата строки
     * @param columnWidth ширина столбца
     * @param cellFont шрифт для ячеек
     */
    private void drawSingleCell(Graphics2D graphics, Row row, int columnIndex, int displayColumnIndex,
                                int tableStartX, int y, int columnWidth, Font cellFont) {
        Cell cell = row.getCell(columnIndex);
        int x = tableStartX + displayColumnIndex * columnWidth;
        int textY = y + cellHeight - 15;

        String cellValue = ExcelUtils.getCellValueAsString(cell);
        if (cellValue != null && !cellValue.isEmpty()) {
            FontMetrics metrics = graphics.getFontMetrics(cellFont);

            if (displayColumnIndex == 0) {
                graphics.setColor(imageRenderer.getVerticalTextColor());
            } else {
                graphics.setColor(imageRenderer.getHorizontalTextColor());
            }

            String trimmedValue = ExcelUtils.trimTextToFit(cellValue, metrics, columnWidth - 20);
            int textX = x + (columnWidth - metrics.stringWidth(trimmedValue)) / 2;
            graphics.drawString(trimmedValue, textX, textY);
        }

        graphics.setColor(imageRenderer.getCellBorderColor());
        graphics.drawLine(x, y, x, y + cellHeight);
    }

    /**
     * Рисует сетку таблицы.
     *
     * @param graphics контекст графики
     * @param tableStartX начальная X координата таблицы
     * @param tableStartY начальная Y координата таблицы
     * @param tableWidth ширина таблицы
     * @param columnWidth ширина столбца
     * @param columnCount количество столбцов
     * @param rowCount количество строк
     * @param cellHeight высота ячейки
     */
    private void drawTableGrid(Graphics2D graphics, int tableStartX, int tableStartY,
                               int tableWidth, int columnWidth, int columnCount,
                               int rowCount, int cellHeight) {
        for (int j = 0; j <= columnCount; j++) {
            int x = tableStartX + j * columnWidth;
            graphics.drawLine(x, tableStartY, x, tableStartY + (rowCount + 1) * cellHeight);
        }

        for (int i = 0; i <= rowCount; i++) {
            int y = tableStartY + (i + 1) * cellHeight;
            graphics.drawLine(tableStartX, y, tableStartX + tableWidth, y);
        }
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
        log.info("ФАЙЛОВЫЙ_РЕНДЕРИНГ_НАЧАЛО {}x{} строк {} колонок", imageWidth, imageHeight, rowCount);

        Path tempDir = Paths.get(tempImageDir);
        if (!Files.exists(tempDir)) {
            Files.createDirectories(tempDir);
        }

        Path tempFile = tempDir.resolve("temp_render_" + System.currentTimeMillis() + ".png");

        try {
            renderToFile(sheet, columnCount, rowCount, imageWidth, imageHeight,
                    columnWidth, columnIndices, rowIndices, tempFile);

            long fileSize = Files.size(tempFile);
            log.info("ФАЙЛОВЫЙ_РЕНДЕРИНГ_СОХРАНЕНИЕ файл создан {} размер {} байт",
                    tempFile, fileSize);

            BufferedImage image = ImageIO.read(tempFile.toFile());

            if (image != null) {
                log.info("ФАЙЛОВЫЙ_РЕНДЕРИНГ_ЗАГРУЗКА изображение загружено {}x{}",
                        image.getWidth(), image.getHeight());
            } else {
                log.error("ФАЙЛОВЫЙ_РЕНДЕРИНГ_ОШИБКА не удалось загрузить изображение из файла");
                throw new IOException("Не удалось загрузить изображение из временного файла");
            }

            return image;

        } finally {
            try {
                Files.deleteIfExists(tempFile);
                log.debug("ФАЙЛОВЫЙ_РЕНДЕРИНГ_ОЧИСТКА временный файл удален");
            } catch (IOException e) {
                log.warn("ФАЙЛОВЫЙ_РЕНДЕРИНГ_ОЧИСТКА_ОШИБКА не удалось удалить временный файл: {}", e.getMessage());
            }
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
            ExcelUtils.configureGraphicsQuality(graphics);
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
     *
     * @param sheet лист Excel для анализа
     * @return результат анализа листа
     */
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

    /**
     * Рассчитывает оптимальную ширину столбцов на основе содержимого ячеек.
     *
     * @param sheet лист Excel
     * @param columnIndices индексы столбцов для анализа
     * @param rowIndices индексы строк для анализа
     * @return оптимальная ширина столбца в пикселях
     */
    private int calculateOptimalColumnWidth(Sheet sheet, List<Integer> columnIndices, List<Integer> rowIndices) {
        int maxCellWidth = minColumnWidth;
        Font tempFont = new Font("Arial", Font.BOLD, 18);

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
                        String cellValue = ExcelUtils.getCellValueAsString(cell);
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

    /**
     * Создает объект Workbook в зависимости от формата файла.
     *
     * @param excelFile Excel файл
     * @param inputStream поток данных файла
     * @return объект Workbook
     * @throws IOException если произошла ошибка чтения файла
     */
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

    /**
     * Получает первый лист из книги.
     *
     * @param workbook книга Excel
     * @return первый лист книги
     */
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

    /**
     * Результат анализа листа Excel.
     */
    private static class SheetAnalysisResult {
        final int actualRows;
        final int actualColumns;
        final List<Integer> nonEmptyColumnIndices;
        final List<Integer> nonEmptyRowIndices;

        SheetAnalysisResult(int actualRows, int actualColumns,
                            List<Integer> nonEmptyColumnIndices, List<Integer> nonEmptyRowIndices) {
            this.actualRows = actualRows;
            this.actualColumns = actualColumns;
            this.nonEmptyColumnIndices = nonEmptyColumnIndices;
            this.nonEmptyRowIndices = nonEmptyRowIndices;
        }
    }
}