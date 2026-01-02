package ru.cs.vsu.social_network.telegram_bot.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_training",
        indexes = {
                @Index(name = "idx_user_training_user_id", columnList = "user_id"),
                @Index(name = "idx_user_training_updated", columnList = "updated_at")
        })
@Getter
@Setter
public class UserTraining extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "max_bench_press")
    private Double maxBenchPress;

    @Column(name = "last_training_date")
    private LocalDateTime lastTrainingDate;

    @Column(name = "training_cycle")
    private String trainingCycle;

    @PrePersist
    @PreUpdate
    protected void updateTimestamps() {
        if (this.getCreatedAt() == null) {
            this.setCreatedAt(LocalDateTime.now());
        }
        this.setUpdatedAt(LocalDateTime.now());

        if (lastTrainingDate == null) {
            lastTrainingDate = LocalDateTime.now();
        }
    }
}