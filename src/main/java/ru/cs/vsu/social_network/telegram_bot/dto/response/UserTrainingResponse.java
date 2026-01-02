package ru.cs.vsu.social_network.telegram_bot.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "Ответ с данными о программе тренировок")
public class UserTrainingResponse {
    @Schema(description = "Идентификатор программы тренировок")
    private UUID id;

    @Schema(description = "Идентификатор пользователя")
    private UUID userId;

    @Schema(description = "Максимальный жим лежа в килограммах")
    private double maxBenchPress;

    @Schema(description = "Название тренировочного цикла")
    private String trainingCycle;

    @Schema(description = "Время создания журнала")
    private LocalDateTime createdAt;

    @Schema(description = "Время обновления журнала")
    private LocalDateTime updatedAt;
}