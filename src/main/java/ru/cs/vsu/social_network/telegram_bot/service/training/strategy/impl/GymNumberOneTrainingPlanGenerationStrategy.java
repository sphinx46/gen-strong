package ru.cs.vsu.social_network.telegram_bot.service.training.strategy.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.cs.vsu.social_network.telegram_bot.dto.response.TrainingCycleInfo;

/**
 * Стратегия генерации тренировочного плана для цикла "Жим номер один"
 */
@Slf4j
@Component
public class GymNumberOneTrainingPlanGenerationStrategy extends AbstractTrainingPlanGenerationStrategy {

    @Value("${training.template.gym-number-one.path:training_cycles/gym-number-one.xlsx}")
    private String templatePath;

    /**
     * {@inheritDoc}
     */
    @Override
    public TrainingCycleInfo getCycleInfo() {
        return TrainingCycleInfo.builder()
                .id("gym-number-one")
                .name("Gym-number-one")
                .displayName("Жим номер один")
                .description("Тренировочный цикл 'Жим номер один'")
                .templatePath(templatePath)
                .author("А.E. Суровецкий")
                .build();
    }
}