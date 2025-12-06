package ru.cs.vsu.social_network.telegram_bot.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "visit", indexes = {
        @Index(name = "idx_visit_visit_date", columnList = "visit_date"),
        @Index(name = "idx_visit_user_date", columnList = "user_id, visit_date", unique = true)
})
public class Visit extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "visit_date")
    private LocalDateTime visitDate;
}
