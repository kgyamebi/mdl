package com.mdl.platform.authorization;

import com.mdl.platform.common.exception.ForbiddenException;
import com.mdl.platform.security.SecurityUtils;
import com.mdl.platform.security.UserContext;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Set;

/**
 * Server-side permission checks — never rely on the frontend alone.
 */
@Service
public class AuthorizationService {

    public UserContext requireAuthenticated() {
        return SecurityUtils.requireCurrentUser();
    }

    public void requirePermission(String permission) {
        UserContext context = requireAuthenticated();
        if (hasPrivilegedRole(context)) {
            return;
        }
        if (!context.permissions().contains(permission)) {
            throw new ForbiddenException("You do not have permission to perform this action");
        }
    }

    public void requireAnyPermission(String... permissions) {
        UserContext context = requireAuthenticated();
        if (hasPrivilegedRole(context)) {
            return;
        }
        Set<String> granted = context.permissions();
        boolean allowed = Arrays.stream(permissions).anyMatch(granted::contains);
        if (!allowed) {
            throw new ForbiddenException("You do not have permission to perform this action");
        }
    }

    public void requireRole(String roleCode) {
        UserContext context = requireAuthenticated();
        if (!context.roles().contains(roleCode)) {
            throw new ForbiddenException("You do not have permission to perform this action");
        }
    }

    public void requireSameBusiness(Long businessId) {
        UserContext context = requireAuthenticated();
        if (!context.businessId().equals(businessId)) {
            throw new ForbiddenException("Access denied for this business");
        }
    }

    private boolean hasPrivilegedRole(UserContext context) {
        return context.roles().contains("OWNER") || context.roles().contains("SUPER_ADMIN");
    }
}
