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
    private LocalDate date;
    private Integer visitorCount;
    private List<String> visitorNames;
}
