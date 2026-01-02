package ru.cs.vsu.social_network.telegram_bot.utils;

public class MessageConstants {

    // VISITOR LOG
    public final static String VISITOR_LOG_NOT_FOUND_FAILURE = "Ошибка! Журнал посещений не найден.";

    // VISIT
    public final static String VISIT_NOT_FOUND_FAILURE = "Ошибка! Посещение не найдено";
    public final static String VISIT_ALREADY_FAILURE = "Ошибка! Вы уже отметились сегодня";

    // USER
    public final static String USER_NOT_FOUND_FAILURE = "Ошибка! Пользователь не найден";
    public final static String USER_NOT_FOUND_BY_TELEGRAM_ID_FAILURE = "Ошибка! Пользователь с указанными telegram id не найден";

    // SERVER
    public final static String ACCESS_DENIED = "Ошибка! Доступ запрещён";

    // ROLE
    public final static String ADMIN_ACCESS_REQUIRED = "Ошибка! Пользователь не является администратором";
    public final static String ROLE_ACCESS_REQUIRED = "Ошибка! Пользователь не имеет требуемую роль";

    // TRAINING PLAN
    public final static String GENERATE_PLAN_FAILURE = "Ошибка! Не удалось сгенерировать тренировочный план";
    public static final String USER_TRAINING_NOT_FOUND_FAILURE = "Ошибка! Запись тренировок пользователя не найдена";
}
