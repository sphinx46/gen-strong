package ru.cs.vsu.social_network.telegram_bot.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.cs.vsu.social_network.telegram_bot.entity.User;
import ru.cs.vsu.social_network.telegram_bot.entity.Visit;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Репозиторий для работы с посещениями тренажерного зала.
 * Обеспечивает операции с индивидуальными записями посещений пользователей.
 */
@Repository
public interface VisitRepository extends JpaRepository<Visit, UUID> {

    /**
     * Находит посещение пользователя на конкретную дату.
     *
     * @param user пользователь
     * @param start начало дня
     * @param end конец дня
     * @return Optional с посещением, если найдено
     */
    Optional<Visit> findByUserAndVisitDateBetween(User user, LocalDateTime start, LocalDateTime end);

    /**
     * Проверяет, посещал ли пользователь зал в указанный день.
     *
     * @param userId идентификатор пользователя
     * @param date дата для проверки
     * @return true если пользователь посещал зал в этот день, false в противном случае
     */
    @Query("SELECT CASE WHEN COUNT(v) > 0 THEN true ELSE false END FROM Visit v " +
            "WHERE v.user.id = :userId AND DATE(v.visitDate) = :date")
    boolean existsByUserIdAndDate(@Param("userId") UUID userId, @Param("date") LocalDate date);

    /**
     * Находит все посещения за указанную дату с пагинацией.
     *
     * @param date дата посещений
     * @param pageable параметры пагинации
     * @return страница с посещениями
     */
    @Query("SELECT v FROM Visit v WHERE DATE(v.visitDate) = :date ORDER BY v.visitDate")
    Page<Visit> findAllByDate(@Param("date") LocalDate date, Pageable pageable);

    /**
     * Находит все посещения пользователя с пагинацией.
     *
     * @param user пользователь
     * @param pageable параметры пагинации
     * @return страница с посещениями пользователя
     */
    Page<Visit> findAllByUser(User user, Pageable pageable);

    /**
     * Находит всех пользователей, посетивших зал в указанный день.
     * Использует JOIN FETCH для оптимизации загрузки связанных сущностей.
     *
     * @param date дата посещений
     * @return список посещений с загруженными пользователями
     */
    @Query("SELECT v FROM Visit v JOIN FETCH v.user WHERE DATE(v.visitDate) = :date ORDER BY v.visitDate")
    List<Visit> findAllWithUsersByDate(@Param("date") LocalDate date);

    /**
     * Находит количество посещений за указанную дату.
     *
     * @param date дата для подсчета
     * @return количество посещений
     */
    @Query("SELECT COUNT(v) FROM Visit v WHERE DATE(v.visitDate) = :date")
    long countByDate(@Param("date") LocalDate date);

    /**
     * Находит все посещения за диапазон дат.
     *
     * @param startDate начальная дата (включительно)
     * @param endDate конечная дата (включительно)
     * @return список посещений
     */
    @Query("SELECT v FROM Visit v JOIN FETCH v.user WHERE DATE(v.visitDate) BETWEEN :startDate AND :endDate ORDER BY v.visitDate")
    List<Visit> findAllByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Удаляет все посещения старше указанной даты.
     * Выполняет batch-удаление для оптимизации.
     *
     * @param cutoffDate дата, старше которой посещения будут удалены
     * @return количество удаленных записей
     */
    @Modifying
    @Query("DELETE FROM Visit v WHERE v.visitDate < :cutoffDate")
    int deleteByVisitDateBefore(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Находит все посещения за последние N дней.
     * Использует функцию FUNCTION для вычисления даты.
     *
     * @param days количество дней для выборки
     * @return список посещений за указанный период
     */
    @Query("SELECT v FROM Visit v WHERE v.visitDate >= FUNCTION('DATE_SUB', CURRENT_DATE, :days, 'DAY')")
    List<Visit> findAllFromLastDays(@Param("days") Integer days);

    /**
     * Пакетное сохранение посещений.
     * Оптимизация для массовой вставки данных.
     *
     * @param visits список посещений для сохранения
     * @param batchSize размер пакета
     * @return список сохраненных посещений
     */
    default List<Visit> saveAllInBatch(List<Visit> visits, int batchSize) {
        for (int i = 0; i < visits.size(); i += batchSize) {
            List<Visit> batch = visits.subList(i, Math.min(i + batchSize, visits.size()));
            saveAll(batch);
            flush();
        }
        return visits;
    }
}