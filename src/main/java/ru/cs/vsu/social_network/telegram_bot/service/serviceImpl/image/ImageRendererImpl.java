package ru.cs.vsu.social_network.telegram_bot.service.serviceImpl.image;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.cs.vsu.social_network.telegram_bot.service.image.ImageRenderer;
import ru.cs.vsu.social_network.telegram_bot.utils.excel.ExcelUtils;

import java.awt.*;
import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Реализация рендерера для отрисовки Excel данных в изображение с использованием Apache POI Cell.
 * Оптимизирован для работы с большими таблицами (30x30+) и HD качеством для Telegram.
 */
@Slf4j
@Service
public class ImageRendererImpl implements ImageRenderer {

    @Value("${training.image.cell.height:50}")
    @Getter
    private int cellHeight;

    @Value("${training.image.header.height:70}")
    @Getter
    private int headerHeight;

    @Value("${training.image.footer.height:30}")
    @Getter
    private int footerHeight;

    @Value("${training.image.padding:15}")
    @Getter
    private int padding;

    @Value("${training.image.font.size.title:24}")
    private int titleFontSize;

    @Value("${training.image.font.size.header:18}")
    private int headerFontSize;

    @Value("${training.image.font.size.cell:14}")
    private int cellFontSize;

    @Value("${training.image.font.name:Arial}")
    private String fontName;

    @Value("${training.image.max.hd.width:1920}")
    private int maxHDWidth;

    @Value("${training.image.max.hd.height:1080}")
    private int maxHDHeight;

    @Value("${training.image.quality.scale:0.7}")
    private double qualityScale;

    @Getter
    private final Color backgroundColor;
    @Getter
    private final Color headerColor;
    @Getter
    private final Color tableHeaderColor;
    @Getter
    private final Color cellBorderColor;
    @Getter
    private final Color oddRowColor;
    @Getter
    private final Color evenRowColor;
    @Getter
    private final Color horizontalTextColor;
    @Getter
    private final Color verticalTextColor;
    @Getter
    private final Color footerTextColor;

    private static final String FOOTER_PREFIX = "Сгенерировано Gen Strong ботом • ";
    private static final String DATE_TIME_FORMAT = "dd.MM.yyyy HH:mm";
    private static final int HEADER_TEXT_OFFSET = 20;
    private static final int CELL_TEXT_OFFSET = 10;
    private static final int TEXT_MARGIN = 10;
    private static final int MIN_COLUMN_WIDTH = 80;
    private static final int MAX_COLUMN_WIDTH = 450;

    /**
     * Конструктор инициализирует цветовую схему для рендеринга.
     */
    public ImageRendererImpl() {
        this.backgroundColor = new Color(250, 250, 250);
        this.headerColor = new Color(52, 152, 219);
        this.tableHeaderColor = new Color(41, 128, 185);
        this.cellBorderColor = new Color(210, 210, 210);
        this.oddRowColor = new Color(255, 255, 255);
        this.evenRowColor = new Color(245, 245, 245);
        this.horizontalTextColor = new Color(220, 0, 0);
        this.verticalTextColor = Color.BLACK;
        this.footerTextColor = new Color(100, 100, 100);
    }

    /** {@inheritDoc} */
    @Override
    public void drawImageContent(Graphics2D graphics, Sheet sheet, int columnCount, int rowCount,
                                 int imageHeight, int imageWidth, int columnWidth,
                                 List<Integer> columnIndices, List<Integer> rowIndices) {
        log.debug("РИСОВАНИЕ_КОНТЕНТА_НАЧАЛО: размер {}x{} столбцов {} строк {}",
                imageWidth, imageHeight, columnCount, rowCount);

        ExcelUtils.configureGraphicsQuality(graphics);

        graphics.setColor(backgroundColor);
        graphics.fillRect(0, 0, imageWidth, imageHeight);

        drawHeader(graphics, imageWidth);
        drawTableWithPOI(graphics, sheet, columnCount, rowCount, imageWidth, columnWidth, columnIndices, rowIndices);
        drawFooter(graphics, imageHeight, imageWidth);

        log.debug("РИСОВАНИЕ_КОНТЕНТА_ЗАВЕРШЕНО");
    }

