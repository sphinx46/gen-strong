package ru.cs.vsu.social_network.telegram_bot.service.serviceImpl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.cs.vsu.social_network.telegram_bot.dto.response.DailyStatsResponse;
import ru.cs.vsu.social_network.telegram_bot.dto.response.ReportResponse;
import ru.cs.vsu.social_network.telegram_bot.dto.response.VisitorLogResponse;
import ru.cs.vsu.social_network.telegram_bot.entity.Visit;
import ru.cs.vsu.social_network.telegram_bot.entity.VisitorLog;
import ru.cs.vsu.social_network.telegram_bot.mapping.EntityMapper;
import ru.cs.vsu.social_network.telegram_bot.provider.VisitEntityProvider;
import ru.cs.vsu.social_network.telegram_bot.provider.VisitorLogEntityProvider;
import ru.cs.vsu.social_network.telegram_bot.repository.VisitorLogRepository;
import ru.cs.vsu.social_network.telegram_bot.service.ReportService;
import ru.cs.vsu.social_network.telegram_bot.utils.factory.VisitorLogFactory;
import ru.cs.vsu.social_network.telegram_bot.utils.report.ReportFormatterService;
import ru.cs.vsu.social_network.telegram_bot.utils.report.ReportStatisticsService;
import ru.cs.vsu.social_network.telegram_bot.validation.VisitorLogValidator;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Реализация сервиса для генерации отчетов и статистики посещений.
 * Координирует работу специализированных сервисов для формирования отчетов.
 */
@Slf4j
@Service
public class ReportServiceImpl implements ReportService {

    private static final String SERVICE_NAME = "ОТЧЕТ_СЕРВИС";

    private final VisitEntityProvider visitEntityProvider;
    private final VisitorLogEntityProvider visitorLogEntityProvider;
    private final VisitorLogRepository visitorLogRepository;
    private final VisitorLogFactory visitorLogFactory;
    private final VisitorLogValidator visitorLogValidator;
    private final EntityMapper entityMapper;
    private final ReportStatisticsService reportStatisticsService;
    private final ReportFormatterService reportFormatterService;

