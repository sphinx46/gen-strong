package ru.cs.vsu.social_network.telegram_bot.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import ru.cs.vsu.social_network.telegram_bot.exception.GenerateTrainingPlanException;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Path;

/**
 * Утилитарный класс для работы с Excel файлами и изображениями.
 * Содержит методы для обработки Excel файлов, рендеринга и работы с графикой.
 */
@Slf4j
public class ExcelUtils {

    private ExcelUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Проверяет доступность Excel файла.
     *
     * @param excelFile файл для проверки
     * @throws GenerateTrainingPlanException если файл недоступен
     */
    public static void validateExcelFile(File excelFile) {
        if (!excelFile.exists()) {
            log.error("EXCEL_УТИЛИТЫ_ФАЙЛ_НЕ_СУЩЕСТВУЕТ {}", excelFile.getAbsolutePath());
            throw new GenerateTrainingPlanException("Excel файл не существует: " + excelFile.getAbsolutePath());
        }

        if (!excelFile.canRead()) {
            log.error("EXCEL_УТИЛИТЫ_ФАЙЛ_НЕДОСТУПЕН {}", excelFile.getAbsolutePath());
            throw new GenerateTrainingPlanException("Excel файл недоступен для чтения: " + excelFile.getAbsolutePath());
        }

        long fileSize = excelFile.length();
        if (fileSize == 0) {
            log.error("EXCEL_УТИЛИТЫ_ФАЙЛ_ПУСТОЙ {}", excelFile.getAbsolutePath());
            throw new GenerateTrainingPlanException("Excel файл пустой: " + excelFile.getAbsolutePath());
        }

        log.info("EXCEL_УТИЛИТЫ_ПРОВЕРКА_ФАЙЛА размер файла {} байт, путь: {}", fileSize, excelFile.getAbsolutePath());
    }

