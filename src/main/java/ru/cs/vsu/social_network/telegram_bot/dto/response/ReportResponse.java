package ru.cs.vsu.social_network.telegram_bot.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDate;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "Ответ с отчетом за период")
public class ReportResponse {
    @Schema(description = "Начальная дата периода")
    private LocalDate startDate;

    @Schema(description = "Конечная дата периода")
    private LocalDate endDate;

    @Schema(description = "Общее количество посещений")
    private Integer totalVisits;

    @Schema(description = "Количество уникальных посетителей")
    private Integer uniqueVisitors;

    @Schema(description = "Количество новых пользователей")
    private Integer totalNewUsers;

    @Schema(description = "Среднее количество посещений в день")
    private Double averageDailyVisits;

    @Schema(description = "Детали по дням")
    private Map<LocalDate, DailyStatsResponse> dailyStats;

    @Schema(description = "Форматированный отчет для Telegram")
    private String telegramFormattedReport;
}

