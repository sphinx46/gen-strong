package ru.cs.vsu.social_network.telegram_bot.utils.report.reportImpl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.cs.vsu.social_network.telegram_bot.dto.response.DailyStatsResponse;
import ru.cs.vsu.social_network.telegram_bot.entity.Visit;
import ru.cs.vsu.social_network.telegram_bot.utils.report.ReportStatisticsService;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Реализация сервиса для расчета статистики посещений.
 * Обеспечивает вычисление метрик, агрегацию данных и формирование статистики.
 */
@Slf4j
@Service
public class ReportStatisticsServiceImpl implements ReportStatisticsService {

    private static final String SERVICE_NAME = "СТАТИСТИКА_СЕРВИС";

    /**
     * {@inheritDoc}
     */
    @Override
    public long[] calculateOverallStatistics(List<Visit> visits) {
        log.debug("{}_ВЫЧИСЛЕНИЕ_ОБЩЕЙ_СТАТИСТИКИ_НАЧАЛО: обработка {} посещений",
                SERVICE_NAME, visits.size());

        long totalVisits = visits.size();
        long uniqueVisitors = visits.stream()
                .map(visit -> visit.getUser().getId())
                .distinct()
                .count();

        log.debug("{}_ВЫЧИСЛЕНИЕ_ОБЩЕЙ_СТАТИСТИКИ_УСПЕХ: всего посещений: {}, уникальных посетителей: {}",
                SERVICE_NAME, totalVisits, uniqueVisitors);

        return new long[]{totalVisits, uniqueVisitors};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<LocalDate, List<Visit>> groupVisitsByDate(List<Visit> visits) {
        log.debug("{}_ГРУППИРОВКА_ПОСЕЩЕНИЙ_ПО_ДАТАМ_НАЧАЛО: обработка {} посещений",
                SERVICE_NAME, visits.size());

        Map<LocalDate, List<Visit>> visitsByDate = visits.stream()
                .collect(Collectors.groupingBy(
                        visit -> visit.getVisitDate().toLocalDate(),
                        TreeMap::new,
                        Collectors.toList()
                ));

        log.debug("{}_ГРУППИРОВКА_ПОСЕЩЕНИЙ_ПО_ДАТАМ_УСПЕХ: сгруппировано по {} датам",
                SERVICE_NAME, visitsByDate.size());

        return visitsByDate;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double calculateAverageDailyVisits(long totalVisits, int daysWithVisits) {
        log.debug("{}_ВЫЧИСЛЕНИЕ_СРЕДНЕГО_ПОСЕЩЕНИЙ_НАЧАЛО: всего посещений: {}, дней с посещениями: {}",
                SERVICE_NAME, totalVisits, daysWithVisits);

        if (daysWithVisits == 0) {
            log.debug("{}_ВЫЧИСЛЕНИЕ_СРЕДНЕГО_ПОСЕЩЕНИЙ: дней с посещениями нет, возвращаем 0",
                    SERVICE_NAME);
            return 0.0;
        }

        double average = (double) totalVisits / daysWithVisits;
        log.debug("{}_ВЫЧИСЛЕНИЕ_СРЕДНЕГО_ПОСЕЩЕНИЙ_УСПЕХ: среднее посещений в день: {}",
                SERVICE_NAME, String.format("%.2f", average));

        return average;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<DailyStatsResponse> generateDailyStatsForPeriod(LocalDate startDate,
                                                                LocalDate endDate,
                                                                Map<LocalDate, List<Visit>> visitsByDate) {
        log.info("{}_ГЕНЕРАЦИЯ_СТАТИСТИКИ_ЗА_ПЕРИОД_НАЧАЛО: период {} - {}",
                SERVICE_NAME, startDate, endDate);

        List<DailyStatsResponse> stats = new ArrayList<>();
        LocalDate currentDate = startDate;

        while (!currentDate.isAfter(endDate)) {
            List<Visit> dailyVisits = visitsByDate.getOrDefault(currentDate, Collections.emptyList());
            DailyStatsResponse dailyStat = createDailyStats(currentDate, dailyVisits);
            stats.add(dailyStat);
            currentDate = currentDate.plusDays(1);
        }

        log.info("{}_ГЕНЕРАЦИЯ_СТАТИСТИКИ_ЗА_ПЕРИОД_УСПЕХ: сгенерировано {} записей статистики",
                SERVICE_NAME, stats.size());

        return stats;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int countNewUsersForDate(LocalDate date, List<Visit> dailyVisits) {
        log.debug("{}_ПОДСЧЕТ_НОВЫХ_ПОЛЬЗОВАТЕЛЕЙ_ЗА_ДЕНЬ_НАЧАЛО: дата: {}, посещений: {}",
                SERVICE_NAME, date, dailyVisits.size());

        if (dailyVisits.isEmpty()) {
            log.debug("{}_ПОДСЧЕТ_НОВЫХ_ПОЛЬЗОВАТЕЛЕЙ_ЗА_ДЕНЬ_УСПЕХ: нет посещений, новых пользователей: 0",
                    SERVICE_NAME);
            return 0;
        }
        Set<UUID> uniqueUsers = new HashSet<>();

        for (Visit visit : dailyVisits) {
            UUID userId = visit.getUser().getId();
            if (!uniqueUsers.contains(userId)) {
                uniqueUsers.add(userId);
            }
        }

        log.debug("{}_ПОДСЧЕТ_НОВЫХ_ПОЛЬЗОВАТЕЛЕЙ_ЗА_ДЕНЬ_УСПЕХ: найдено {} уникальных пользователей",
                SERVICE_NAME, uniqueUsers.size());

        return uniqueUsers.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int countTotalNewUsersForPeriod(LocalDate startDate, LocalDate endDate,
                                           Map<LocalDate, List<Visit>> visitsByDate) {
        log.info("{}_ПОДСЧЕТ_ОБЩЕГО_КОЛИЧЕСТВА_НОВЫХ_ПОЛЬЗОВАТЕЛЕЙ_ЗА_ПЕРИОД_НАЧАЛО: период {} - {}",
                SERVICE_NAME, startDate, endDate);

        Set<UUID> uniqueNewUsers = new HashSet<>();

        for (Map.Entry<LocalDate, List<Visit>> entry : visitsByDate.entrySet()) {
            LocalDate currentDate = entry.getKey();
            List<Visit> dailyVisits = entry.getValue();

            for (Visit visit : dailyVisits) {
                UUID userId = visit.getUser().getId();
                uniqueNewUsers.add(userId);
            }
        }

        int totalNewUsers = uniqueNewUsers.size();

        log.info("{}_ПОДСЧЕТ_ОБЩЕГО_КОЛИЧЕСТВА_НОВЫХ_ПОЛЬЗОВАТЕЛЕЙ_ЗА_ПЕРИОД_УСПЕХ: " +
                "найдено {} новых пользователей за период", SERVICE_NAME, totalNewUsers);

        return totalNewUsers;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DailyStatsResponse createDailyStats(LocalDate date, List<Visit> dailyVisits) {
        log.debug("{}_СОЗДАНИЕ_СТАТИСТИКИ_ЗА_ДЕНЬ_НАЧАЛО: дата: {}, посещений: {}",
                SERVICE_NAME, date, dailyVisits.size());

        List<String> visitorNames = dailyVisits.stream()
                .map(visit -> {
                    try {
                        return visit.getUser() != null ? visit.getUser().getDisplayName() : null;
                    } catch (Exception e) {
                        log.warn("{}_СОЗДАНИЕ_СТАТИСТИКИ_ОШИБКА_ПОЛУЧЕНИЯ_ИМЕНИ: " +
                                        "ошибка при получении имени пользователя",
                                SERVICE_NAME);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        int newUsersCount = countNewUsersForDate(date, dailyVisits);

        DailyStatsResponse dailyStat = DailyStatsResponse.builder()
                .date(date)
                .visitorCount(visitorNames.size())
                .visitorNames(visitorNames)
                .newUsersCount(newUsersCount)
                .build();

        log.debug("{}_СОЗДАНИЕ_СТАТИСТИКИ_ЗА_ДЕНЬ_УСПЕХ: " +
                        "создана статистика за {}, посетителей: {}, новых пользователей: {}",
                SERVICE_NAME, date, visitorNames.size(), newUsersCount);

        return dailyStat;
    }
}