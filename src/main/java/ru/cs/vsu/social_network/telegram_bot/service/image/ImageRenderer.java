package ru.cs.vsu.social_network.telegram_bot.service.image;

import org.apache.poi.ss.usermodel.Sheet;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Рендерер для отрисовки Excel данных в изображение.
 */
public interface ImageRenderer {

    /**
     * Рисует содержимое Excel листа на изображении.
     *
     * @param graphics объект Graphics2D для рисования
     * @param sheet лист Excel
     * @param columnCount количество колонок
     * @param rowCount количество строк
     * @param imageHeight высота изображения
     * @param imageWidth ширина изображения
     * @param columnWidth ширина колонки
     * @param columnIndices индексы колонок
     * @param rowIndices индексы строк
     */
    void drawImageContent(Graphics2D graphics, Sheet sheet, int columnCount, int rowCount,
                          int imageHeight, int imageWidth, int columnWidth,
                          List<Integer> columnIndices, List<Integer> rowIndices);

    /**
     * Рисует заголовок изображения.
     *
     * @param graphics объект Graphics2D
     * @param imageWidth ширина изображения
     */
    void drawHeader(Graphics2D graphics, int imageWidth);

    /**
     * Рисует таблицу на изображении.
     *
     * @param graphics объект Graphics2D
     * @param sheet лист Excel
     * @param columnCount количество колонок
     * @param rowCount количество строк
     * @param imageWidth ширина изображения
     * @param columnWidth ширина колонки
     * @param columnIndices индексы колонок
     * @param rowIndices индексы строк
     */
    void drawTable(Graphics2D graphics, Sheet sheet, int columnCount, int rowCount,
                   int imageWidth, int columnWidth,
                   List<Integer> columnIndices, List<Integer> rowIndices);

    /**
     * Рисует подвал изображения.
     *
     * @param graphics объект Graphics2D
     * @param imageHeight высота изображения
     * @param imageWidth ширина изображения
     */
    void drawFooter(Graphics2D graphics, int imageHeight, int imageWidth);

    /**
     * Рисует строки таблицы.
     *
     * @param graphics объект Graphics2D
     * @param sheet лист Excel
     * @param tableWidth ширина таблицы
     * @param colWidth ширина колонки
     * @param tableStartY начальная Y координата таблицы
     * @param rowCount количество строк
     * @param columnCount количество колонок
     * @param tableStartX начальная X координата таблицы
     * @param columnIndices индексы колонок
     * @param rowIndices индексы строк
     */
    void drawTableRows(Graphics2D graphics, Sheet sheet, int tableWidth, int colWidth,
                       int tableStartY, int rowCount, int columnCount, int tableStartX,
                       List<Integer> columnIndices, List<Integer> rowIndices);

    /**
     * Рисует заголовок таблицы.
     *
     * @param graphics объект Graphics2D
     * @param sheet лист Excel
     * @param tableWidth ширина таблицы
     * @param colWidth ширина колонки
     * @param tableStartY начальная Y координата таблицы
     * @param columnCount количество колонок
     * @param tableStartX начальная X координата таблицы
     * @param columnIndices индексы колонок
     */
    void drawTableHeader(Graphics2D graphics, Sheet sheet, int tableWidth, int colWidth,
                         int tableStartY, int columnCount, int tableStartX,
                         List<Integer> columnIndices);
}