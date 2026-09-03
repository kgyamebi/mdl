package com.mdl.platform.users.repository;

import com.mdl.platform.users.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findByUsernameIgnoreCase(String username);

    Optional<User> findByEmailIgnoreCaseOrUsernameIgnoreCase(String email, String username);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByUsernameIgnoreCase(String username);

    @Query("""
            SELECT u FROM User u
            WHERE u.id IN :ids
            AND (:search IS NULL OR :search = '' OR
                 LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR
                 LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')) OR
                 LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR
                 LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY u.firstName, u.lastName
            """)
    List<User> findByIdsAndSearch(@Param("ids") List<Long> ids, @Param("search") String search);
}
