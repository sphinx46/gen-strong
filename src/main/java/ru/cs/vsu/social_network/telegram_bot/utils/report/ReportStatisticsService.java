package ru.cs.vsu.social_network.telegram_bot.utils.report;

import ru.cs.vsu.social_network.telegram_bot.dto.response.DailyStatsResponse;
import ru.cs.vsu.social_network.telegram_bot.entity.Visit;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Сервис для расчета статистики посещений тренажерного зала.
 * Обеспечивает вычисление метрик, агрегацию данных и формирование статистики.
 */
public interface ReportStatisticsService {

    /**
     * Вычисляет общую статистику по списку посещений.
     * Включает общее количество посещений и количество уникальных посетителей.
     *
     * @param visits список посещений для анализа
     * @return массив [общее количество посещений, количество уникальных посетителей]
     */
    long[] calculateOverallStatistics(List<Visit> visits);

    /**
     * Группирует посещения по датам.
     * Создает карту, где ключ - дата, значение - список посещений за эту дату.
     *
     * @param visits список посещений для группировки
     * @return карта с группированными посещениями
     */
    Map<LocalDate, List<Visit>> groupVisitsByDate(List<Visit> visits);

    /**
     * Рассчитывает среднее количество посещений в день.
     * Основано на количестве дней с посещениями.
     *
     * @param totalVisits общее количество посещений
     * @param daysWithVisits количество дней с посещениями
     * @return среднее количество посещений в день
     */
    double calculateAverageDailyVisits(long totalVisits, int daysWithVisits);

    /**
     * Создает статистику за день на основе списка посещений.
     * Включает дату, количество посетителей и их имена.
     *
     * @param date дата для статистики
     * @param dailyVisits список посещений за день
     * @return статистика за день
     */
    DailyStatsResponse createDailyStats(LocalDate date, List<Visit> dailyVisits);

    /**
     * Генерирует ежедневную статистику за указанный период.
     * Включает данные за каждый день периода, даже если посещений не было.
     *
     * @param startDate начальная дата периода
     * @param endDate конечная дата периода
     * @param visitsByDate карта с группированными посещениями
     * @return список статистики за каждый день периода
     */
    List<DailyStatsResponse> generateDailyStatsForPeriod(LocalDate startDate,
                                                         LocalDate endDate,
                                                         Map<LocalDate, List<Visit>> visitsByDate);


    /**
     * Подсчитывает количество новых пользователей за указанный день.
     * Новым считается пользователь, у которого это первое посещение.
     *
     * @param date дата для подсчета
     * @param dailyVisits список посещений за день
     * @return количество новых пользователей
     */
    int countNewUsersForDate(LocalDate date, List<Visit> dailyVisits);

    /**
     * Подсчитывает общее количество новых пользователей за период.
     *
     * @param startDate начальная дата
     * @param endDate конечная дата
     * @param visitsByDate сгруппированные по датам посещения
     * @return общее количество новых пользователей за период
     */
    int countTotalNewUsersForPeriod(LocalDate startDate, LocalDate endDate,
                                    Map<LocalDate, List<Visit>> visitsByDate);
}