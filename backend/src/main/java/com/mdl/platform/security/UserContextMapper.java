package com.mdl.platform.security;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class UserContextMapper {

    private UserContextMapper() {
    }

    public static AuthenticatedUser toAuthenticatedUser(UserContext context) {
        var authorities = Stream.concat(
                        context.roles().stream().map(role -> "ROLE_" + role),
                        context.permissions().stream())
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());

        return new AuthenticatedUser(context, authorities);
    }
}
