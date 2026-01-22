package ru.cs.vsu.social_network.telegram_bot.entity;

import jakarta.persistence.*;
import lombok.*;
import ru.cs.vsu.social_network.telegram_bot.entity.enums.FITNESS_GOAL;

@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "user_metrics", indexes = {
        @Index(name = "idx_user_metrics_telegram_id", columnList = "telegram_id", unique = true),
        @Index(name = "idx_user_metrics_created_at", columnList = "created_at")
})
public class UserMetrics extends BaseEntity {

    @Column(name = "telegram_id", nullable = false, unique = true)
    private Long telegramId;

    @Column(name = "weight")
    private Double weight;

    @Enumerated(EnumType.STRING)
    @Column(name = "goal")
    private FITNESS_GOAL goal;

    @Column(name = "goal_russian_name")
    private String goalRussianName;

    @Column(name = "workouts_per_week")
    private Integer workoutsPerWeek;

    @Column(name = "training_experience")
    private Double trainingExperience;

    @Column(name = "age")
    private Integer age;

    @Column(name = "comment", length = 1000)
    private String comment;
}