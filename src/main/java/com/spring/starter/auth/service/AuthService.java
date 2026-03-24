package com.spring.starter.auth.service;

import java.time.Instant;
import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.spring.starter.auth.dto.AuthRequest;
import com.spring.starter.auth.dto.AuthResponse;
import com.spring.starter.auth.dto.ChangePasswordRequest;
import com.spring.starter.auth.dto.RegisterRequest;
import com.spring.starter.auth.dto.RegisterResponse;
import com.spring.starter.auth.dto.SessionMetadata;
import com.spring.starter.auth.dto.UserSessionResponse;
import com.spring.starter.auth.entity.User;
import com.spring.starter.auth.enums.Role;
import com.spring.starter.auth.enums.UserStatus;
import com.spring.starter.auth.repository.UserRepository;
import com.spring.starter.common.config.JwtProperties;
import com.spring.starter.common.exception.AppException;
import com.spring.starter.common.exception.ErrorCode;
import com.spring.starter.common.security.JwtTokenProvider;
import com.spring.starter.user.entity.UserProfile;
import com.spring.starter.user.repository.UserProfileRepository;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthService {

    UserRepository userRepository;
    UserProfileRepository userProfileRepository;
    PasswordEncoder passwordEncoder;
    JwtTokenProvider jwtTokenProvider;
    JwtProperties jwtProperties;
    RefreshTokenService refreshTokenService;
    AuthRecoveryService authRecoveryService;

    @Transactional
    public RegisterResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.email())) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }

        var user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .status(UserStatus.UNVERIFIED)
                .build();
        user.addRole(Role.USER);
        user = userRepository.save(user);

        // Create default profile
        var profile = UserProfile.builder()
                .userId(user.getId())
                .firstName(request.firstName())
                .lastName(request.lastName())
                .build();
        userProfileRepository.save(profile);

        authRecoveryService.sendVerifyEmailOtp(user.getEmail());

        return new RegisterResponse(
                user.getId(), user.getEmail(), extractRoles(user), profile.getFirstName(), profile.getLastName());
    }

    public AuthResponse authenticate(AuthRequest request, HttpServletRequest httpRequest) {

        var user = userRepository
                .findByEmail(request.email())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        boolean authenticated = passwordEncoder.matches(request.password(), user.getPasswordHash());

        if (!authenticated) throw new AppException(ErrorCode.UNAUTHENTICATED);

        if (user.getStatus() == UserStatus.UNVERIFIED) {
            throw new AppException(ErrorCode.USER_EMAIL_NOT_VERIFIED);
        }

        return generateTokens(user, httpRequest);
    }

    public AuthResponse authenticate(AuthRequest request) {
        return authenticate(request, null);
    }

    public AuthResponse refreshAccessToken(String refreshToken, HttpServletRequest httpRequest) {

        String jti = jwtTokenProvider.extractToken(refreshToken).getId();
        String username = jwtTokenProvider.extractToken(refreshToken).getSubject();

        if (!refreshTokenService.isRefreshTokenValid(jti)) {
            throw new AppException(ErrorCode.TOKEN_REVOKED);
        }

        var user = userRepository.findByEmail(username).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        refreshTokenService.revokeRefreshToken(jti);

        return generateTokens(user, httpRequest);
    }

    public AuthResponse refreshAccessToken(String refreshToken) {
        return refreshAccessToken(refreshToken, null);
    }

    public void logout(String refreshToken) {
        String jti = jwtTokenProvider.extractToken(refreshToken).getId();
        refreshTokenService.revokeRefreshToken(jti);
    }

    @Transactional
    public void changePassword(String email, ChangePasswordRequest request, String refreshToken) {
        var user = userRepository.findByEmail(email).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.INVALID_CURRENT_PASSWORD);
        }

        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.PASSWORD_NOT_CHANGED);
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));

        if (refreshToken != null && !refreshToken.isBlank()) {
            String jti = jwtTokenProvider.extractToken(refreshToken).getId();
            refreshTokenService.revokeRefreshToken(jti);
        }
    }

    public AuthResponse generateTokens(User user, HttpServletRequest httpRequest) {

        String accessToken = jwtTokenProvider.issueAccessToken(user);
        String refreshToken = jwtTokenProvider.issueRefreshToken(user);

        SessionMetadata sessionMetadata = buildSessionMetadata(httpRequest);

        refreshTokenService.storeRefreshToken(
                user.getId().toString(),
                jwtTokenProvider.extractToken(refreshToken).getId(),
                jwtProperties.refreshTokenExpirySeconds(),
                sessionMetadata);

        return new AuthResponse(accessToken, refreshToken, "Bearer", jwtProperties.accessTokenExpirySeconds());
    }

    public AuthResponse generateTokens(User user) {
        return generateTokens(user, null);
    }

    @Transactional(readOnly = true)
    public List<UserSessionResponse> listSessions(String email) {
        var user = userRepository.findByEmail(email).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        return refreshTokenService.listUserSessionIds(user.getId().toString()).stream()
                .map(sessionId -> {
                    SessionMetadata metadata = refreshTokenService.getSessionMetadata(sessionId);

                    return new UserSessionResponse(
                            sessionId,
                            metadata == null ? null : metadata.ipAddress(),
                            metadata == null ? null : metadata.userAgent(),
                            metadata == null ? null : metadata.loginTime());
                })
                .toList();
    }

    public void revokeSession(String email, String sessionId) {
        var user = userRepository.findByEmail(email).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        boolean revoked = refreshTokenService.revokeUserSession(user.getId().toString(), sessionId);
        if (!revoked) {
            throw new AppException(ErrorCode.SESSION_NOT_FOUND);
        }
    }

    private List<String> extractRoles(User user) {
        return user.getRoles().stream().map(ur -> ur.getRole().name()).toList();
    }

    private SessionMetadata buildSessionMetadata(HttpServletRequest request) {
        if (request == null) {
            return new SessionMetadata(null, null, Instant.now());
        }

        return new SessionMetadata(extractClientIp(request), request.getHeader("User-Agent"), Instant.now());
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");

        if (forwardedFor == null || forwardedFor.isBlank()) {
            return request.getRemoteAddr();
        }

        String[] forwardedIps = forwardedFor.split(",");
        return forwardedIps.length == 0 ? request.getRemoteAddr() : forwardedIps[0].trim();
    }
}
