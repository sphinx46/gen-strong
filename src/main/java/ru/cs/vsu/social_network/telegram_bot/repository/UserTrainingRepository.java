package ru.cs.vsu.social_network.telegram_bot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.cs.vsu.social_network.telegram_bot.entity.UserTraining;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Репозиторий для работы с данными тренировок пользователей.
 * Оптимизирован для работы с максимальным жимом лежа.
 */
@Repository
public interface UserTrainingRepository extends JpaRepository<UserTraining, UUID> {

    /**
     * Находит запись тренировок пользователя по ID пользователя.
     * Использует JOIN FETCH для загрузки связанного пользователя за один запрос.
     *
     * @param userId идентификатор пользователя
     * @return Optional с записью тренировок пользователя, если найдена
     */
    @Query("SELECT ut FROM UserTraining ut JOIN FETCH ut.user u WHERE u.id = :userId")
    Optional<UserTraining> findByUserId(@Param("userId") UUID userId);

    /**
     * Находит запись тренировок пользователя по Telegram ID пользователя.
     * Более эффективный запрос для Telegram бота.
     *
     * @param telegramId Telegram ID пользователя
     * @return Optional с записью тренировок пользователя, если найдена
     */
    @Query("SELECT ut FROM UserTraining ut JOIN FETCH ut.user u WHERE u.telegramId = :telegramId")
    Optional<UserTraining> findByUserTelegramId(@Param("telegramId") Long telegramId);

    /**
     * Обновляет максимальный жим лежа пользователя.
     * Использует нативный запрос для оптимизации.
     *
     * @param userId идентификатор пользователя
     * @param maxBenchPress новое значение максимального жима лежа
     * @param updatedAt время обновления
     * @param lastTrainingDate дата последней тренировки
     * @return количество обновленных записей
     */
    @Modifying
    @Query("UPDATE UserTraining ut SET ut.maxBenchPress = :maxBenchPress, " +
            "ut.updatedAt = :updatedAt, ut.lastTrainingDate = :lastTrainingDate " +
            "WHERE ut.user.id = :userId")
    int updateMaxBenchPress(@Param("userId") UUID userId,
                            @Param("maxBenchPress") Double maxBenchPress,
                            @Param("updatedAt") LocalDateTime updatedAt,
                            @Param("lastTrainingDate") LocalDateTime lastTrainingDate);

    /**
     * Проверяет существование записи тренировок для пользователя.
     *
     * @param userId идентификатор пользователя
     * @return true, если запись существует
     */
    @Query("SELECT CASE WHEN COUNT(ut) > 0 THEN true ELSE false END " +
            "FROM UserTraining ut WHERE ut.user.id = :userId")
    boolean existsByUserId(@Param("userId") UUID userId);

    /**
     * Удаляет запись тренировок по ID пользователя.
     *
     * @param userId идентификатор пользователя
     */
    @Modifying
    @Query("DELETE FROM UserTraining ut WHERE ut.user.id = :userId")
    void deleteByUserId(@Param("userId") UUID userId);
}