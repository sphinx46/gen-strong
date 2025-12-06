package ru.cs.vsu.social_network.telegram_bot.service;

import ru.cs.vsu.social_network.telegram_bot.dto.request.UserCreateRequest;
import ru.cs.vsu.social_network.telegram_bot.dto.request.UserUpdateRequest;
import ru.cs.vsu.social_network.telegram_bot.dto.response.UserInfoResponse;
import ru.cs.vsu.social_network.telegram_bot.dto.response.pageable.PageResponse;
import ru.cs.vsu.social_network.telegram_bot.entity.enums.ROLE;
import ru.cs.vsu.social_network.telegram_bot.dto.request.pageable.PageRequest;

import java.util.UUID;

/**
 * Сервис для управления пользователями Telegram-бота тренажерного зала.
 * Обеспечивает регистрацию, обновление, поиск пользователей и управление их ролями.
 */
public interface UserService {

    /**
     * Регистрирует нового пользователя на основе данных из Telegram.
     * Создает запись пользователя с ролью USER по умолчанию.
     *
     * @param createRequest DTO с данными для создания пользователя
     * @return DTO созданного пользователя
     */
    UserInfoResponse registerUser(UserCreateRequest createRequest);

    /**
     * Обновляет данные существующего пользователя.
     * Позволяет изменить отображаемое имя и/или роль пользователя.
     *
     * @param userId идентификатор пользователя
     * @param updateRequest DTO с данными для обновления
     * @return DTO обновленного пользователя
     */
    UserInfoResponse updateUser(UUID userId, UserUpdateRequest updateRequest);

    /**
     * Получает информацию о пользователе по его идентификатору.
     * Включает базовые данные и статистику посещений.
     *
     * @param userId идентификатор пользователя
     * @return DTO с информацией о пользователе
     */
    UserInfoResponse getUserById(UUID userId);

    /**
     * Получает информацию о пользователе по Telegram ID.
     * Используется для быстрого поиска по Telegram идентификатору.
     *
     * @param telegramId идентификатор пользователя в Telegram
     * @return DTO с информацией о пользователе
     */
    UserInfoResponse getUserByTelegramId(Long telegramId);

    /**
     * Получает страницу пользователей с указанной ролью.
     * Используется администраторами для управления пользователями.
     *
     * @param role роль для фильтрации
     * @param pageRequest параметры пагинации и сортировки
     * @return страница с пользователями
     */
    PageResponse<UserInfoResponse> getUsersByRole(ROLE role, PageRequest pageRequest);

    /**
     * Изменяет роль пользователя.
     * Требует прав администратора для выполнения.
     *
     * @param adminUserId идентификатор администратора, выполняющего операцию
     * @param targetUserId идентификатор пользователя, чья роль изменяется
     * @param newRole новая роль пользователя
     * @return DTO пользователя с обновленной ролью
     */
    UserInfoResponse changeUserRole(UUID adminUserId, UUID targetUserId, ROLE newRole);

    /**
     * Обновляет отображаемое имя пользователя.
     * Используется при команде /start для настройки обращения к пользователю.
     *
     * @param userId идентификатор пользователя
     * @param displayName новое отображаемое имя
     * @return DTO пользователя с обновленным именем
     */
    UserInfoResponse updateDisplayName(UUID userId, String displayName);
}