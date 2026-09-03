package com.mdl.platform.authorization.repository;

import com.mdl.platform.authorization.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

    List<UserRole> findByUserIdAndBusinessId(Long userId, Long businessId);

    void deleteByUserIdAndBusinessId(Long userId, Long businessId);

    @Query(value = """
            SELECT r.code FROM user_roles ur
            JOIN roles r ON r.id = ur.role_id
            WHERE ur.user_id = :userId AND ur.business_id = :businessId
            """, nativeQuery = true)
    List<String> findRoleCodesByUserAndBusiness(@Param("userId") Long userId, @Param("businessId") Long businessId);
}
