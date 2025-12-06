package ru.cs.vsu.social_network.telegram_bot.utils.factory;

import ru.cs.vsu.social_network.telegram_bot.dto.request.UserCreateRequest;
import ru.cs.vsu.social_network.telegram_bot.entity.User;

/**
 * Фабрика для создания сущностей User.
 * Определяет контракт для создания пользователей на основе запросов.
 */
public interface UserFactory extends EntityFactory<User, UserCreateRequest> {
}