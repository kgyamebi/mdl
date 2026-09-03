package com.mdl.platform.auth.service;

import com.mdl.platform.alerts.service.AlertNotifier;
import com.mdl.platform.audit.service.AuditRecorder;
import com.mdl.platform.auth.dto.LoginRequest;
import com.mdl.platform.authorization.projection.UserAuthProfile;
import com.mdl.platform.authorization.repository.UserAuthProfileRepository;
import com.mdl.platform.common.exception.UnauthorizedException;
import com.mdl.platform.security.TokenIssuer;
import com.mdl.platform.users.entity.User;
import com.mdl.platform.users.repository.UserRepository;
import com.mdl.platform.users.repository.UserSessionRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSessionRepository userSessionRepository;

    @Mock
    private UserAuthProfileRepository userAuthProfileRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenIssuer tokenIssuer;

    @Mock
    private AuditRecorder auditRecorder;

    @Mock
    private AlertNotifier alertNotifier;

    @Mock
    private HttpServletRequest httpServletRequest;

    private AuthService authService;

    private User user;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                userSessionRepository,
                userAuthProfileRepository,
                passwordEncoder,
                tokenIssuer,
                auditRecorder,
                alertNotifier,
                15);

        user = new User();
        user.setId(1L);
        user.setEmail("owner@mdl.local");
        user.setUsername("owner");
        user.setPasswordHash("hashed");
        user.setFirstName("MDL");
        user.setLastName("Owner");
        user.setStatus("ACTIVE");
    }

    @Test
    void loginSucceedsWithValidCredentials() {
        LoginRequest request = new LoginRequest("owner@mdl.local", "Owner@123!");
        UserAuthProfile.BusinessProfile business = businessProfile();

        when(userRepository.findByEmailIgnoreCaseOrUsernameIgnoreCase("owner@mdl.local", "owner@mdl.local"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Owner@123!", "hashed")).thenReturn(true);
        when(userAuthProfileRepository.findDefaultBusiness(1L)).thenReturn(Optional.of(business));
        when(userAuthProfileRepository.findRoleCodes(1L, 10L)).thenReturn(List.of("OWNER"));
        when(userAuthProfileRepository.findPermissionCodes(1L, 10L)).thenReturn(List.of("business:view"));
        when(tokenIssuer.generateRefreshToken()).thenReturn("refresh-token");
        when(tokenIssuer.generateAccessToken(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn("access-token");
        when(tokenIssuer.getRefreshTokenExpiryDays()).thenReturn(7L);
        when(httpServletRequest.getRemoteAddr()).thenReturn("127.0.0.1");

        var response = authService.login(request, httpServletRequest);

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.user().businessCode()).isEqualTo("MDL");
        assertThat(response.user().roles()).containsExactly("OWNER");
        verify(userRepository).save(user);
        verify(userSessionRepository).save(any());
    }

    @Test
    void loginFailsWithInvalidPassword() {
        LoginRequest request = new LoginRequest("owner@mdl.local", "wrong");

        when(userRepository.findByEmailIgnoreCaseOrUsernameIgnoreCase("owner@mdl.local", "owner@mdl.local"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request, httpServletRequest))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid email/username or password");
    }

    private UserAuthProfile.BusinessProfile businessProfile() {
        return new UserAuthProfile.BusinessProfile() {
            @Override
            public Long getBusinessId() {
                return 10L;
            }

            @Override
            public String getBusinessCode() {
                return "MDL";
            }

            @Override
            public String getBusinessName() {
                return "Modern Dream Light";
            }

            @Override
            public String getCurrencyCode() {
                return "GHS";
            }
        };
    }
}
