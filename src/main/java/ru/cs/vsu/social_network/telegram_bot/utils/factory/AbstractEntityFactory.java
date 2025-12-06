package ru.cs.vsu.social_network.telegram_bot.utils.factory;

import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

/**
 * Абстрактная реализация фабрики сущностей.
 * Предоставляет общую логику для создания сущностей с логированием.
 *
 * @param <T> тип создаваемой сущности
 * @param <R> тип запроса для создания сущности
 */
@Slf4j
public abstract class AbstractEntityFactory<T, R> implements EntityFactory<T, R> {

    /**
     * {@inheritDoc}
     */
    @Override
    public T create(UUID userId, R request) {
        log.info("{}_ФАБРИКА_СОЗДАНИЕ_НАЧАЛО: создание сущности для пользователя: {}",
                getFactoryName(), userId);

        T entity = buildEntity(userId, request);

        log.info("{}_ФАБРИКА_СОЗДАНИЕ_УСПЕХ: сущность создана для пользователя: {}",
                getFactoryName(), userId);
        return entity;
    }

    /**
     * Создает сущность на основе данных пользователя и запроса.
     *
     * @param userId идентификатор пользователя
     * @param request данные для создания сущности (может быть null)
     * @return созданная сущность
     */
    protected abstract T buildEntity(UUID userId, R request);

    /**
     * Возвращает имя фабрики для логирования.
     *
     * @return имя фабрики
     */
    protected abstract String getFactoryName();
}