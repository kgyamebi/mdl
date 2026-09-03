package com.mdl.platform.authorization.repository;

import com.mdl.platform.authorization.projection.UserAuthProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.mdl.platform.users.entity.User;

import java.util.Optional;

@Repository
public interface UserAuthProfileRepository extends JpaRepository<User, Long> {

    @Query(value = """
            SELECT b.id AS businessId, b.code AS businessCode, b.name AS businessName,
                   b.currency_code AS currencyCode
            FROM user_business_memberships m
            JOIN businesses b ON b.id = m.business_id
            WHERE m.user_id = :userId AND m.status = 'ACTIVE'
            ORDER BY m.is_default DESC, m.joined_at ASC
            LIMIT 1
            """, nativeQuery = true)
    Optional<UserAuthProfile.BusinessProfile> findDefaultBusiness(@Param("userId") Long userId);

    @Query(value = """
            SELECT DISTINCT r.code
            FROM user_roles ur
            JOIN roles r ON r.id = ur.role_id
            WHERE ur.user_id = :userId AND ur.business_id = :businessId AND r.status = 'ACTIVE'
            """, nativeQuery = true)
    java.util.List<String> findRoleCodes(@Param("userId") Long userId, @Param("businessId") Long businessId);

    @Query(value = """
            SELECT DISTINCT p.code
            FROM user_roles ur
            JOIN role_permissions rp ON rp.role_id = ur.role_id
            JOIN permissions p ON p.id = rp.permission_id
            WHERE ur.user_id = :userId AND ur.business_id = :businessId
            """, nativeQuery = true)
    java.util.List<String> findPermissionCodes(@Param("userId") Long userId, @Param("businessId") Long businessId);

    @Query(value = """
            SELECT DISTINCT ur.user_id
            FROM user_roles ur
            JOIN role_permissions rp ON rp.role_id = ur.role_id
            JOIN permissions p ON p.id = rp.permission_id
            WHERE ur.business_id = :businessId AND p.code = :permissionCode
            """, nativeQuery = true)
    java.util.List<Long> findUserIdsWithPermission(
            @Param("businessId") Long businessId,
            @Param("permissionCode") String permissionCode);
}