    /**
     * Проверяет наличие содержимого в ячейке Excel.
     *
     * @param cell ячейка для проверки
     * @return true если ячейка содержит данные
     */
    public static boolean hasCellContent(Cell cell) {
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

    /**
     * Преобразует значение ячейки в строку.
     *
     * @param cell ячейка Excel
     * @return строковое представление значения ячейки
     */
    public static String getCellValueAsString(Cell cell) {
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
                    log.warn("EXCEL_УТИЛИТЫ_ФОРМУЛА_ВЫЧИСЛЕНИЕ_ОШИБКА {}", e.getMessage());
                    return cell.getCellFormula();
                }
            default:
                return "";
        }
    }

    /**
     * Настраивает качество графики для Graphics2D.
     *
     * @param graphics объект Graphics2D для настройки
     */
    public static void configureGraphicsQuality(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        try {
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        } catch (Exception e) {
            log.warn("EXCEL_УТИЛИТЫ_СГЛАЖИВАНИЕ_ТЕКСТА_ОШИБКА {}", e.getMessage());
        }

        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    /**
     * Обрезает текст чтобы он помещался в заданную ширину.
     *
     * @param text текст для обрезки
     * @param metrics метрики шрифта
     * @param maxWidth максимальная ширина в пикселях
     * @return обрезанный текст с многоточием если необходимо
     */
    public static String trimTextToFit(String text, FontMetrics metrics, int maxWidth) {
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
     * Сохраняет изображение с компрессией.
     *
     * @param image изображение для сохранения
     * @param defaultImageFormat формат изображения
     * @param outputFile файл для сохранения
     * @throws IOException если произошла ошибка сохранения
     */
    public static void saveImageWithCompression(BufferedImage image, String defaultImageFormat, File outputFile) throws IOException {
        log.info("EXCEL_УТИЛИТЫ_СОХРАНЕНИЕ_ИЗОБРАЖЕНИЯ формат {} размер {}x{}",
                defaultImageFormat, image.getWidth(), image.getHeight());

        try (FileOutputStream imageOut = new FileOutputStream(outputFile)) {
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

            log.info("EXCEL_УТИЛИТЫ_ИЗОБРАЖЕНИЕ_СОХРАНЕНО файл {} размер {} байт",
                    outputFile.getAbsolutePath(), outputFile.length());
        }
    }

    /**
     * Создает объект Workbook в зависимости от формата файла.
     *
     * @param excelFile Excel файл
     * @param inputStream поток данных файла
     * @return объект Workbook
     * @throws IOException если произошла ошибка чтения файла
     */
    public static Workbook createWorkbook(File excelFile, InputStream inputStream) throws IOException {
        String fileName = excelFile.getName().toLowerCase();

        if (fileName.endsWith(".xlsx")) {
            log.info("EXCEL_УТИЛИТЫ_СОЗДАНИЕ_WORKBOOK формат XLSX файл {}", excelFile.getName());
            return new XSSFWorkbook(inputStream);
        } else if (fileName.endsWith(".xls")) {
            log.info("EXCEL_УТИЛИТЫ_СОЗДАНИЕ_WORKBOOK формат XLS файл {}", excelFile.getName());
            return new HSSFWorkbook(inputStream);
        } else {
            log.error("EXCEL_УТИЛИТЫ_НЕПОДДЕРЖИВАЕМЫЙ_ФОРМАТ {}", excelFile.getName());
            throw new GenerateTrainingPlanException("Неподдерживаемый формат Excel файла: " + excelFile.getName());
        }
    }

    /**
     * Получает первый лист из книги Excel.
     *
     * @param workbook книга Excel
     * @return первый лист книги
     * @throws GenerateTrainingPlanException если книга пустая или лист не найден
     */
    public static Sheet getFirstSheet(Workbook workbook) {
        if (workbook.getNumberOfSheets() == 0) {
            log.error("EXCEL_УТИЛИТЫ_ПУСТАЯ_КНИГА нет листов");
            throw new GenerateTrainingPlanException("Excel файл не содержит листов");
        }

        Sheet sheet = workbook.getSheetAt(0);
        if (sheet == null) {
            log.error("EXCEL_УТИЛИТЫ_ЛИСТ_НЕ_НАЙДЕН");
            throw new GenerateTrainingPlanException("Первый лист не найден");
        }

        log.info("EXCEL_УТИЛИТЫ_ЛИСТ_ПОЛУЧЕН строк: {}, столбцов: {}",
                sheet.getLastRowNum() + 1, getMaxColumnCount(sheet));
        return sheet;
    }

    /**
     * Рассчитывает максимальное количество столбцов в листе.
     *
     * @param sheet лист Excel
     * @return максимальное количество столбцов
     */
    public static int getMaxColumnCount(Sheet sheet) {
        int maxColumns = 0;
        for (Row row : sheet) {
            int lastCellNum = row.getLastCellNum();
            if (lastCellNum > maxColumns) {
                maxColumns = lastCellNum;
            }
        }
        return maxColumns;
    }

    /**
     * Анализирует ячейки листа для определения диапазона данных.
     *
     * @param sheet лист Excel
     * @return массив с минимальной и максимальной строкой и столбцом
     */
    public static int[] getDataRange(Sheet sheet) {
        int minRow = Integer.MAX_VALUE;
        int maxRow = -1;
        int minCol = Integer.MAX_VALUE;
        int maxCol = -1;

        for (Row row : sheet) {
            if (row != null) {
                int rowNum = row.getRowNum();
                if (rowNum < minRow) minRow = rowNum;
                if (rowNum > maxRow) maxRow = rowNum;

                for (Cell cell : row) {
                    if (cell != null && hasCellContent(cell)) {
                        int colNum = cell.getColumnIndex();
                        if (colNum < minCol) minCol = colNum;
                        if (colNum > maxCol) maxCol = colNum;
                    }
                }
            }
        }

        if (minRow == Integer.MAX_VALUE) minRow = 0;
        if (maxRow == -1) maxRow = 0;
        if (minCol == Integer.MAX_VALUE) minCol = 0;
        if (maxCol == -1) maxCol = 0;

        return new int[]{minRow, maxRow, minCol, maxCol};
    }

    /**
     * Вычисляет ширину столбца на основе содержимого ячеек.
     *
     * @param sheet лист Excel
     * @param columnIndices индексы столбцов
     * @param rowIndices индексы строк
     * @param minColumnWidth минимальная ширина столбца
     * @return оптимальная ширина столбца
     */
    public static int calculateOptimalColumnWidth(Sheet sheet, java.util.List<Integer> columnIndices,
                                                  java.util.List<Integer> rowIndices, int minColumnWidth) {
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
            log.warn("EXCEL_УТИЛИТЫ_ОПТИМАЛЬНАЯ_ШИРИНА_ОШИБКА {}", e.getMessage());
            return minColumnWidth;
        }

        return Math.min(maxCellWidth, 350);
    }

    /**
     * Создает временный файл для работы с изображением.
     *
     * @param prefix префикс имени файла
     * @param suffix суффикс имени файла
     * @param tempDir директория для временных файлов
     * @return путь к созданному временному файлу
     * @throws IOException если не удалось создать файл
     */
    public static Path createTempFile(String prefix, String suffix, String tempDir) throws IOException {
        java.nio.file.Path tempDirPath = java.nio.file.Paths.get(tempDir);
        if (!java.nio.file.Files.exists(tempDirPath)) {
            java.nio.file.Files.createDirectories(tempDirPath);
            log.info("EXCEL_УТИЛИТЫ_ДИРЕКТОРИЯ_СОЗДАНА {}", tempDirPath.toAbsolutePath());
        }

        String timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = String.format("%s_%s_%s", prefix, timestamp, suffix);
        return tempDirPath.resolve(filename);
    }

    /**
     * Удаляет временный файл с логированием.
     *
     * @param file файл для удаления
     */
    public static void deleteTempFile(File file) {
        if (file != null && file.exists()) {
            log.info("EXCEL_УТИЛИТЫ_УДАЛЕНИЕ_ВРЕМЕННОГО_ФАЙЛА {}", file.getAbsolutePath());
            if (file.delete()) {
                log.info("EXCEL_УТИЛИТЫ_ВРЕМЕННЫЙ_ФАЙЛ_УДАЛЕН");
            } else {
                log.warn("EXCEL_УТИЛИТЫ_ВРЕМЕННЫЙ_ФАЙЛ_НЕ_УДАЛЕН требуется ручное удаление");
            }
        }
    }

    /**
     * Проверяет требования к памяти для обработки таблицы.
     *
     * @param rows количество строк
     * @param columns количество столбцов
     * @param cellHeight высота ячейки
     * @param warningThreshold порог предупреждения в байтах
     */
    public static void checkMemoryRequirements(int rows, int columns, int cellHeight, long warningThreshold) {
        long estimatedMemory = (long) rows * columns * cellHeight * 120 * 4L;
        if (estimatedMemory > warningThreshold) {
            log.warn("EXCEL_УТИЛИТЫ_ПРОВЕРКА_ПАМЯТИ оценка памяти {} байт для {} строк {} колонок",
                    estimatedMemory, rows, columns);
            System.gc();
        }
    }

    /**
     * Загружает изображение из файла.
     *
     * @param imageFile файл с изображением
     * @return BufferedImage изображение
     * @throws IOException если не удалось загрузить изображение
     */
    public static BufferedImage loadImageFromFile(File imageFile) throws IOException {
        log.info("EXCEL_УТИЛИТЫ_ЗАГРУЗКА_ИЗОБРАЖЕНИЯ файл {} размер {} байт",
                imageFile.getAbsolutePath(), imageFile.length());

        BufferedImage image = ImageIO.read(imageFile);
        if (image == null) {
            throw new IOException("Не удалось загрузить изображение из файла: " + imageFile.getAbsolutePath());
        }

        log.info("EXCEL_УТИЛИТЫ_ИЗОБРАЖЕНИЕ_ЗАГРУЖЕНО размер {}x{}", image.getWidth(), image.getHeight());
        return image;
    }
}