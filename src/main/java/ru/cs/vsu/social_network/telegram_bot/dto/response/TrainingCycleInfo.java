package ru.cs.vsu.social_network.telegram_bot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для информации о тренировочном цикле
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainingCycleInfo {

    private String id;
    private String name;
    private String displayName;
    private String description;
    private String templatePath;
    private String author;
}