package com.mdl.platform.users.repository;

import com.mdl.platform.users.entity.UserBusinessMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserBusinessMembershipRepository extends JpaRepository<UserBusinessMembership, Long> {

    Optional<UserBusinessMembership> findByUserIdAndBusinessId(Long userId, Long businessId);

    boolean existsByUserIdAndBusinessId(Long userId, Long businessId);

    boolean existsByUserIdAndBusinessIdAndStatus(Long userId, Long businessId, String status);

    @Query(value = """
            SELECT u.id FROM users u
            JOIN user_business_memberships m ON m.user_id = u.id
            WHERE m.business_id = :businessId
            ORDER BY u.first_name, u.last_name
            """, nativeQuery = true)
    List<Long> findUserIdsByBusinessId(@Param("businessId") Long businessId);

    @Query("""
            SELECT u FROM User u
            JOIN UserBusinessMembership m ON m.userId = u.id
            WHERE m.businessId = :businessId
              AND m.status = 'ACTIVE'
              AND u.lockedUntil IS NOT NULL
              AND u.lockedUntil > :now
            """)
    List<com.mdl.platform.users.entity.User> findLockedUsersInBusiness(
            @Param("businessId") Long businessId,
            @Param("now") java.time.Instant now);
}
