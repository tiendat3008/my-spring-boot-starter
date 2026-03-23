package com.spring.starter.auth.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.spring.starter.auth.dto.ForgotPasswordRequest;
import com.spring.starter.auth.dto.ResendOtpRequest;
import com.spring.starter.auth.dto.ResetPasswordRequest;
import com.spring.starter.auth.dto.VerifyEmailRequest;
import com.spring.starter.auth.repository.UserRepository;
import com.spring.starter.common.exception.AppException;
import com.spring.starter.common.exception.ErrorCode;
import com.spring.starter.infrastructure.mail.MailService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthRecoveryService {

    static final Logger logger = LoggerFactory.getLogger(AuthRecoveryService.class);

    static final String PURPOSE_RESET_PASSWORD = "reset-password";
    static final String PURPOSE_VERIFY_EMAIL = "verify-email";
    static final Set<String> ALLOWED_PURPOSES = Set.of(PURPOSE_RESET_PASSWORD, PURPOSE_VERIFY_EMAIL);

    static final Duration OTP_TTL = Duration.ofMinutes(5);

    UserRepository userRepository;
    StringRedisTemplate redisTemplate;
    MailService mailService;
    PasswordEncoder passwordEncoder;

    SecureRandom secureRandom = new SecureRandom();

    public void forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.email()).ifPresent(user -> issueOtp(request.email(), PURPOSE_RESET_PASSWORD));
    }

    public void resendOtp(ResendOtpRequest request) {
        String purpose = normalizePurpose(request.purpose());
        userRepository.findByEmail(request.email()).ifPresent(user -> issueOtp(request.email(), purpose));
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        var user = userRepository
                .findByEmail(request.email())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (!consumeOtp(request.email(), PURPOSE_RESET_PASSWORD, request.otp())) {
            throw new AppException(ErrorCode.OTP_INVALID_OR_EXPIRED);
        }

        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.PASSWORD_NOT_CHANGED);
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    }

    public void verifyEmail(VerifyEmailRequest request) {
        if (!consumeOtp(request.email(), PURPOSE_VERIFY_EMAIL, request.otp())) {
            throw new AppException(ErrorCode.OTP_INVALID_OR_EXPIRED);
        }
    }

    private void issueOtp(String email, String purpose) {
        String otp = generateOtpCode();
        redisTemplate.opsForValue().set(buildOtpKey(email, purpose), otp, OTP_TTL);
        mailService.sendOtpEmail(email, otp, purpose);
    }

    private boolean consumeOtp(String email, String purpose, String inputOtp) {
        String key = buildOtpKey(email, purpose);
        String expectedOtp = redisTemplate.opsForValue().get(key);
        logger.debug("Consuming OTP for key {}: expected={}, input={}", key, expectedOtp, inputOtp);

        if (expectedOtp == null || !expectedOtp.equals(inputOtp)) {
            return false;
        }

        redisTemplate.delete(key);
        return true;
    }

    private String normalizePurpose(String purpose) {
        String normalized = purpose == null ? "" : purpose.trim().toLowerCase();

        if (!ALLOWED_PURPOSES.contains(normalized)) {
            throw new AppException(ErrorCode.OTP_PURPOSE_UNSUPPORTED);
        }

        return normalized;
    }

    private String buildOtpKey(String email, String purpose) {
        return "otp:" + purpose + ":" + email.toLowerCase();
    }

    private String generateOtpCode() {
        int value = secureRandom.nextInt(900000) + 100000;
        return String.valueOf(value);
    }
}
