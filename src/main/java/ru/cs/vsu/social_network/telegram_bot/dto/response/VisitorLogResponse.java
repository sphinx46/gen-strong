package ru.cs.vsu.social_network.telegram_bot.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "Ответ с данными о журнале посещений")
public class VisitorLogResponse {
    @Schema(description = "Идентификатор журнала посещений")
    private UUID id;

    @Schema(description = "Список посетителей (полные данные)")
    private List<UserInfoResponse> visitors;

    @Schema(description = "Количество посетителей")
    private Integer visitorsCount;

    @Schema(description = "Дата посещений")
    private LocalDate logDate;

    @Schema(description = "Форматированный отчет для отображения")
    private String formattedReport;

    @Schema(description = "Время создания журнала")
    private LocalDateTime createdAt;

    @Schema(description = "Время обновления журнала")
    private LocalDateTime updatedAt;
}