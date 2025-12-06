package ru.cs.vsu.social_network.telegram_bot.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "Статистика за день")
public class DailyStatsResponse {
    @Schema(description = "Дата")
    private LocalDate date;

    @Schema(description = "Список посетителей")
    private List<String> visitorNames;

    @Schema(description = "Количество посетителей")
    private Integer visitorCount;

    @Schema(description = "Количество новых пользователей")
    private int newUsersCount;
}
