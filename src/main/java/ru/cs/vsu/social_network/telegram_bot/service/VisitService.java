package ru.cs.vsu.social_network.telegram_bot.service;

import ru.cs.vsu.social_network.telegram_bot.dto.response.VisitResponse;
import ru.cs.vsu.social_network.telegram_bot.dto.response.pageable.PageResponse;
import ru.cs.vsu.social_network.telegram_bot.dto.request.pageable.PageRequest;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Сервис для управления посещениями тренажерного зала.
 * Обеспечивает обработку посещений пользователей, проверку повторных посещений и получение статистики.
 */
public interface VisitService {

    /**
     * Создает запись о посещении тренажерного зала для указанного пользователя.
     * Проверяет, не отмечался ли пользователь уже сегодня.
     *
     * @param userId идентификатор пользователя
     * @return DTO созданного посещения
     */
    VisitResponse createVisit(UUID userId);

    /**
     * Создает запись о посещении для пользователя по его Telegram ID.
     * Используется при обработке команды "Я в зале" из Telegram.
     *
     * @param telegramId идентификатор пользователя в Telegram
     * @return DTO созданного посещения
     */
    VisitResponse createVisitByTelegramId(Long telegramId);

    /**
     * Получает посещение по его идентификатору.
     * Включает информацию о пользователе и времени посещения.
     *
     * @param visitId идентификатор посещения
     * @return DTO найденного посещения
     */
    VisitResponse getVisitById(UUID visitId);

    /**
     * Получает страницу посещений за указанную дату.
     * Используется для просмотра всех посетителей за день.
     *
     * @param date дата посещений
     * @param pageRequest параметры пагинации и сортировки
     * @return страница с посещениями
     */
    PageResponse<VisitResponse> getVisitsByDate(LocalDate date, PageRequest pageRequest);

    /**
     * Получает страницу посещений указанного пользователя.
     * Используется для просмотра истории посещений пользователя.
     *
     * @param userId идентификатор пользователя
     * @param pageRequest параметры пагинации и сортировки
     * @return страница с посещениями пользователя
     */
    PageResponse<VisitResponse> getVisitsByUser(UUID userId, PageRequest pageRequest);

    /**
     * Проверяет, посещал ли пользователь зал сегодня.
     * Используется для предотвращения повторных отметок.
     *
     * @param userId идентификатор пользователя
     * @return true если пользователь уже отмечался сегодня, иначе false
     */
    boolean hasUserVisitedToday(UUID userId);

    /**
     * Получает количество посещений за указанную дату.
     * Используется для статистики и отчетов.
     *
     * @param date дата для подсчета
     * @return количество посещений за день
     */
    long getVisitCountByDate(LocalDate date);

    /**
     * Получает количество посещений указанного пользователя.
     * Используется для статистики пользователя.
     *
     * @param userId идентификатор пользователя
     * @return общее количество посещений пользователя
     */
    long getVisitCountByUser(UUID userId);
}