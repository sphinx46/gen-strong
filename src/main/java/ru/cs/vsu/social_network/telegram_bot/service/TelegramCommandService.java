package ru.cs.vsu.social_network.telegram_bot.service;

/**
 * Сервис обработки команд Telegram бота
 */
public interface TelegramCommandService {

    /**
     * Обрабатывает команду /start
     *
     * @param telegramId идентификатор пользователя в Telegram
     * @param username имя пользователя в Telegram
     * @param firstName имя пользователя
     * @param lastName фамилия пользователя
     * @return результат обработки команды
     */
    String handleStartCommand(Long telegramId, String username, String firstName, String lastName);

    /**
     * Обрабатывает команду /help
     *
     * @param telegramId идентификатор пользователя в Telegram
     * @return результат обработки команды
     */
    String handleHelpCommand(Long telegramId);

    /**
     * Обрабатывает неизвестную команду
     *
     * @param telegramId идентификатор пользователя в Telegram
     * @return результат обработки команды
     */
    String handleUnknownCommand(Long telegramId);

    /**
     * Обрабатывает команду "Я в зале"
     *
     * @param telegramId идентификатор пользователя в Telegram
     * @return результат обработки команды
     */
    String handleInGymCommand(Long telegramId);

    /**
     * Обрабатывает команду "Сменить имя"
     *
     * @param telegramId идентификатор пользователя в Telegram
     * @return результат обработки команды
     */
    String handleChangeNameCommand(Long telegramId);

    /**
     * Обрабатывает команду "Составить программу тренировок"
     *
     * @param telegramId идентификатор пользователя в Telegram
     * @param input входные данные (номер цикла или значение жима лежа)
     * @return результат обработки команды
     */
    String handleTrainingProgramCommand(Long telegramId, String input);

    /**
     * Обрабатывает ввод отображаемого имени
     *
     * @param telegramId идентификатор пользователя в Telegram
     * @param displayName введенное отображаемое имя
     * @return результат обработки ввода
     */
    String handleDisplayNameInput(Long telegramId, String displayName);

    /**
     * Обрабатывает команду администраторского меню
     *
     * @param telegramId идентификатор пользователя в Telegram
     * @param commandText текст команды
     * @return результат обработки команды
     */
    String handleAdminMenuCommand(Long telegramId, String commandText);

    /**
     * Обрабатывает ввод даты администратором
     *
     * @param telegramId идентификатор пользователя в Telegram
     * @param dateInput введенная дата
     * @return результат обработки ввода
     */
    String handleAdminDateInput(Long telegramId, String dateInput);

    /**
     * Обрабатывает команду дневного отчета администратора
     *
     * @param telegramId идентификатор пользователя в Telegram
     * @param dateStr строка с датой
     * @return результат обработки команды
     */
    String handleDailyReportCommand(Long telegramId, String dateStr);

    /**
     * Обрабатывает команду отчета за период администратора
     *
     * @param telegramId идентификатор пользователя в Telegram
     * @param startDateStr строка с начальной датой
     * @param endDateStr строка с конечной датой
     * @return результат обработки команды
     */
    String handlePeriodReportCommand(Long telegramId, String startDateStr, String endDateStr);

    /**
     * Обрабатывает команду вывода таблицы
     *
     * @param telegramId идентификатор пользователя в Telegram
     * @param input входные данные
     * @return результат обработки команды
     */
    String handleTableCommand(Long telegramId, String input);

    /**
     * Обрабатывает команду "Внести вклад в развитие"
     *
     * @param telegramId идентификатор пользователя в Telegram
     * @return результат обработки команды
     */
    String handleContributionCommand(Long telegramId);

    /**
     * Обрабатывает команду сбора метрик
     *
     * @param telegramId идентификатор пользователя в Telegram
     * @param input введенные данные (вес, цель и т.д.) или null для начала сбора
     * @return результат обработки команды
     */
    String handleMetricsCommand(Long telegramId, String input);

    /**
     * Получает текущее состояние пользователя
     *
     * @param telegramId идентификатор пользователя
     * @return состояние пользователя или null если состояние не установлено
     */
    String getUserState(Long telegramId);
}