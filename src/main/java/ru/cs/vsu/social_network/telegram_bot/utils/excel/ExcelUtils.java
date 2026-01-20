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