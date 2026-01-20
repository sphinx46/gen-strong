package ru.cs.vsu.social_network.telegram_bot.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import ru.cs.vsu.social_network.telegram_bot.entity.enums.FITNESS_GOAL;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
@Schema(description = "Запрос на сохранение фитнес-метрик пользователя")
public class UserMetricsRequest {

    @Schema(description = "Идентификатор пользователя в телеграм")
    private Long telegramId;

    @Schema(description = "Текущий вес в кг", example = "75.5")
    private Double weight;

    @Schema(description = "Фитнес-цель")
    private FITNESS_GOAL goal;

    @Schema(description = "Количество тренировок в неделю",example = "3")
    private Integer workoutsPerWeek;

    @Schema(description = "Тренировочный стаж в годах",example = "2.5")
    private Double trainingExperience;

    @Schema(description = "Возраст пользователя",example = "25")
    private Integer age;

    @Schema(description = "Дополнительный комментарий", example = "Хочу уделить больше внимания плечам")
    private String comment;
}