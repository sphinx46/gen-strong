package ru.cs.vsu.social_network.telegram_bot.service.command;

import lombok.extern.slf4j.Slf4j;
import ru.cs.vsu.social_network.telegram_bot.dto.response.UserInfoResponse;
import ru.cs.vsu.social_network.telegram_bot.service.UserService;
import ru.cs.vsu.social_network.telegram_bot.validation.UserValidator;

import java.util.Map;
import java.util.UUID;

/**
 * Базовый класс для всех команд Telegram бота.
 * Содержит общие зависимости и методы для обработки команд.
 */
@Slf4j
public abstract class BaseTelegramCommand implements TelegramCommand {

    protected static final String SERVICE_NAME = "TELEGRAM_COMMAND";

    protected final UserService userService;
    protected final UserValidator userValidator;

    protected Map<Long, String> userStates;
    protected Map<Long, String> adminStates;
    protected Map<Long, Double> pendingBenchPressValues;

    /**
     * Конструктор базового класса команд.
     *
     * @param userService сервис для работы с пользователями
     * @param userValidator валидатор пользователей
     */
    protected BaseTelegramCommand(UserService userService, UserValidator userValidator) {
        this.userService = userService;
        this.userValidator = userValidator;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setUserStates(Map<Long, String> userStates) {
        this.userStates = userStates;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAdminStates(Map<Long, String> adminStates) {
        this.adminStates = adminStates;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setPendingBenchPressValues(Map<Long, Double> pendingBenchPressValues) {
        this.pendingBenchPressValues = pendingBenchPressValues;
    }

    /**
     * Получает информацию о пользователе.
     *
     * @param telegramId идентификатор пользователя в Telegram
     * @return информация о пользователе
     */
    protected UserInfoResponse getUserInfo(Long telegramId) {
        return userService.getUserByTelegramId(telegramId);
    }

    /**
     * Проверяет, является ли пользователь администратором.
     *
     * @param userId идентификатор пользователя
     * @return true если пользователь является администратором
     */
    protected boolean isAdmin(UUID userId) {
        try {
            userValidator.validateAdminAccessById(userId);
            return true;
        } catch (Exception e) {
            log.warn("{}_ADMIN_VALIDATION_ERROR: пользователь {} не является администратором: {}",
                    SERVICE_NAME, userId, e.getMessage());
            return false;
        }
    }
}