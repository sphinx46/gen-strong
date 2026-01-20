package ru.cs.vsu.social_network.telegram_bot.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import ru.cs.vsu.social_network.telegram_bot.entity.enums.FITNESS_GOAL;


import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "Ответ с фитнес-метриками пользователя")
public class UserMetricsResponse {

    @Schema(description = "Идентификатор пользователя в телеграм")
    private Long telegramId;

    @Schema(description = "Имя пользователя для отображения")
    private String displayName;

    @Schema(description = "Текущий вес в кг")
    private Double weight;

    @Schema(description = "Фитнес-цель")
    private FITNESS_GOAL goal;

    @Schema(description = "Название цели на русском")
    private String goalRussianName;

    @Schema(description = "Количество тренировок в неделю")
    private Integer workoutsPerWeek;

    @Schema(description = "Тренировочный стаж в годах")
    private Double trainingExperience;

    @Schema(description = "Возраст пользователя")
    private Integer age;

    @Schema(description = "Дополнительный комментарий")
    private String comment;

    @Schema(description = "Дата и время создания записи")
    private LocalDateTime createdAt;

    @Schema(description = "Дата и время обновления записи")
    private LocalDateTime updatedAt;
}