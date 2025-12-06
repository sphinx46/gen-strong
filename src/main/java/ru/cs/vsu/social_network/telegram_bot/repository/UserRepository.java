package ru.cs.vsu.social_network.telegram_bot.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.cs.vsu.social_network.telegram_bot.entity.User;
import ru.cs.vsu.social_network.telegram_bot.entity.enums.ROLE;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Репозиторий для работы с пользователями бота.
 * Обеспечивает операции с учетными записями пользователей и их ролями.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Находит пользователя по Telegram ID.
     *
     * @param telegramId идентификатор пользователя в Telegram
     * @return Optional с пользователем, если найден
     */
    Optional<User> findByTelegramId(Long telegramId);

    /**
     * Проверяет существование пользователя с указанным Telegram ID.
     *
     * @param telegramId идентификатор пользователя в Telegram
     * @return true если пользователь существует, false в противном случае
     */
    boolean existsByTelegramId(Long telegramId);

    /**
     * Находит всех пользователей с указанной ролью.
     * Использует пагинацию для оптимизации работы с большим количеством пользователей.
     *
     * @param role роль пользователя
     * @param pageable параметры пагинации
     * @return страница с пользователями
     */
    Page<User> findAllByRole(ROLE role, Pageable pageable);

    /**
     * Обновляет отображаемое имя пользователя.
     *
     * @param userId идентификатор пользователя
     * @param displayName новое отображаемое имя
     * @return количество обновленных записей (0 или 1)
     */
    @Modifying
    @Query("UPDATE User u SET u.displayName = :displayName WHERE u.id = :userId")
    int updateDisplayName(@Param("userId") UUID userId, @Param("displayName") String displayName);

    /**
     * Находит пользователей по части отображаемого имени (поиск с пагинацией).
     *
     * @param displayNamePart часть отображаемого имени
     * @param pageable параметры пагинации
     * @return страница с пользователями
     */
    @Query("SELECT u FROM User u WHERE LOWER(u.displayName) LIKE LOWER(CONCAT('%', :displayNamePart, '%'))")
    Page<User> findByDisplayNameContainingIgnoreCase(@Param("displayNamePart") String displayNamePart, Pageable pageable);

    /**
     * Находит всех активных пользователей (тех, кто совершал посещения за последний месяц).
     *
     * @param oneMonthAgo дата, от которой отсчитывается месяц
     * @return список активных пользователей
     */
    @Query("SELECT DISTINCT v.user FROM Visit v WHERE v.visitDate >= :oneMonthAgo")
    List<User> findActiveUsers(@Param("oneMonthAgo") LocalDate oneMonthAgo);

    /**
     * Обновляет роль пользователя.
     *
     * @param userId идентификатор пользователя
     * @param role новая роль
     * @return количество обновленных записей (0 или 1)
     */
    @Modifying
    @Query("UPDATE User u SET u.role = :role WHERE u.id = :userId")
    int updateUserRole(@Param("userId") UUID userId, @Param("role") ROLE role);

    /**
     * Пакетное обновление ролей пользователей.
     * Оптимизация для массового обновления данных.
     *
     * @param userIds список идентификаторов пользователей
     * @param role новая роль
     * @return количество обновленных записей
     */
    @Modifying
    @Query("UPDATE User u SET u.role = :role WHERE u.id IN :userIds")
    int updateRolesInBatch(@Param("userIds") List<UUID> userIds, @Param("role") ROLE role);
}