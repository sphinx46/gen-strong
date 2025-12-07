package ru.cs.vsu.social_network.telegram_bot.testUtils;

import ru.cs.vsu.social_network.telegram_bot.dto.request.UserCreateRequest;
import ru.cs.vsu.social_network.telegram_bot.dto.request.UserUpdateRequest;
import ru.cs.vsu.social_network.telegram_bot.dto.response.*;
import ru.cs.vsu.social_network.telegram_bot.entity.Visit;
import ru.cs.vsu.social_network.telegram_bot.entity.User;
import ru.cs.vsu.social_network.telegram_bot.entity.enums.ROLE;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Фабрика тестовых данных для telegram_bot модуля.
 * Содержит методы для создания DTO и сущностей с предсказуемыми значениями.
 * Используется в модульных тестах для обеспечения согласованности тестовых данных.
 */
public final class TestDataFactory {

    private TestDataFactory() {
    }

    /**
     * Создает тестовую сущность пользователя.
     *
     * @param userId уникальный идентификатор пользователя
     * @param username имя пользователя
     * @param displayName отображаемое имя
     * @return новый экземпляр User с указанными параметрами
     */
    public static User createUser(final UUID userId,
                                  final String username,
                                  final String displayName) {
        final User user = new User();
        user.setId(userId);
        user.setUsername(username);
        user.setDisplayName(displayName);
        user.setCreatedAt(LocalDateTime.now());
        return user;
    }

    /**
     * Создает тестовую сущность посещения.
     *
     * @param visitId уникальный идентификатор посещения
     * @param user пользователь
     * @param visitDate дата посещения
     * @return новый экземпляр Visit с указанными параметрами
     */
    public static Visit createVisit(final UUID visitId,
                                    final User user,
                                    final LocalDateTime visitDate) {
        final Visit visit = new Visit();
        visit.setId(visitId);
        visit.setUser(user);
        visit.setVisitDate(visitDate);
        visit.setCreatedAt(LocalDateTime.now());
        return visit;
    }

