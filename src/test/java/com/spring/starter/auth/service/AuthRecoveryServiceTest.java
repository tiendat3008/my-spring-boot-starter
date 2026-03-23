package com.spring.starter.auth.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.spring.starter.auth.dto.ForgotPasswordRequest;
import com.spring.starter.auth.dto.ResendOtpRequest;
import com.spring.starter.auth.dto.ResetPasswordRequest;
import com.spring.starter.auth.dto.VerifyEmailRequest;
import com.spring.starter.auth.entity.User;
import com.spring.starter.auth.enums.UserStatus;
import com.spring.starter.auth.repository.UserRepository;
import com.spring.starter.common.exception.AppException;
import com.spring.starter.common.exception.ErrorCode;
import com.spring.starter.infrastructure.mail.MailService;

@ExtendWith(MockitoExtension.class)
class AuthRecoveryServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    StringRedisTemplate redisTemplate;

    @Mock
    ValueOperations<String, String> valueOperations;

    @Mock
    MailService mailService;

    @Mock
    PasswordEncoder passwordEncoder;

    @InjectMocks
    AuthRecoveryService authRecoveryService;

    @Test
    void forgotPassword_shouldIssueOtp_whenUserExists() {
        var request = new ForgotPasswordRequest("user@example.com");
        var user = User.builder().email("user@example.com").passwordHash("hash").build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        authRecoveryService.forgotPassword(request);

        verify(valueOperations).set(any(), any(), eq(Duration.ofMinutes(5)));
        verify(mailService).sendOtpEmail(eq("user@example.com"), any(), eq("reset-password"));
    }

    @Test
    void forgotPassword_shouldNotIssueOtp_whenUserNotExists() {
        var request = new ForgotPasswordRequest("missing@example.com");

        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        authRecoveryService.forgotPassword(request);

        verify(redisTemplate, never()).opsForValue();
        verify(mailService, never()).sendOtpEmail(any(), any(), any());
    }

    @Test
    void resendOtp_shouldThrow_whenPurposeUnsupported() {
        var request = new ResendOtpRequest("user@example.com", "signup");

        assertThatThrownBy(() -> authRecoveryService.resendOtp(request))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.OTP_PURPOSE_UNSUPPORTED);
    }

    @Test
    void resetPassword_shouldThrow_whenOtpInvalid() {
        var request = new ResetPasswordRequest("user@example.com", "123456", "new-password");
        var user = User.builder().email("user@example.com").passwordHash("hash").build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("otp:reset-password:user@example.com")).thenReturn(null);

        assertThatThrownBy(() -> authRecoveryService.resetPassword(request))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.OTP_INVALID_OR_EXPIRED);
    }

    @Test
    void verifyEmail_shouldThrow_whenOtpInvalid() {
        var request = new VerifyEmailRequest("user@example.com", "123456");
        var user = User.builder().email("user@example.com").status(UserStatus.UNVERIFIED).build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("otp:verify-email:user@example.com")).thenReturn("000000");

        assertThatThrownBy(() -> authRecoveryService.verifyEmail(request))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.OTP_INVALID_OR_EXPIRED);
    }

    @Test
    void verifyEmail_shouldActivateUser_whenOtpValid() {
        var request = new VerifyEmailRequest("user@example.com", "123456");
        var user = User.builder().email("user@example.com").status(UserStatus.UNVERIFIED).build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("otp:verify-email:user@example.com")).thenReturn("123456");

        authRecoveryService.verifyEmail(request);

        org.assertj.core.api.Assertions.assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        verify(redisTemplate).delete("otp:verify-email:user@example.com");
    }

    @Test
    void verifyEmail_shouldThrow_whenUserNotExists() {
        var request = new VerifyEmailRequest("missing@example.com", "123456");

        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authRecoveryService.verifyEmail(request))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_EXISTED);
    }
}
