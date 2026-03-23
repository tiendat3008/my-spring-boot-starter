package com.spring.starter.auth.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.spring.starter.auth.dto.AuthRequest;
import com.spring.starter.auth.dto.RegisterRequest;
import com.spring.starter.auth.dto.SessionMetadata;
import com.spring.starter.auth.dto.ChangePasswordRequest;
import com.spring.starter.auth.dto.UserSessionResponse;
import com.spring.starter.auth.entity.User;
import com.spring.starter.auth.enums.UserStatus;
import com.spring.starter.auth.repository.UserRepository;
import com.spring.starter.user.entity.UserProfile;
import com.spring.starter.user.repository.UserProfileRepository;
import com.spring.starter.common.config.JwtProperties;
import com.spring.starter.common.exception.AppException;
import com.spring.starter.common.exception.ErrorCode;
import com.spring.starter.common.security.JwtTokenProvider;

import jakarta.servlet.http.HttpServletRequest;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    UserProfileRepository userProfileRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    JwtTokenProvider jwtTokenProvider;

    @Mock
    JwtProperties jwtProperties;

    @Mock
    RefreshTokenService refreshTokenService;

    @Mock
    AuthRecoveryService authRecoveryService;

    @InjectMocks
    AuthService authService;

    @Test
    void register_shouldSendVerifyOtp_whenUserCreated() {
        var request = new RegisterRequest("user@example.com", "password123", "Dat", "Tran");

        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(10L);
            return user;
        });
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = authService.register(request);

        org.assertj.core.api.Assertions.assertThat(result.userId()).isEqualTo(10L);
        verify(authRecoveryService).sendVerifyEmailOtp("user@example.com");
    }

    @Test
    void changePassword_shouldUpdatePassword_whenCurrentPasswordIsValid() {
        var user = User.builder().email("user@example.com").passwordHash("old-hash").build();
        var request = new ChangePasswordRequest("old-password", "new-password");

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old-password", "old-hash")).thenReturn(true);
        when(passwordEncoder.matches("new-password", "old-hash")).thenReturn(false);
        when(passwordEncoder.encode("new-password")).thenReturn("new-hash");

        authService.changePassword("user@example.com", request, null);

        verify(userRepository).findByEmail("user@example.com");
        verify(passwordEncoder).encode("new-password");
    }

    @Test
    void changePassword_shouldThrow_whenCurrentPasswordInvalid() {
        var user = User.builder().email("user@example.com").passwordHash("old-hash").build();
        var request = new ChangePasswordRequest("wrong-password", "new-password");

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "old-hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.changePassword("user@example.com", request, null))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_CURRENT_PASSWORD);

        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void changePassword_shouldThrow_whenNewPasswordMatchesCurrentPassword() {
        var user = User.builder().email("user@example.com").passwordHash("old-hash").build();
        var request = new ChangePasswordRequest("old-password", "old-password");

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old-password", "old-hash")).thenReturn(true);

        assertThatThrownBy(() -> authService.changePassword("user@example.com", request, null))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PASSWORD_NOT_CHANGED);

        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void listSessions_shouldReturnUserSessions() {
        var user = User.builder().email("user@example.com").build();
        user.setId(10L);

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(refreshTokenService.listUserSessionIds("10")).thenReturn(List.of("session-1", "session-2"));
        when(refreshTokenService.getSessionMetadata("session-1"))
            .thenReturn(new SessionMetadata("203.0.113.5", "Chrome", java.time.Instant.parse("2026-03-23T10:15:30Z")));
        when(refreshTokenService.getSessionMetadata("session-2"))
            .thenReturn(new SessionMetadata("198.51.100.7", "Firefox", java.time.Instant.parse("2026-03-23T11:15:30Z")));

        var result = authService.listSessions("user@example.com");

        verify(refreshTokenService).listUserSessionIds("10");
        org.assertj.core.api.Assertions.assertThat(result)
                .extracting(UserSessionResponse::sessionId)
                .containsExactly("session-1", "session-2");
        org.assertj.core.api.Assertions.assertThat(result)
            .extracting(UserSessionResponse::ipAddress)
            .containsExactly("203.0.113.5", "198.51.100.7");
    }

        @Test
        void authenticate_shouldThrow_whenUserIsUnverified() {
        var request = new AuthRequest("user@example.com", "password123");
        var user = User.builder()
            .email("user@example.com")
            .passwordHash("hash")
            .status(UserStatus.UNVERIFIED)
            .build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hash")).thenReturn(true);

        assertThatThrownBy(() -> authService.authenticate(request, null))
            .isInstanceOf(AppException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.USER_EMAIL_NOT_VERIFIED);
        }

        @Test
        void authenticate_shouldStoreSessionMetadata_whenCredentialsValid() {
        var request = new AuthRequest("user@example.com", "password123");
        var user = User.builder()
            .email("user@example.com")
            .passwordHash("hash")
            .status(UserStatus.ACTIVE)
            .build();
        user.setId(10L);

        HttpServletRequest httpRequest = org.mockito.Mockito.mock(HttpServletRequest.class);

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hash")).thenReturn(true);
        when(jwtTokenProvider.issueAccessToken(user)).thenReturn("access-token");
        when(jwtTokenProvider.issueRefreshToken(user)).thenReturn("refresh-token");
        when(jwtTokenProvider.extractToken("refresh-token"))
            .thenReturn(org.springframework.security.oauth2.jwt.Jwt.withTokenValue("refresh-token")
                .header("alg", "none")
                .claim("type", "refresh")
                .claim("jti", "jti-1")
                .subject("user@example.com")
                .issuedAt(java.time.Instant.now())
                .expiresAt(java.time.Instant.now().plusSeconds(3600))
                .build());
        when(jwtProperties.refreshTokenExpirySeconds()).thenReturn(604800L);
        when(jwtProperties.accessTokenExpirySeconds()).thenReturn(3600L);
        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn("203.0.113.5, 10.0.0.1");
        when(httpRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0");

        authService.authenticate(request, httpRequest);

        verify(refreshTokenService).storeRefreshToken(
            org.mockito.ArgumentMatchers.eq("10"),
            org.mockito.ArgumentMatchers.eq("jti-1"),
            org.mockito.ArgumentMatchers.eq(604800L),
            org.mockito.ArgumentMatchers.argThat(metadata ->
                metadata != null
                    && "203.0.113.5".equals(metadata.ipAddress())
                    && "Mozilla/5.0".equals(metadata.userAgent())
                    && metadata.loginTime() != null
            ));
        }

    @Test
    void revokeSession_shouldThrow_whenSessionMissing() {
        var user = User.builder().email("user@example.com").build();
        user.setId(10L);

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(refreshTokenService.revokeUserSession("10", "session-x")).thenReturn(false);

        assertThatThrownBy(() -> authService.revokeSession("user@example.com", "session-x"))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SESSION_NOT_FOUND);
    }
}
