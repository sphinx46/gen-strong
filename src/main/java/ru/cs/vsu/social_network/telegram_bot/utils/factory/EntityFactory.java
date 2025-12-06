package ru.cs.vsu.social_network.telegram_bot.utils.factory;

import java.util.UUID;

/**
 * Обобщенная фабрика для создания сущностей.
 * Определяет базовый контракт для создания сущностей на основе запросов.
 *
 * @param <T> тип создаваемой сущности
 * @param <R> тип запроса для создания сущности
 */
public interface EntityFactory<T, R> {

    /**
     * Создает новую сущность на основе данных пользователя и запроса.
     *
     * @param userId идентификатор пользователя
     * @param request данные для создания сущности
     * @return созданная сущность
     */
    T create(UUID userId, R request);
}