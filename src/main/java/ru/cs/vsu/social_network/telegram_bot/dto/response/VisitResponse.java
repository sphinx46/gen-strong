package ru.cs.vsu.social_network.telegram_bot.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "Ответ с данными о посещении")
public class VisitResponse {
    @Schema(description = "Идентификатор посещения")
    private UUID id;

    @Schema(description = "ID пользователя")
    private UUID userId;

    @Schema(description = "Отображаемое имя пользователя")
    private String userDisplayName;

    @Schema(description = "Дата посещения")
    private LocalDate visitDate;

    @Schema(description = "Время создания записи")
    private LocalDateTime createdAt;
}
