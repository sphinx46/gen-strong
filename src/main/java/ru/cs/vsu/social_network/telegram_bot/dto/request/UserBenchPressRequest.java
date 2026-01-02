package ru.cs.vsu.social_network.telegram_bot.dto.request;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "Запрос на добавление максимального жима лежа пользователя")
public class UserBenchPressRequest {
    @Min(20)
    @Max(1000)
    @Schema(description = "Максимальный жим лежа")
    private double maxBenchPress;
}