package ru.cs.vsu.social_network.telegram_bot.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import ru.cs.vsu.social_network.telegram_bot.entity.enums.ROLE;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "Запрос на обновление пользователя")
public class UserUpdateRequest {
    @Schema(description = "Никнейм для отображения в боте")
    private String displayName;

    @Schema(description = "Роль пользователя")
    private ROLE role;
}