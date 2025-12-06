package ru.cs.vsu.social_network.telegram_bot.provider;

import ru.cs.vsu.social_network.telegram_bot.entity.User;
import ru.cs.vsu.social_network.telegram_bot.entity.Visit;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Специализированный провайдер для доступа к сущностям Visit.
 * Обеспечивает получение посещений с обработкой ошибок.
 */
public interface VisitEntityProvider extends EntityProvider<Visit> {

    /**
     * Проверяет, посещал ли пользователь зал в указанный день.
     *
     * @param userId идентификатор пользователя
     * @param date дата для проверки
     * @return true если пользователь посещал зал в этот день, false в противном случае
     */
    boolean existsByUserIdAndDate(UUID userId, LocalDate date);

    /**
     * Находит всех пользователей, посетивших зал в указанный день.
     *
     * @param date дата посещений
     * @return список посещений с загруженными пользователями
     */
    List<Visit> findAllWithUsersByDate(LocalDate date);

    /**
     * Находит количество посещений за указанную дату.
     *
     * @param date дата для подсчета
     * @return количество посещений
     */
    long countByDate(LocalDate date);

    /**
     * Находит все посещения за диапазон дат.
     *
     * @param startDate начальная дата (включительно)
     * @param endDate конечная дата (включительно)
     * @return список посещений
     */
    List<Visit> findAllByDateRange(LocalDate startDate, LocalDate endDate);

    /**
     * Находит посещение пользователя на конкретную дату.
     *
     * @param user пользователь
     * @param start начало дня
     * @param end конец дня
     * @return Optional с посещением, если найдено
     */
    Optional<Visit> findByUserAndVisitDateBetween(User user,
                                                  LocalDateTime start,
                                                  LocalDateTime end);

    /**
     * Подсчитывает количество новых пользователей за указанную дату.
     * Новым считается пользователь, у которого это первое посещение в системе.
     *
     * @param date дата для подсчета
     * @return количество новых пользователей за указанный день
     */
    int countNewUsersByDate(LocalDate date);

    /**
     * Подсчитывает количество новых пользователей за диапазон дат.
     * Новым считается пользователь, у которого первое посещение попадает в этот период.
     *
     * @param startDate начальная дата (включительно)
     * @param endDate конечная дата (включительно)
     * @return количество новых пользователей за указанный период
     */
    int countNewUsersByDateRange(LocalDate startDate, LocalDate endDate);


    /**
     * Находит всех новых пользователей за указанную дату.
     *
     * @param date дата для поиска
     * @return список посещений новых пользователей
     */
    List<Visit> findNewUsersByDate(LocalDate date);
}