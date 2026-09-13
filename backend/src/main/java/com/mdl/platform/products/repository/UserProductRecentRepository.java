package com.mdl.platform.products.repository;

import com.mdl.platform.products.entity.UserProductRecent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserProductRecentRepository extends JpaRepository<UserProductRecent, Long> {

    Optional<UserProductRecent> findByUserIdAndProductId(Long userId, Long productId);

    List<UserProductRecent> findTop20ByUserIdOrderBySelectedAtDesc(Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            DELETE FROM user_product_recents
            WHERE user_id = :userId
              AND product_id NOT IN (
                    SELECT product_id FROM (
                        SELECT product_id
                        FROM user_product_recents
                        WHERE user_id = :userId
                        ORDER BY selected_at DESC
                        LIMIT 40
                    ) keep_rows
              )
            """, nativeQuery = true)
    void trimOlderThan(@Param("userId") Long userId);
}
