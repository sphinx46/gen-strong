package ru.cs.vsu.social_network.telegram_bot.service.command;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Фабрика для создания и управления командами.
 * Обеспечивает инъекцию зависимостей в команды.
 */
@Component
public class CommandFactory {

    private final Map<String, TelegramCommand> commands = new ConcurrentHashMap<>();
    private final Map<Long, String> userStates = new ConcurrentHashMap<>();
    private final Map<Long, String> adminStates = new ConcurrentHashMap<>();
    private final Map<Long, Double> pendingBenchPressValues = new ConcurrentHashMap<>();

    /**
     * Регистрирует команду в фабрике.
     *
     * @param commandName имя команды
     * @param command объект команды
     */
    public void registerCommand(String commandName, TelegramCommand command) {
        if (command instanceof BaseTelegramCommand) {
            BaseTelegramCommand baseCommand = (BaseTelegramCommand) command;
            baseCommand.setUserStates(userStates);
            baseCommand.setAdminStates(adminStates);
            baseCommand.setPendingBenchPressValues(pendingBenchPressValues);
        }
        commands.put(commandName.toLowerCase(), command);
    }

    /**
     * Получает команду по имени.
     *
     * @param commandName имя команды
     * @return объект команды или null если не найдена
     */
    public TelegramCommand getCommand(String commandName) {
        return commands.get(commandName.toLowerCase());
    }

    /**
     * Получает карту состояний пользователей.
     *
     * @return карта состояний пользователей
     */
    public Map<Long, String> getUserStates() {
        return userStates;
    }

    /**
     * Получает карту состояний администраторов.
     *
     * @return карта состояний администраторов
     */
    public Map<Long, String> getAdminStates() {
        return adminStates;
    }

    /**
     * Получает карту значений жима лежа.
     *
     * @return карта значений жима лежа
     */
    public Map<Long, Double> getPendingBenchPressValues() {
        return pendingBenchPressValues;
    }

    /**
     * Очищает состояние пользователя.
     *
     * @param telegramId идентификатор пользователя
     */
    public void clearUserState(Long telegramId) {
        userStates.remove(telegramId);
        adminStates.remove(telegramId);
        pendingBenchPressValues.remove(telegramId);
    }
}