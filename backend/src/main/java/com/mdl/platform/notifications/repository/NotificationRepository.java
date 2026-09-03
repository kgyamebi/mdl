package com.mdl.platform.notifications.repository;

import com.mdl.platform.notifications.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Optional<Notification> findByIdAndBusinessIdAndUserId(Long id, Long businessId, Long userId);

    Optional<Notification> findByBusinessIdAndUserIdAndDedupeKeyAndStatus(
            Long businessId, Long userId, String dedupeKey, String status);

    long countByBusinessIdAndUserIdAndStatus(Long businessId, Long userId, String status);

    @Query("""
            SELECT n FROM Notification n
            WHERE n.businessId = :businessId
              AND n.userId = :userId
              AND (:status IS NULL OR n.status = :status)
              AND (:category IS NULL OR n.category = :category)
            ORDER BY n.createdAt DESC
            """)
    Page<Notification> search(
            @Param("businessId") Long businessId,
            @Param("userId") Long userId,
            @Param("status") String status,
            @Param("category") String category,
            Pageable pageable);

    @Modifying
    @Query("""
            UPDATE Notification n
            SET n.status = 'READ', n.readAt = :readAt
            WHERE n.businessId = :businessId
              AND n.userId = :userId
              AND n.status = 'UNREAD'
            """)
    int markAllRead(
            @Param("businessId") Long businessId,
            @Param("userId") Long userId,
            @Param("readAt") Instant readAt);

    List<Notification> findTop5ByBusinessIdAndUserIdAndStatusOrderByCreatedAtDesc(
            Long businessId, Long userId, String status);
}
