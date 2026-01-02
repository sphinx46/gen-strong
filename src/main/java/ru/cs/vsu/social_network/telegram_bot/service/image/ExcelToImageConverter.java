package ru.cs.vsu.social_network.telegram_bot.service.image;

import org.apache.poi.ss.usermodel.Sheet;

import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Конвертер Excel файлов в изображения.
 */
public interface ExcelToImageConverter {

    /**
     * Конвертирует Excel файл в изображение.
     *
     * @param excelFile исходный Excel файл
     * @param outputFormat формат выходного изображения
     * @return BufferedImage с содержимым Excel
     */
    BufferedImage convertExcelToImage(File excelFile, String outputFormat);

    /**
     * Рендерит лист Excel в изображение.
     *
     * @param sheet лист Excel
     * @return BufferedImage с содержимым листа
     */
    BufferedImage renderSheetToImage(Sheet sheet);
}