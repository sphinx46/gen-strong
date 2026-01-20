package ru.cs.vsu.social_network.telegram_bot.service.training.strategy.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.cs.vsu.social_network.telegram_bot.dto.response.TrainingCycleInfo;

/**
 * Стратегия генерации тренировочного плана для цикла "СТО-2ж"
 */
@Slf4j
@Component
public class Sto2gTrainingPlanGenerationStrategy extends AbstractTrainingPlanGenerationStrategy {

    @Value("${training.template.sto-2g.path:training_cycles/STO-2g.xlsx}")
    private String templatePath;

    /**
     * {@inheritDoc}
     */
    @Override
    public TrainingCycleInfo getCycleInfo() {
        return TrainingCycleInfo.builder()
                .id("sto-2g")
                .name("STO-2G")
                .displayName("СТО-2ж")
                .templatePath(templatePath)
                .author("А.E. Суровецкий")
                .build();
    }
}