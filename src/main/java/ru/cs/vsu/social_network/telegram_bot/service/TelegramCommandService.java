package ru.cs.vsu.social_network.telegram_bot.service;

/**
 * Сервис для обработки команд Telegram бота тренажерного зала.
 * Управляет входящими командами, состояниями пользователей и формированием ответов.
 */
public interface TelegramCommandService {

    /**
     * Обрабатывает команду /start от пользователя.
     * Регистрирует пользователя, запрашивает имя для обращения.
     *
     * @param telegramId идентификатор пользователя в Telegram
     * @param username никнейм пользователя
     * @param firstName имя пользователя
     * @param lastName фамилия пользователя
     * @return ответное сообщение
     */
    String handleStartCommand(Long telegramId, String username,
                              String firstName, String lastName);

    /**
     * Обрабатывает команду "Я в зале" от пользователя.
     * Создает запись о посещении и формирует журнал за текущий день.
     *
     * @param telegramId идентификатор пользователя в Telegram
     * @return ответное сообщение с подтверждением и журналом
     */
    String handleInGymCommand(Long telegramId);

    /**
     * Обрабатывает ввод отображаемого имени пользователя.
     * Сохраняет имя, которое будет использоваться для обращения к пользователю.
     *
     * @param telegramId идентификатор пользователя в Telegram
     * @param displayName отображаемое имя
     * @return подтверждающее сообщение
     */
    String handleDisplayNameInput(Long telegramId, String displayName);

    /**
     * Обрабатывает команду администратора для получения отчета за день.
     * Формирует журнал посещений за текущий день или указанную дату.
     *
     * @param telegramId идентификатор администратора в Telegram
     * @param dateStr строка с датой (опционально)
     * @return форматированный отчет за день
     */
    String handleDailyReportCommand(Long telegramId, String dateStr);

    /**
     * Обрабатывает команду администратора для получения отчета за период.
     * Формирует статистику посещений за указанный период.
     *
     * @param telegramId идентификатор администратора в Telegram
     * @param startDateStr строка с начальной датой
     * @param endDateStr строка с конечной датой
     * @return форматированный отчет за период
     */
    String handlePeriodReportCommand(Long telegramId, String startDateStr, String endDateStr);

    /**
     * Обрабатывает неизвестную команду или сообщение.
     * Предоставляет пользователю информацию о доступных командах.
     *
     * @param telegramId идентификатор пользователя в Telegram
     * @return сообщение со списком доступных команд
     */
    String handleUnknownCommand(Long telegramId);
}