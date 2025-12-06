package ru.cs.vsu.social_network.telegram_bot.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import ru.cs.vsu.social_network.telegram_bot.entity.enums.ROLE;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "Ответ с данными о пользователе")
public class UserInfoResponse {
    @Schema(description = "Идентификатор пользователя")
    private UUID id;

    @Schema(description = "Идентификатор пользователя в телеграм")
    private Long telegramId;

    @Schema(description = "Никнейм пользователя")
    private String username;

    @Schema(description = "Имя пользователя")
    private String firstName;

    @Schema(description = "Фамилия пользователя")
    private String lastName;

    @Schema(description = "Никнейм для отображения в боте")
    private String displayName;

    @Schema(description = "Роль пользователя")
    private ROLE role;

    @Schema(description = "Дата регистрации")
    private LocalDateTime registeredAt;

    @Schema(description = "Общее количество посещений")
    private Long totalVisits;
}
