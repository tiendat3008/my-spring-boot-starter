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

import com.spring.starter.auth.dto.ChangePasswordRequest;
import com.spring.starter.auth.dto.UserSessionResponse;
import com.spring.starter.auth.entity.User;
import com.spring.starter.auth.repository.UserRepository;
import com.spring.starter.common.config.JwtProperties;
import com.spring.starter.common.exception.AppException;
import com.spring.starter.common.exception.ErrorCode;
import com.spring.starter.common.security.JwtTokenProvider;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    JwtTokenProvider jwtTokenProvider;

    @Mock
    JwtProperties jwtProperties;

    @Mock
    RefreshTokenService refreshTokenService;

    @InjectMocks
    AuthService authService;

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

        var result = authService.listSessions("user@example.com");

        verify(refreshTokenService).listUserSessionIds("10");
        org.assertj.core.api.Assertions.assertThat(result)
                .extracting(UserSessionResponse::sessionId)
                .containsExactly("session-1", "session-2");
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
