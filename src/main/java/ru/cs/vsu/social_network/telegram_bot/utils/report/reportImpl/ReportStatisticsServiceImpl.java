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
    public DailyStatsResponse createDailyStats(LocalDate date, List<Visit> dailyVisits) {
        log.debug("{}_СОЗДАНИЕ_СТАТИСТИКИ_ЗА_ДЕНЬ_НАЧАЛО: дата: {}, посещений: {}",
                SERVICE_NAME, date, dailyVisits.size());

        List<String> visitorNames = dailyVisits.stream()
                .map(visit -> visit.getUser().getDisplayName())
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        DailyStatsResponse dailyStat = DailyStatsResponse.builder()
                .date(date)
                .visitorCount(visitorNames.size())
                .visitorNames(visitorNames)
                .build();

        log.debug("{}_СОЗДАНИЕ_СТАТИСТИКИ_ЗА_ДЕНЬ_УСПЕХ: создана статистика за {}, посетителей: {}",
                SERVICE_NAME, date, visitorNames.size());

        return dailyStat;
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
}