package ru.cs.vsu.social_network.telegram_bot.service.command;

import java.util.Map;

/**
 * Интерфейс для обработки команд Telegram бота.
 */
public interface TelegramCommand {

    /**
     * Выполняет команду.
     *
     * @param telegramId идентификатор пользователя в Telegram
     * @param input входные параметры команды
     * @return результат выполнения команды
     */
    String execute(Long telegramId, String input);

    /**
     * Устанавливает карту состояний пользователей.
     *
     * @param userStates карта состояний пользователей
     */
    void setUserStates(Map<Long, String> userStates);

    /**
     * Устанавливает карту состояний администраторов.
     *
     * @param adminStates карта состояний администраторов
     */
    void setAdminStates(Map<Long, String> adminStates);

    /**
     * Устанавливает карту значений жима лежа.
     *
     * @param pendingBenchPressValues карта значений жима лежа
     */
    void setPendingBenchPressValues(Map<Long, Double> pendingBenchPressValues);

    /**
     * Устанавливает карту тренировочных циклов.
     *
     * @param pendingTrainingCycles карта тренировочных циклов
     */
    void setPendingTrainingCycles(Map<Long, String> pendingTrainingCycles);

    /**
     * Устанавливает карту выбранных форматов.
     *
     * @param pendingFormatSelections карта выбранных форматов
     */
    void setPendingFormatSelections(Map<Long, String> pendingFormatSelections);
}