package com.mdl.platform.authorization.service;

import com.mdl.platform.authorization.AuthorizationService;
import com.mdl.platform.authorization.dto.PermissionResponse;
import com.mdl.platform.authorization.dto.RoleResponse;
import com.mdl.platform.authorization.entity.Permission;
import com.mdl.platform.authorization.entity.Role;
import com.mdl.platform.authorization.repository.PermissionRepository;
import com.mdl.platform.authorization.repository.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class RoleService {

    private final AuthorizationService authorizationService;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public RoleService(
            AuthorizationService authorizationService,
            RoleRepository roleRepository,
            PermissionRepository permissionRepository) {
        this.authorizationService = authorizationService;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    public List<RoleResponse> listSystemRoles() {
        authorizationService.requireAnyPermission("user:view", "user:manage");

        return roleRepository.findByBusinessIdIsNullAndStatusOrderByNameAsc("ACTIVE").stream()
                .map(this::toRoleResponse)
                .toList();
    }

    public List<PermissionResponse> listPermissions() {
        authorizationService.requirePermission("user:manage");

        return permissionRepository.findAllByOrderByModuleAscCodeAsc().stream()
                .map(this::toPermissionResponse)
                .toList();
    }

    public Role findSystemRoleByCode(String code) {
        return roleRepository.findByCodeAndBusinessIdIsNull(code)
                .orElseThrow(() -> new com.mdl.platform.common.exception.NotFoundException("Role not found: " + code));
    }

    private RoleResponse toRoleResponse(Role role) {
        return new RoleResponse(role.getId(), role.getCode(), role.getName(), role.getDescription(), role.isSystem());
    }

    private PermissionResponse toPermissionResponse(Permission permission) {
        return new PermissionResponse(
                permission.getId(),
                permission.getCode(),
                permission.getName(),
                permission.getModule());
    }
}