    /**
     * Отрисовывает таблицу с использованием Apache POI Cell рендеринга.
     *
     * @param graphics объект Graphics2D для отрисовки
     * @param sheet лист Excel
     * @param columnCount количество столбцов
     * @param rowCount количество строк
     * @param imageWidth ширина изображения
     * @param columnWidth ширина столбца
     * @param columnIndices индексы столбцов
     * @param rowIndices индексы строк
     */
    private void drawTableWithPOI(Graphics2D graphics, Sheet sheet, int columnCount, int rowCount,
                                  int imageWidth, int columnWidth, List<Integer> columnIndices,
                                  List<Integer> rowIndices) {
        log.debug("РИСОВАНИЕ_ТАБЛИЦЫ_POI: столбцов {} строк {}", columnCount, rowCount);

        int tableWidth = columnWidth * columnCount;
        int tableStartY = headerHeight + padding;
        int tableStartX = padding;

        if (tableWidth + (2 * padding) > imageWidth) {
            tableStartX = (imageWidth - tableWidth) / 2;
            log.debug("РИСОВАНИЕ_ТАБЛИЦЫ_ЦЕНТРИРОВАНИЕ: смещение X: {}", tableStartX);
        }

        drawTableHeader(graphics, sheet, tableWidth, columnWidth, tableStartY, columnCount, tableStartX, columnIndices);
        drawTableRowsWithPOI(graphics, sheet, tableWidth, columnWidth, tableStartY,
                rowCount, columnCount, tableStartX, columnIndices, rowIndices);
    }

    /**
     * Отрисовывает строки таблицы с использованием Apache POI.
     *
     * @param graphics объект Graphics2D
     * @param sheet лист Excel
     * @param tableWidth ширина таблицы
     * @param colWidth ширина столбца
     * @param tableStartY начальная Y координата
     * @param rowCount количество строк
     * @param columnCount количество столбцов
     * @param tableStartX начальная X координата
     * @param columnIndices индексы столбцов
     * @param rowIndices индексы строк
     */
    private void drawTableRowsWithPOI(Graphics2D graphics, Sheet sheet, int tableWidth, int colWidth,
                                      int tableStartY, int rowCount, int columnCount, int tableStartX,
                                      List<Integer> columnIndices, List<Integer> rowIndices) {
        log.debug("РИСОВАНИЕ_СТРОК_ТАБЛИЦЫ_POI: строк {} столбцов {}", rowCount, columnCount);

        Font cellFont = new Font(fontName, Font.PLAIN, cellFontSize);
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
                    int textY = y + cellHeight - CELL_TEXT_OFFSET;

                    String cellValue = ExcelUtils.getCellValueAsString(cell);
                    if (cellValue != null && !cellValue.isEmpty()) {
                        FontMetrics metrics = graphics.getFontMetrics(cellFont);

                        if (j == 0) {
                            graphics.setColor(verticalTextColor);
                        } else {
                            graphics.setColor(horizontalTextColor);
                        }

                        String trimmedValue = ExcelUtils.trimTextToFit(cellValue, metrics, colWidth - TEXT_MARGIN);
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

    /** {@inheritDoc} */
    @Override
    public void drawHeader(Graphics2D graphics, int imageWidth) {
        log.debug("РИСОВАНИЕ_ЗАГОЛОВКА: ширина {}", imageWidth);

        graphics.setColor(headerColor);
        graphics.fillRect(0, 0, imageWidth, headerHeight);

        Font titleFont = new Font(fontName, Font.BOLD, titleFontSize);
        graphics.setFont(titleFont);
        graphics.setColor(Color.WHITE);

        String title = "ТРЕНИРОВОЧНЫЙ ПЛАН";
        FontMetrics titleMetrics = graphics.getFontMetrics(titleFont);
        int titleX = (imageWidth - titleMetrics.stringWidth(title)) / 2;
        graphics.drawString(title, titleX, headerHeight - HEADER_TEXT_OFFSET);
    }

    /** {@inheritDoc} */
    @Override
    public void drawTable(Graphics2D graphics, Sheet sheet, int columnCount, int rowCount,
                          int imageWidth, int columnWidth, List<Integer> columnIndices, List<Integer> rowIndices) {
        drawTableWithPOI(graphics, sheet, columnCount, rowCount, imageWidth, columnWidth, columnIndices, rowIndices);
    }

    /** {@inheritDoc} */
    @Override
    public void drawTableHeader(Graphics2D graphics, Sheet sheet, int tableWidth, int colWidth,
                                int tableStartY, int columnCount, int tableStartX, List<Integer> columnIndices) {
        log.debug("РИСОВАНИЕ_ЗАГОЛОВКА_ТАБЛИЦЫ: ширина {} столбцов {}", tableWidth, columnCount);

        graphics.setColor(tableHeaderColor);
        graphics.fillRect(tableStartX, tableStartY, tableWidth, cellHeight);

        graphics.setColor(Color.WHITE);
        Font headerFont = new Font(fontName, Font.BOLD, headerFontSize);
        graphics.setFont(headerFont);

        Row headerRow = sheet.getRow(0);
        if (headerRow != null) {
            for (int j = 0; j < columnCount && j < columnIndices.size(); j++) {
                int actualColumn = columnIndices.get(j);
                Cell cell = headerRow.getCell(actualColumn);
                int x = tableStartX + j * colWidth;
                int y = tableStartY + cellHeight - CELL_TEXT_OFFSET;

                String cellValue = ExcelUtils.getCellValueAsString(cell);
                if (cellValue != null && !cellValue.isEmpty()) {
                    FontMetrics metrics = graphics.getFontMetrics();
                    String trimmedValue = ExcelUtils.trimTextToFit(cellValue, metrics, colWidth - TEXT_MARGIN);
                    int textX = x + (colWidth - metrics.stringWidth(trimmedValue)) / 2;
                    graphics.drawString(trimmedValue, textX, y);
                }
            }
        }

        graphics.setColor(cellBorderColor);
        graphics.drawRect(tableStartX, tableStartY, tableWidth, cellHeight);
    }

    /** {@inheritDoc} */
    @Override
    public void drawTableRows(Graphics2D graphics, Sheet sheet, int tableWidth, int colWidth,
                              int tableStartY, int rowCount, int columnCount, int tableStartX,
                              List<Integer> columnIndices, List<Integer> rowIndices) {
        drawTableRowsWithPOI(graphics, sheet, tableWidth, colWidth, tableStartY,
                rowCount, columnCount, tableStartX, columnIndices, rowIndices);
    }

    /** {@inheritDoc} */
    @Override
    public void drawFooter(Graphics2D graphics, int imageHeight, int imageWidth) {
        log.debug("РИСОВАНИЕ_ПОДВАЛА: размер {}x{}", imageWidth, imageHeight);

        graphics.setColor(footerTextColor);
        Font footerFont = new Font(fontName, Font.ITALIC, 10);
        graphics.setFont(footerFont);

        String footerText = FOOTER_PREFIX +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT));

        FontMetrics footerMetrics = graphics.getFontMetrics();
        int footerX = (imageWidth - footerMetrics.stringWidth(footerText)) / 2;
        int footerY = imageHeight - 10;

        graphics.drawString(footerText, footerX, footerY);
    }

