package ru.cs.vsu.social_network.telegram_bot.utils.factory.factoryImpl;

import org.springframework.stereotype.Component;
import ru.cs.vsu.social_network.telegram_bot.dto.request.UserCreateRequest;
import ru.cs.vsu.social_network.telegram_bot.entity.User;
import ru.cs.vsu.social_network.telegram_bot.entity.enums.ROLE;
import ru.cs.vsu.social_network.telegram_bot.utils.factory.AbstractEntityFactory;
import ru.cs.vsu.social_network.telegram_bot.utils.factory.UserFactory;

import java.util.UUID;

/**
 * Реализация фабрики для создания сущностей User.
 * Создает новые экземпляры пользователей на основе входных данных.
 */
@Component
public final class UserFactoryImpl extends AbstractEntityFactory<User, UserCreateRequest>
        implements UserFactory {

    /**
     * {@inheritDoc}
     */
    @Override
    protected User buildEntity(UserCreateRequest request) {
        return User.builder()
                .telegramId(request.getTelegramId())
                .username(request.getUsername())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .displayName(request.getDisplayName())
                .role(request.getRole() != null ? request.getRole() : ROLE.USER)
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected User buildEntityWithUserId(UUID userId, UserCreateRequest request) {
        return buildEntity(request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getFactoryName() {
        return "ПОЛЬЗОВАТЕЛЬ";
    }
}