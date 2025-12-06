package ru.cs.vsu.social_network.telegram_bot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.*;
import ru.cs.vsu.social_network.telegram_bot.entity.enums.ROLE;

@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "app_user", indexes = {
        @Index(name = "idx_user_telegram_id", columnList = "telegram_id", unique = true)
})
public class User extends BaseEntity {
    @Column(name = "telegram_id")
    private Long telegramId;

    @Column(name = "username")
    private String username;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "role")
    private ROLE role;
}