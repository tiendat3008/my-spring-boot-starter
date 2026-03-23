package com.spring.starter.auth.service;

import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.spring.starter.auth.dto.AuthResponse;
import com.spring.starter.auth.dto.SocialLoginRequest;
import com.spring.starter.auth.dto.SocialProfile;
import com.spring.starter.auth.entity.SocialAccount;
import com.spring.starter.auth.entity.User;
import com.spring.starter.auth.enums.Role;
import com.spring.starter.auth.enums.SocialProvider;
import com.spring.starter.auth.oauth2.SocialProviderClient;
import com.spring.starter.auth.oauth2.SocialProviderClientFactory;
import com.spring.starter.auth.repository.SocialAccountRepository;
import com.spring.starter.auth.repository.UserRepository;
import com.spring.starter.common.exception.AppException;
import com.spring.starter.common.exception.ErrorCode;

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
    AuthService authService;  
    OAuthStateService oAuthStateService;
    SocialProviderClientFactory clientFactory;

    @Transactional
    public AuthResponse authenticate(SocialProvider provider, SocialLoginRequest request) {

        // Validate state to prevent CSRF attacks
        if (!oAuthStateService.consumeState(provider, request.state())) {
            throw new AppException(ErrorCode.OAUTH_STATE_INVALID);
        }

        // Strategy pattern to handle multiple providers in a clean way
        SocialProviderClient client = clientFactory.getClient(provider);

        // Delegate the authentication and user info fetching to the provider-specific client
        SocialProfile profile = client.authenticate(request);

        // Business logic
        User user = resolveUser(provider, profile);

        return authService.generateTokens(user);
    }

    public String issueState(SocialProvider provider) {
        return oAuthStateService.issueState(provider);
    }

    private User resolveUser(SocialProvider provider, SocialProfile profile) {
        return socialAccountRepository
            .findByProviderAndProviderUserId(provider, profile.providerUserId())
            .map(SocialAccount::getUser)
            .orElseGet(() -> createOrLinkUser(provider, profile));
    }

    private User createOrLinkUser(SocialProvider provider, SocialProfile profile) {
        return userRepository.findByEmail(profile.email())
            // If email already exists, link the social account to the existing user
            .map(existingUser -> {
                socialAccountRepository.save(buildSocialAccount(existingUser, provider, profile));
                return existingUser;
            })
            // If email does not exist, create a new user and link the social account
            // try-catch to handle potential race condition
            .orElseGet(() -> {
                try {
                    User newUser = createUser(profile.email());
                    socialAccountRepository.save(buildSocialAccount(newUser, provider, profile));
                    return newUser;
                } catch (DataIntegrityViolationException e) {
                    User existingUser = userRepository.findByEmail(profile.email())
                        .orElseThrow(() -> new IllegalStateException("Concurrent user creation conflict", e));
                    socialAccountRepository.save(buildSocialAccount(existingUser, provider, profile));
                    return existingUser;
                }
            });
    }

    private SocialAccount buildSocialAccount(User user, SocialProvider provider, SocialProfile profile) {
        return SocialAccount.builder()
                .user(user)
                .provider(provider)
                .providerUserId(profile.providerUserId())
                .providerEmail(profile.email())
                .providerDisplayName(profile.displayName())
                .providerAvatarUrl(profile.avatarUrl())
                .build();
    }

    private User createUser(String email) {
        // Social login users have no password — store random hash as placeholder
        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                .build();
        user.addRole(Role.USER);
        return userRepository.save(user);
    }
}