    public ReportServiceImpl(final VisitEntityProvider visitEntityProvider,
                             final VisitorLogEntityProvider visitorLogEntityProvider,
                             final VisitorLogRepository visitorLogRepository,
                             final VisitorLogFactory visitorLogFactory,
                             final VisitorLogValidator visitorLogValidator,
                             final EntityMapper entityMapper,
                             final ReportStatisticsService reportStatisticsService,
                             final ReportFormatterService reportFormatterService) {
        this.visitEntityProvider = visitEntityProvider;
        this.visitorLogEntityProvider = visitorLogEntityProvider;
        this.visitorLogRepository = visitorLogRepository;
        this.visitorLogFactory = visitorLogFactory;
        this.visitorLogValidator = visitorLogValidator;
        this.entityMapper = entityMapper;
        this.reportStatisticsService = reportStatisticsService;
        this.reportFormatterService = reportFormatterService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public VisitorLogResponse generateDailyReport(final UUID adminUserId) {
        log.info("{}_ГЕНЕРАЦИЯ_ЕЖЕДНЕВНОГО_ОТЧЕТА_НАЧАЛО: администратор {}",
                SERVICE_NAME, adminUserId);

        visitorLogValidator.validateAdminAccessForLogs(adminUserId);

        final LocalDate today = LocalDate.now();
        return generateAndSaveVisitorLog(today);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public VisitorLogResponse generateDailyReportForDate(final UUID adminUserId, final LocalDate date) {
        log.info("{}_ГЕНЕРАЦИЯ_ОТЧЕТА_ПО_ДАТЕ_НАЧАЛО: " +
                "администратор {}, дата: {}", SERVICE_NAME, adminUserId, date);

        visitorLogValidator.validateAdminAccessForLogs(adminUserId);

        return generateAndSaveVisitorLog(date);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ReportResponse generatePeriodReport(final UUID adminUserId,
                                               final LocalDate startDate,
                                               final LocalDate endDate) {
        log.info("{}_ГЕНЕРАЦИЯ_ОТЧЕТА_ЗА_ПЕРИОД_НАЧАЛО: " +
                        "администратор {}, период: {} - {}",
                SERVICE_NAME, adminUserId, startDate, endDate);

        visitorLogValidator.validateAdminAccessForLogs(adminUserId);

        final List<Visit> visits = visitEntityProvider.findAllByDateRange(startDate, endDate);

        final long[] overallStats = reportStatisticsService.calculateOverallStatistics(visits);
        final long totalVisits = overallStats[0];
        final long uniqueVisitors = overallStats[1];

        final Map<LocalDate, List<Visit>> visitsByDate = reportStatisticsService.groupVisitsByDate(visits);

        final Map<LocalDate, DailyStatsResponse> dailyStats = new TreeMap<>();
        for (final Map.Entry<LocalDate, List<Visit>> entry : visitsByDate.entrySet()) {
            final LocalDate date = entry.getKey();
            final List<Visit> dailyVisits = entry.getValue();

            final DailyStatsResponse dailyStat = reportStatisticsService.createDailyStats(date, dailyVisits);
            dailyStats.put(date, dailyStat);
        }

        final double averageDailyVisits = reportStatisticsService.calculateAverageDailyVisits(
                totalVisits, dailyStats.size());

        final String telegramReport = reportFormatterService.formatPeriodTelegramReport(
                startDate, endDate, dailyStats,
                totalVisits, uniqueVisitors, averageDailyVisits);

        final ReportResponse report = entityMapper.map(
                ReportResponse.builder()
                        .startDate(startDate)
                        .endDate(endDate)
                        .totalVisits((int) totalVisits)
                        .uniqueVisitors((int) uniqueVisitors)
                        .averageDailyVisits(averageDailyVisits)
                        .dailyStats(dailyStats)
                        .telegramFormattedReport(telegramReport)
                        .build(),
                ReportResponse.class
        );

        log.info("{}_ГЕНЕРАЦИЯ_ОТЧЕТА_ЗА_ПЕРИОД_УСПЕХ: " +
                        "отчет за период {} - {} сгенерирован, всего посещений: {}",
                SERVICE_NAME, startDate, endDate, totalVisits);

        return report;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<DailyStatsResponse> generateDailyStats(final UUID adminUserId,
                                                       final LocalDate startDate,
                                                       final LocalDate endDate) {
        log.info("{}_ГЕНЕРАЦИЯ_ЕЖЕДНЕВНОЙ_СТАТИСТИКИ_НАЧАЛО: " +
                        "администратор {}, период: {} - {}",
                SERVICE_NAME, adminUserId, startDate, endDate);

        visitorLogValidator.validateAdminAccessForLogs(adminUserId);

        final List<Visit> visits = visitEntityProvider.findAllByDateRange(startDate, endDate);
        final Map<LocalDate, List<Visit>> visitsByDate = reportStatisticsService.groupVisitsByDate(visits);

        final List<DailyStatsResponse> stats = reportStatisticsService.generateDailyStatsForPeriod(
                startDate, endDate, visitsByDate);

        log.info("{}_ГЕНЕРАЦИЯ_ЕЖЕДНЕВНОЙ_СТАТИСТИКИ_УСПЕХ: " +
                "сгенерировано {} записей статистики", SERVICE_NAME, stats.size());

        return stats;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String formatTelegramReport(final LocalDate date, final List<String> visitorNames) {
        log.debug("{}_ФОРМАТИРОВАНИЕ_ОТЧЕТА_ДЛЯ_TELEGRAM_НАЧАЛО: " +
                "дата: {}, посетителей: {}", SERVICE_NAME, date, visitorNames.size());

        final String result = reportFormatterService.formatDailyTelegramReport(date, visitorNames);

        log.debug("{}_ФОРМАТИРОВАНИЕ_ОТЧЕТА_ДЛЯ_TELEGRAM_УСПЕХ: " +
                "отчет сформирован, длина: {}", SERVICE_NAME, result.length());

        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<VisitorLogResponse> getRecentVisitorLogs(final UUID adminUserId, final int days) {
        log.info("{}_ПОЛУЧЕНИЕ_ПОСЛЕДНИХ_ЖУРНАЛОВ_НАЧАЛО: " +
                "администратор {}, дней: {}", SERVICE_NAME, adminUserId, days);

        visitorLogValidator.validateAdminAccessForLogs(adminUserId);

        final List<VisitorLog> logs = visitorLogEntityProvider.findLatest(days);
        final List<VisitorLogResponse> responses = entityMapper.mapList(logs, VisitorLogResponse.class);

        log.info("{}_ПОЛУЧЕНИЕ_ПОСЛЕДНИХ_ЖУРНАЛОВ_УСПЕХ: " +
                "получено {} журналов", SERVICE_NAME, responses.size());

        return responses;
    }

    /**
     * Генерирует и сохраняет журнал посещений для указанной даты.
     *
     * @param date дата для генерации журнала
     * @return DTO сохраненного журнала посещений
     */
    private VisitorLogResponse generateAndSaveVisitorLog(final LocalDate date) {
        final List<Visit> visits = visitEntityProvider.findAllWithUsersByDate(date);

        final List<String> visitorNames = visits.stream()
                .map(visit -> visit.getUser().getDisplayName())
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        final String rawData = String.join(", ", visitorNames);

        VisitorLog visitorLog = visitorLogEntityProvider.findByLogDate(date)
                .orElseGet(() -> visitorLogFactory.createWithData(
                        visitorNames.size(), rawData, date));

        visitorLog.setVisitorCount(visitorNames.size());
        visitorLog.setRawData(rawData);
        visitorLog.setLogDate(date);

        final VisitorLog savedLog = visitorLogRepository.save(visitorLog);
        final VisitorLogResponse response = entityMapper.map(savedLog, VisitorLogResponse.class);

        response.setFormattedReport(formatTelegramReport(date, visitorNames));

        log.info("{}_СОХРАНЕНИЕ_ЖУРНАЛА_УСПЕХ: " +
                        "журнал за дату {} сохранен, посетителей: {}",
                SERVICE_NAME, date, visitorNames.size());

        return response;
    }
}