package ru.cs.vsu.social_network.telegram_bot.utils.excel;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import ru.cs.vsu.social_network.telegram_bot.exception.GenerateTrainingPlanException;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;

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

        return switch (cell.getCellType()) {
            case STRING -> {
                String stringValue = cell.getStringCellValue();
                yield stringValue != null && !stringValue.trim().isEmpty();
            }
            case NUMERIC, BOOLEAN, FORMULA -> true;
            default -> false;
        };
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
}