package com.mdl.platform.locations.repository;

import com.mdl.platform.locations.entity.UserLocationAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserLocationAssignmentRepository extends JpaRepository<UserLocationAssignment, Long> {

    List<UserLocationAssignment> findByUserIdAndBusinessId(Long userId, Long businessId);

    void deleteByUserIdAndBusinessId(Long userId, Long businessId);

    @Query(value = """
            SELECT l.id, l.code, l.name, l.location_type, ula.access_level
            FROM user_location_assignments ula
            JOIN locations l ON l.id = ula.location_id
            WHERE ula.user_id = :userId AND ula.business_id = :businessId
            ORDER BY l.name
            """, nativeQuery = true)
    List<Object[]> findLocationDetailsByUserAndBusiness(@Param("userId") Long userId, @Param("businessId") Long businessId);
}
