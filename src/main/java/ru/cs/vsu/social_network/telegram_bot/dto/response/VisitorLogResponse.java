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

    @Schema(description = "Количество посетителей")
    private Integer visitorCount;

    @Schema(description = "Сырые данные о посетителях")
    private String rawData;

    @Schema(description = "Дата посещений")
    private LocalDate logDate;

    @Schema(description = "Форматированный отчет для отображения")
    private String formattedReport;

    @Schema(description = "Количество новых пользователей")
    private Integer newUsersCount;

    @Schema(description = "Время создания журнала")
    private LocalDateTime createdAt;

    @Schema(description = "Время обновления журнала")
    private LocalDateTime updatedAt;
}