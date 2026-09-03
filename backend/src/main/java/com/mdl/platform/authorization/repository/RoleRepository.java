package com.mdl.platform.authorization.repository;

import com.mdl.platform.authorization.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    List<Role> findByBusinessIdIsNullAndStatusOrderByNameAsc(String status);

    Optional<Role> findByCodeAndBusinessIdIsNull(String code);

    List<Role> findByCodeInAndBusinessIdIsNull(List<String> codes);
}
