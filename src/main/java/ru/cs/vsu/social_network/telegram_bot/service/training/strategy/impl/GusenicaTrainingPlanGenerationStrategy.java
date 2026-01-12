package ru.cs.vsu.social_network.telegram_bot.service.training.strategy.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.cs.vsu.social_network.telegram_bot.dto.response.TrainingCycleInfo;

/**
 * Стратегия генерации тренировочного плана для цикла "Гусеница"
 */
@Slf4j
@Component
public class GusenicaTrainingPlanGenerationStrategy extends AbstractTrainingPlanGenerationStrategy {

    @Value("${training.template.gusenica.path:training_cycles/gusenica_cycle.xlsx}")
    private String templatePath;

    /**
     * {@inheritDoc}
     */
    @Override
    public TrainingCycleInfo getCycleInfo() {
        return TrainingCycleInfo.builder()
                .id("gusenica")
                .name("Гусеница")
                .displayName("Гусеница")
                .description("Тренировочный цикл 'Гусеница'")
                .templatePath(templatePath)
                .author("А.E. Суровецкий")
                .build();
    }
}