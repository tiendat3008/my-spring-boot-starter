package com.spring.starter.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.spring.starter.config.JwtProperties;
import com.spring.starter.config.JwtTokenProvider;
import com.spring.starter.dto.request.AuthRequest;
import com.spring.starter.dto.request.RegisterRequest;
import com.spring.starter.dto.response.AuthResponse;
import com.spring.starter.dto.response.RegisterResponse;
import com.spring.starter.entity.User;
import com.spring.starter.enums.Role;
import com.spring.starter.exception.AppException;
import com.spring.starter.exception.ErrorCode;
import com.spring.starter.repository.UserRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthService {

    static Logger logger = LoggerFactory.getLogger(AuthService.class);

    UserRepository userRepository;
    PasswordEncoder passwordEncoder;
    JwtTokenProvider jwtTokenProvider;
    JwtProperties jwtProperties;
    RefreshTokenService refreshTokenService;

    @Transactional
    public RegisterResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.email())) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }

        var user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .build();
        user.addRole(Role.USER);
        user = userRepository.save(user);

        return new RegisterResponse(
                user.getId(),
                user.getEmail(),
                extractRoles(user)
        );
    }

    public AuthResponse authenticate(AuthRequest request){

        var user = userRepository
                .findByEmail(request.email())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        boolean authenticated = passwordEncoder.matches(request.password(), user.getPasswordHash());

        if (!authenticated)
            throw new AppException(ErrorCode.UNAUTHENTICATED);

        return generateTokens(user);
    }

    public AuthResponse refreshAccessToken(String refreshToken) {

        String jti = jwtTokenProvider.extractToken(refreshToken).getId();
        String username = jwtTokenProvider.extractToken(refreshToken).getSubject();

        if (!refreshTokenService.isRefreshTokenValid(jti)) {
            throw new AppException(ErrorCode.TOKEN_REVOKED);
        }

        var user = userRepository
                .findByEmail(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        refreshTokenService.revokeRefreshToken(jti);

        return generateTokens(user);
    }

    public void logout(String refreshToken) {
        String jti = jwtTokenProvider.extractToken(refreshToken).getId();
        refreshTokenService.revokeRefreshToken(jti);
    }

    private AuthResponse generateTokens(User user) {
        
        String accessToken = jwtTokenProvider.issueAccessToken(user);
        String refreshToken = jwtTokenProvider.issueRefreshToken(user);

        refreshTokenService.storeRefreshToken(
                user.getId().toString(),
                jwtTokenProvider.extractToken(refreshToken).getId(),
                jwtProperties.refreshTokenExpirySeconds());

        return new AuthResponse(
                accessToken,
                refreshToken,
                "Bearer",
                jwtProperties.accessTokenExpirySeconds());
    }

    private List<String> extractRoles(User user) {
        return user.getRoles().stream()
                .map(ur -> ur.getRole().name())
                .toList();
    }
}
