package ru.cs.vsu.social_network.telegram_bot.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.cs.vsu.social_network.telegram_bot.entity.VisitorLog;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Репозиторий для работы с журналом посетителей.
 * Обеспечивает операции с агрегированными данными о посещениях за день.
 */
@Repository
public interface VisitorLogRepository extends JpaRepository<VisitorLog, UUID> {

    /**
     * Находит запись журнала по дате.
     *
     * @param logDate дата журнала
     * @return Optional с записью журнала, если найдена
     */
    Optional<VisitorLog> findByLogDate(LocalDate logDate);

    /**
     * Находит все записи журнала в указанном диапазоне дат.
     * Использует пагинацию для оптимизации работы с большими объемами данных.
     *
     * @param startDate начальная дата диапазона (включительно)
     * @param endDate конечная дата диапазона (включительно)
     * @param pageable параметры пагинации
     * @return страница с записями журнала
     */
    Page<VisitorLog> findAllByLogDateBetween(LocalDate startDate, LocalDate endDate, Pageable pageable);

    /**
     * Находит все записи журнала за указанный месяц.
     *
     * @param year год
     * @param month месяц (1-12)
     * @return список записей журнала за месяц
     */
    @Query("SELECT vl FROM VisitorLog vl WHERE YEAR(vl.logDate) = :year AND MONTH(vl.logDate) = :month ORDER BY vl.logDate")
    List<VisitorLog> findAllByMonth(@Param("year") int year, @Param("month") int month);

    /**
     * Проверяет существование записи журнала на указанную дату.
     *
     * @param logDate дата для проверки
     * @return true если запись существует, false в противном случае
     */
    boolean existsByLogDate(LocalDate logDate);

    /**
     * Удаляет записи журнала старше указанной даты.
     * Выполняет batch-удаление для оптимизации работы с большими объемами данных.
     *
     * @param cutoffDate дата, старше которой записи будут удалены
     * @return количество удаленных записей
     */
    @Modifying
    @Query("DELETE FROM VisitorLog vl WHERE vl.logDate < :cutoffDate")
    int deleteByLogDateBefore(@Param("cutoffDate") LocalDate cutoffDate);

    /**
     * Находит последние N записей журнала.
     *
     * @param limit количество записей
     * @return список последних записей журнала
     */
    @Query("SELECT vl FROM VisitorLog vl ORDER BY vl.logDate DESC")
    List<VisitorLog> findLatest(@Param("limit") int limit);

    /**
     * Находит все записи журнала в указанном диапазоне дат, отсортированные по убыванию даты.
     * Используется для получения журналов за период.
     *
     * @param startDate начальная дата диапазона (включительно)
     * @param endDate конечная дата диапазона (включительно)
     * @return список записей журнала, отсортированный по дате в порядке убывания
     */
    List<VisitorLog> findAllByLogDateBetweenOrderByLogDateDesc(LocalDate startDate, LocalDate endDate);

    /**
     * Находит все записи журнала в указанном диапазоне дат.
     * Альтернативная версия с явным JPQL запросом.
     *
     * @param startDate начальная дата диапазона (включительно)
     * @param endDate конечная дата диапазона (включительно)
     * @return список записей журнала, отсортированный по дате в порядке убывания
     */
    @Query("SELECT vl FROM VisitorLog vl WHERE vl.logDate >= :startDate AND vl.logDate <= :endDate ORDER BY vl.logDate DESC")
    List<VisitorLog> findAllByDateRange(@Param("startDate") LocalDate startDate,
                                        @Param("endDate") LocalDate endDate);
}