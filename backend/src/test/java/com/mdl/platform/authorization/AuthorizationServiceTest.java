package com.mdl.platform.authorization;

import com.mdl.platform.common.exception.ForbiddenException;
import com.mdl.platform.security.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthorizationServiceTest {

    private final AuthorizationService authorizationService = new AuthorizationService();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void allowsWhenPermissionIsGranted() {
        setContext(Set.of("user:view"), Set.of("VIEWER"));

        assertThatCode(() -> authorizationService.requirePermission("user:view"))
                .doesNotThrowAnyException();
    }

    @Test
    void deniesWhenPermissionIsMissing() {
        setContext(Set.of("sale:create"), Set.of("SHOP_WORKER"));

        assertThatThrownBy(() -> authorizationService.requirePermission("user:manage"))
                .isInstanceOf(ForbiddenException.class);
    }

    private void setContext(Set<String> permissions, Set<String> roles) {
        UserContext userContext = new UserContext(
                1L, "test@mdl.local", "test", 1L, "MDL", "GHS", roles, permissions, 1L);

        var authorities = permissions.stream().map(SimpleGrantedAuthority::new).toList();
        var authentication = new UsernamePasswordAuthenticationToken(
                new com.mdl.platform.security.AuthenticatedUser(userContext, authorities),
                null,
                authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
