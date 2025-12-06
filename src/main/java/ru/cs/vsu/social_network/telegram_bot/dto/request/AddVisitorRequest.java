package ru.cs.vsu.social_network.telegram_bot.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "Запрос на добавление посетителя в журнал за день")
public class AddVisitorRequest {
    @Schema(description = "Отображаемое имя посетителя")
    private String displayName;

    @Schema(description = "Дата посещения", defaultValue = "Сегодня")
    private LocalDate visitDate = LocalDate.now();
}