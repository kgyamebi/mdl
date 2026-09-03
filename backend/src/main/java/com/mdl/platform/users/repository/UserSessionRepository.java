package com.mdl.platform.users.repository;

import com.mdl.platform.users.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

    Optional<UserSession> findByRefreshTokenHashAndStatus(String refreshTokenHash, String status);
}
