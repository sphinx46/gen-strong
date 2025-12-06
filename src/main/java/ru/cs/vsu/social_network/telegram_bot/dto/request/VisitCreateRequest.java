package ru.cs.vsu.social_network.telegram_bot.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "Запрос на отметку в журнале посещений")
public class VisitCreateRequest {
    @Schema(description = "Идентификатор пользователя в телеграм")
    private Long telegramId;

    @Schema(description = "Дата посещения")
    private LocalDate visitDate;
}