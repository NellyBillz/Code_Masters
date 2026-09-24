package za.codemaster.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import za.codemaster.backend.domain.model.Notification;

import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /** A user's notifications, most recent first, paginated. */
    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /** Backs the bell icon's unread badge — a cheap count, not a full row load. */
    long countByUserIdAndReadAtIsNull(Long userId);

    /** Ownership-scoped lookup: a user can only ever mark their own notification read. */
    Optional<Notification> findByIdAndUserId(Long id, Long userId);

    /** Marks every one of a user's unread notifications read in one statement. */
    @Modifying
    @Query("UPDATE Notification n SET n.readAt = :now WHERE n.user.id = :userId AND n.readAt IS NULL")
    void markAllRead(@Param("userId") Long userId, @Param("now") OffsetDateTime now);
}
