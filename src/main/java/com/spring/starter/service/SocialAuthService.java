package com.spring.starter.service;

import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.spring.starter.config.JwtProperties;
import com.spring.starter.config.JwtTokenProvider;
import com.spring.starter.dto.request.SocialLoginRequest;
import com.spring.starter.dto.response.SocialLoginResponse;
import com.spring.starter.entity.SocialAccount;
import com.spring.starter.entity.User;
import com.spring.starter.enums.Role;
import com.spring.starter.enums.SocialProvider;
import com.spring.starter.exception.AppException;
import com.spring.starter.exception.ErrorCode;
import com.spring.starter.repository.SocialAccountRepository;
import com.spring.starter.repository.UserRepository;
import com.spring.starter.service.social.SocialProfile;
import com.spring.starter.service.social.SocialProviderClient;
import com.spring.starter.service.social.SocialProviderClientFactory;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SocialAuthService {

    UserRepository userRepository;
    SocialAccountRepository socialAccountRepository;
    PasswordEncoder passwordEncoder;
    JwtTokenProvider jwtTokenProvider;
    JwtProperties jwtProperties;
    RefreshTokenService refreshTokenService;    
    OAuthStateService oAuthStateService;
    SocialProviderClientFactory clientFactory;

    @Transactional
    public SocialLoginResponse authenticate(SocialProvider provider, SocialLoginRequest request) {

        // Validate state to prevent CSRF attacks
        if (!oAuthStateService.consumeState(provider, request.state())) {
            throw new AppException(ErrorCode.OAUTH_STATE_INVALID);
        }

        // Strategy pattern to handle multiple providers in a clean way
        SocialProviderClient client = clientFactory.getClient(provider);

        // Delegate the authentication and user info fetching to the provider-specific client
        SocialProfile profile = client.authenticate(request);

        // Business logic
        AccountResolution accountResolution = resolveUser(provider, profile);

        String accessToken = jwtTokenProvider.issueAccessToken(accountResolution.user());
        String refreshToken = jwtTokenProvider.issueRefreshToken(accountResolution.user());

        String refreshJti = jwtTokenProvider.extractToken(refreshToken).getId();
        refreshTokenService.storeRefreshToken(
                accountResolution.user().getId().toString(),
                refreshJti,
                jwtProperties.refreshTokenExpirySeconds());

        return new SocialLoginResponse(
                accessToken,
                refreshToken,
                "Bearer",
                jwtProperties.accessTokenExpirySeconds(),
                new SocialLoginResponse.UserInfo(
                        accountResolution.user().getId(),
                        accountResolution.user().getEmail(),
                        profile.displayName(),
                        profile.avatarUrl(),
                        accountResolution.newUser()
                )
        );
    }

    public String issueState(SocialProvider provider) {
        return oAuthStateService.issueState(provider);
    }

    private AccountResolution resolveUser(SocialProvider provider, SocialProfile profile) {
        return socialAccountRepository
                .findByProviderAndProviderUserId(provider, profile.providerUserId())
                .map(existingSocial -> new AccountResolution(existingSocial.getUser(), false))
                .orElseGet(() -> createOrLinkUser(provider, profile));
    }

    private AccountResolution createOrLinkUser(SocialProvider provider, SocialProfile profile) {

        boolean[] isNewUser = new boolean[] { false };

        User user = userRepository
                .findByEmail(profile.email())
                .orElseGet(() -> {
                    isNewUser[0] = true;
                    return createUser(profile.email());
                });

        SocialAccount socialAccount = SocialAccount.builder()
                .user(user)
                .provider(provider)
                .providerUserId(profile.providerUserId())
                .providerEmail(profile.email())
                .providerDisplayName(profile.displayName())
                .providerAvatarUrl(profile.avatarUrl())
                .build();

        socialAccountRepository.save(socialAccount);

        return new AccountResolution(user, isNewUser[0]);
    }

    private User createUser(String email) {

        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                .build();

        user.addRole(Role.USER);

        return userRepository.save(user);
    }

    private record AccountResolution(User user, boolean newUser) {
    }
}
