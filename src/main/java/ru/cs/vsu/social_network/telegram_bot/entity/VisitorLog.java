package ru.cs.vsu.social_network.telegram_bot.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "visitor_log", indexes = {
        @Index(name = "idx_visitor_log_date", columnList = "log_date", unique = true)
})
public class VisitorLog extends BaseEntity {
    @Column(name = "visitor_count")
    private Integer visitorCount;

    @Column(name = "raw_data", columnDefinition = "TEXT")
    private String rawData;

    @Column(name = "log_date")
    private LocalDate logDate;
}
