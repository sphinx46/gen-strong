package ru.cs.vsu.social_network.telegram_bot.service.serviceImpl.image;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.cs.vsu.social_network.telegram_bot.service.image.ImageRenderer;
import ru.cs.vsu.social_network.telegram_bot.utils.ExcelUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Реализация рендерера для отрисовки Excel данных в изображение.
 * Обеспечивает высококачественную визуализацию табличных данных.
 */
@Slf4j
@Service
public class ImageRendererImpl implements ImageRenderer {

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

    /**
     * Конструктор инициализирует цветовую схему для рендеринга.
     */
    public ImageRendererImpl() {
        this.backgroundColor = new Color(250, 250, 250);
        this.headerColor = new Color(52, 152, 219);
        this.tableHeaderColor = new Color(41, 128, 185);
        this.cellBorderColor = new Color(220, 220, 220);
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

        graphics.setColor(backgroundColor);
        graphics.fillRect(0, 0, imageWidth, imageHeight);

        drawHeader(graphics, imageWidth);
        drawTable(graphics, sheet, columnCount, rowCount, imageWidth, columnWidth, columnIndices, rowIndices);
        drawFooter(graphics, imageHeight, imageWidth);

        log.debug("РИСОВАНИЕ_КОНТЕНТА_ЗАВЕРШЕНО");
    }

    /** {@inheritDoc} */
    @Override
    public void drawHeader(Graphics2D graphics, int imageWidth) {
        log.debug("РИСОВАНИЕ_ЗАГОЛОВКА: ширина {}", imageWidth);

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

    /** {@inheritDoc} */
    @Override
    public void drawTable(Graphics2D graphics, Sheet sheet, int columnCount, int rowCount,
                          int imageWidth, int columnWidth, List<Integer> columnIndices, List<Integer> rowIndices) {
        log.debug("РИСОВАНИЕ_ТАБЛИЦЫ: столбцов {} строк {}", columnCount, rowCount);

        int tableWidth = columnWidth * columnCount;
        int tableStartY = headerHeight + padding;
        int tableStartX = padding;

        if (tableWidth + (2 * padding) > imageWidth) {
            tableStartX = (imageWidth - tableWidth) / 2;
            log.debug("РИСОВАНИЕ_ТАБЛИЦЫ_ЦЕНТРИРОВАНИЕ: смещение X: {}", tableStartX);
        }

        drawTableHeader(graphics, sheet, tableWidth, columnWidth, tableStartY, columnCount, tableStartX, columnIndices);
        drawTableRows(graphics, sheet, tableWidth, columnWidth, tableStartY, rowCount, columnCount, tableStartX, columnIndices, rowIndices);
    }

    /** {@inheritDoc} */
    @Override
    public void drawTableHeader(Graphics2D graphics, Sheet sheet, int tableWidth, int colWidth,
                                int tableStartY, int columnCount, int tableStartX, List<Integer> columnIndices) {
        log.debug("РИСОВАНИЕ_ЗАГОЛОВКА_ТАБЛИЦЫ: ширина {} столбцов {}", tableWidth, columnCount);

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

                String cellValue = ExcelUtils.getCellValueAsString(cell);
                if (cellValue != null && !cellValue.isEmpty()) {
                    FontMetrics metrics = graphics.getFontMetrics();
                    String trimmedValue = ExcelUtils.trimTextToFit(cellValue, metrics, colWidth - 20);
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
        log.debug("РИСОВАНИЕ_СТРОК_ТАБЛИЦЫ: строк {} столбцов {}", rowCount, columnCount);

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

                    String cellValue = ExcelUtils.getCellValueAsString(cell);
                    if (cellValue != null && !cellValue.isEmpty()) {
                        FontMetrics metrics = graphics.getFontMetrics(cellFont);

                        if (j == 0) {
                            graphics.setColor(verticalTextColor);
                        } else {
                            graphics.setColor(horizontalTextColor);
                        }

                        String trimmedValue = ExcelUtils.trimTextToFit(cellValue, metrics, colWidth - 20);
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
    public void drawFooter(Graphics2D graphics, int imageHeight, int imageWidth) {
        log.debug("РИСОВАНИЕ_ПОДВАЛА: размер {}x{}", imageWidth, imageHeight);

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
}