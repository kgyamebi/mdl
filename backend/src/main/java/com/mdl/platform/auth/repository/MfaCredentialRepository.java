package com.mdl.platform.auth.repository;

import com.mdl.platform.auth.entity.MfaCredential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MfaCredentialRepository extends JpaRepository<MfaCredential, Long> {

    List<MfaCredential> findByUserId(Long userId);

    Optional<MfaCredential> findByUserIdAndPrimaryTrue(Long userId);

    void deleteByUserIdAndPrimaryFalse(Long userId);
}
