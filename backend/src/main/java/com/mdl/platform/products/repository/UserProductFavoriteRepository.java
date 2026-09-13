package com.mdl.platform.products.repository;

import com.mdl.platform.products.entity.UserProductFavorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface UserProductFavoriteRepository extends JpaRepository<UserProductFavorite, Long> {

    Optional<UserProductFavorite> findByUserIdAndProductId(Long userId, Long productId);

    List<UserProductFavorite> findByUserIdOrderByCreatedAtDesc(Long userId);

    boolean existsByUserIdAndProductId(Long userId, Long productId);

    void deleteByUserIdAndProductId(Long userId, Long productId);

    @org.springframework.data.jpa.repository.Query("""
            SELECT f.productId FROM UserProductFavorite f
            WHERE f.userId = :userId AND f.productId IN :productIds
            """)
    Set<Long> findFavoriteProductIds(
            @org.springframework.data.repository.query.Param("userId") Long userId,
            @org.springframework.data.repository.query.Param("productIds") List<Long> productIds);
}