    /**
     * Рассчитывает оптимальный размер изображения для HD качества.
     *
     * @param tableWidth исходная ширина таблицы
     * @param tableHeight исходная высота таблицы
     * @return масштабированные размеры
     */
    public Dimension calculateHDSize(int tableWidth, int tableHeight) {
        int targetWidth = tableWidth;
        int targetHeight = tableHeight;

        if (tableWidth > maxHDWidth) {
            double scale = (double) maxHDWidth / tableWidth;
            targetWidth = maxHDWidth;
            targetHeight = (int) (tableHeight * scale * qualityScale);
            log.debug("МАСШТАБИРОВАНИЕ_ШИРИНЫ: {} -> {} (scale: {})", tableWidth, targetWidth, scale);
        }

        if (targetHeight > maxHDHeight) {
            double scale = (double) maxHDHeight / targetHeight;
            targetHeight = maxHDHeight;
            targetWidth = (int) (targetWidth * scale * qualityScale);
            log.debug("МАСШТАБИРОВАНИЕ_ВЫСОТЫ: {} -> {} (scale: {})", tableHeight, targetHeight, scale);
        }

        return new Dimension(targetWidth, targetHeight);
    }

    /**
     * Рассчитывает оптимальную ширину столбца на основе содержимого.
     *
     * @param sheet лист Excel
     * @param columnIndices индексы столбцов
     * @param sampleRows количество строк для анализа
     * @return оптимальная ширина столбца
     */
    public int calculateOptimalColumnWidth(Sheet sheet, List<Integer> columnIndices, int sampleRows) {
        int maxWidth = MIN_COLUMN_WIDTH;

        try {
            BufferedImage tempImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
            Graphics2D tempGraphics = tempImage.createGraphics();
            Font tempFont = new Font(fontName, Font.PLAIN, cellFontSize);
            tempGraphics.setFont(tempFont);
            FontMetrics metrics = tempGraphics.getFontMetrics();

            for (int colIdx : columnIndices) {
                int columnMaxWidth = MIN_COLUMN_WIDTH;

                for (int i = 0; i < Math.min(sampleRows, sheet.getLastRowNum()); i++) {
                    Row row = sheet.getRow(i);
                    if (row != null) {
                        Cell cell = row.getCell(colIdx);
                        String cellValue = ExcelUtils.getCellValueAsString(cell);
                        if (cellValue != null && !cellValue.isEmpty()) {
                            int textWidth = metrics.stringWidth(cellValue);
                            columnMaxWidth = Math.max(columnMaxWidth, textWidth + TEXT_MARGIN * 2);
                        }
                    }
                }

                maxWidth = Math.max(maxWidth, columnMaxWidth);
            }

            tempGraphics.dispose();
        } catch (Exception e) {
            log.warn("ОПТИМАЛЬНАЯ_ШИРИНА_СТОЛБЦА_ОШИБКА {}", e.getMessage());
        }

        return Math.min(maxWidth, MAX_COLUMN_WIDTH);
    }
}