    /**
     * Создает список тестовых посещений для указанной даты.
     *
     * @param date дата посещений
     * @param count количество посещений
     * @return список Visit для указанной даты
     */
    public static List<Visit> createVisitsForDate(final LocalDate date, final int count) {
        final List<Visit> visits = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            final User user = createUser(
                    UUID.randomUUID(),
                    "user" + i,
                    "User " + i
            );
            final Visit visit = createVisit(
                    UUID.randomUUID(),
                    user,
                    date.atTime(10 + i, 0, 0)
            );
            visits.add(visit);
        }
        return visits;
    }

    /**
     * Создает тестовый ответ со статистикой за день.
     *
     * @param date дата
     * @param visitorCount количество посетителей
     * @param newUsersCount количество новых пользователей
     * @return новый экземпляр DailyStatsResponse с указанными параметрами
     */
    public static DailyStatsResponse createDailyStatsResponse(final LocalDate date,
                                                              final int visitorCount,
                                                              final int newUsersCount) {
        return DailyStatsResponse.builder()
                .date(date)
                .visitorCount(visitorCount)
                .visitorNames(createVisitorNames(visitorCount))
                .newUsersCount(newUsersCount)
                .build();
    }

    /**
     * Создает тестовый ответ журнала посещений.
     *
     * @param logId уникальный идентификатор журнала
     * @param logDate дата журнала
     * @param visitorCount количество посетителей
     * @param newUsersCount количество новых пользователей
     * @return новый экземпляр VisitorLogResponse с указанными параметрами
     */
    public static VisitorLogResponse createVisitorLogResponse(final UUID logId,
                                                              final LocalDate logDate,
                                                              final int visitorCount,
                                                              final Integer newUsersCount) {
        return VisitorLogResponse.builder()
                .id(logId)
                .logDate(logDate)
                .visitorCount(visitorCount)
                .newUsersCount(newUsersCount)
                .rawData(String.join(", ", createVisitorNames(visitorCount)))
                .formattedReport("Тестовый отчет за " + logDate)
                .build();
    }

    /**
     * Создает список имен посетителей.
     *
     * @param count количество имен
     * @return список имен посетителей
     */
    private static List<String> createVisitorNames(final int count) {
        final List<String> names = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            names.add("User " + i);
        }
        return names;
    }

    /**
     * Создает карту статистики по датам.
     *
     * @param startDate начальная дата
     * @param endDate конечная дата
     * @return карта DailyStatsResponse по датам
     */
    public static Map<LocalDate, DailyStatsResponse> createDailyStatsMap(final LocalDate startDate,
                                                                         final LocalDate endDate) {
        final Map<LocalDate, DailyStatsResponse> statsMap = new HashMap<>();
        LocalDate currentDate = startDate;
        int day = 1;

        while (!currentDate.isAfter(endDate)) {
            final int visitorCount = 5 + day % 3;
            final int newUsersCount = day % 2 == 0 ? 1 : 0;

            statsMap.put(currentDate, createDailyStatsResponse(currentDate, visitorCount, newUsersCount));

            currentDate = currentDate.plusDays(1);
            day++;
        }

        return statsMap;
    }

    /**
     * Создает список ответов журналов посещений за период.
     *
     * @param startDate начальная дата
     * @param endDate конечная дата
     * @return список VisitorLogResponse за период
     */
    public static List<VisitorLogResponse> createVisitorLogsForPeriod(final LocalDate startDate,
                                                                      final LocalDate endDate) {
        final List<VisitorLogResponse> logs = new ArrayList<>();
        LocalDate currentDate = startDate;
        int day = 1;

        while (!currentDate.isAfter(endDate)) {
            final int visitorCount = 5 + day % 3;
            final int newUsersCount = day % 2 == 0 ? 1 : 0;

            logs.add(createVisitorLogResponse(
                    UUID.randomUUID(),
                    currentDate,
                    visitorCount,
                    newUsersCount
            ));

            currentDate = currentDate.plusDays(1);
            day++;
        }

        return logs;
    }

    /**
     * Создает тестовый запрос на создание пользователя.
     *
     * @param telegramId идентификатор Telegram
     * @param username имя пользователя
     * @param firstName имя
     * @param lastName фамилия
     * @param displayName отображаемое имя
     * @return новый экземпляр UserCreateRequest
     */
    public static UserCreateRequest createUserCreateRequest(final Long telegramId,
                                                            final String username,
                                                            final String firstName,
                                                            final String lastName,
                                                            final String displayName) {
        return UserCreateRequest.builder()
                .telegramId(telegramId)
                .username(username)
                .firstName(firstName)
                .lastName(lastName)
                .displayName(displayName)
                .build();
    }

    /**
     * Создает тестовый ответ с информацией о пользователе.
     *
     * @param userId уникальный идентификатор пользователя
     * @param telegramId идентификатор Telegram
     * @param username имя пользователя
     * @param displayName отображаемое имя
     * @param role роль пользователя
     * @return новый экземпляр UserInfoResponse
     */
    public static UserInfoResponse createUserInfoResponse(final UUID userId,
                                                          final Long telegramId,
                                                          final String username,
                                                          final String displayName,
                                                          final ROLE role) {
        return UserInfoResponse.builder()
                .id(userId)
                .telegramId(telegramId)
                .username(username)
                .firstName("Test")
                .lastName("User")
                .displayName(displayName)
                .role(role)
                .build();
    }

    /**
     * Создает тестовый запрос на обновление пользователя.
     *
     * @param displayName новое отображаемое имя
     * @param role новая роль
     * @return новый экземпляр UserUpdateRequest
     */
    public static UserUpdateRequest createUserUpdateRequest(final String displayName,
                                                            final ROLE role) {
        return UserUpdateRequest.builder()
                .displayName(displayName)
                .role(role)
                .build();
    }

    /**
     * Создает тестовый ответ с информацией о посещении.
     *
     * @param visitId уникальный идентификатор посещения
     * @param userId идентификатор пользователя
     * @param visitDate дата посещения
     * @return новый экземпляр VisitResponse
     */
    public static VisitResponse createVisitResponse(final UUID visitId,
                                                    final UUID userId,
                                                    final LocalDate visitDate) {
        return VisitResponse.builder()
                .id(visitId)
                .userId(userId)
                .visitDate(LocalDate.from(visitDate.atStartOfDay()))
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * Создает тестовый ответ с отчетом за период.
     *
     * @param startDate начальная дата
     * @param endDate конечная дата
     * @param totalVisits общее количество посещений
     * @param uniqueVisitors количество уникальных посетителей
     * @param totalNewUsers количество новых пользователей
     * @return новый экземпляр ReportResponse
     */
    public static ReportResponse createReportResponse(final LocalDate startDate,
                                                      final LocalDate endDate,
                                                      final int totalVisits,
                                                      final int uniqueVisitors,
                                                      final int totalNewUsers) {
        return ReportResponse.builder()
                .startDate(startDate)
                .endDate(endDate)
                .totalVisits(totalVisits)
                .uniqueVisitors(uniqueVisitors)
                .totalNewUsers(totalNewUsers)
                .averageDailyVisits(10.5)
                .dailyStats(createDailyStatsMap(startDate, endDate))
                .telegramFormattedReport("Тестовый отчет за период")
                .build();
    }

    /**
     * Создает список UUID для тестирования.
     *
     * @param count количество UUID
     * @return список UUID
     */
    public static List<UUID> createUuids(final int count) {
        final List<UUID> uuids = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            uuids.add(UUID.randomUUID());
        }
        return uuids;
    }
}