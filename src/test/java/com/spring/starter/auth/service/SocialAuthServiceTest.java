package com.spring.starter.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
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

import com.spring.starter.auth.dto.AuthResponse;
import com.spring.starter.auth.dto.LinkedSocialAccountResponse;
import com.spring.starter.auth.dto.SocialLoginRequest;
import com.spring.starter.auth.entity.SocialAccount;
import com.spring.starter.auth.entity.User;
import com.spring.starter.auth.enums.SocialProvider;
import com.spring.starter.auth.oauth2.SocialProviderClient;
import com.spring.starter.auth.oauth2.SocialProviderClientFactory;
import com.spring.starter.auth.repository.SocialAccountRepository;
import com.spring.starter.auth.repository.UserRepository;
import com.spring.starter.common.exception.AppException;
import com.spring.starter.common.exception.ErrorCode;

import jakarta.servlet.http.HttpServletRequest;

@ExtendWith(MockitoExtension.class)
class SocialAuthServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    SocialAccountRepository socialAccountRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    AuthService authService;

    @Mock
    OAuthStateService oAuthStateService;

    @Mock
    SocialProviderClientFactory clientFactory;

    @Mock
    SocialProviderClient socialProviderClient;

    @InjectMocks
    SocialAuthService socialAuthService;

        @Test
        void authenticate_shouldForwardHttpRequest_whenStateValid() {
        var request = new SocialLoginRequest("code", "state", null, "http://localhost/callback");
        HttpServletRequest httpRequest = org.mockito.Mockito.mock(HttpServletRequest.class);
        var socialProfile = new com.spring.starter.auth.dto.SocialProfile(
            "google-123",
            "user@example.com",
            true,
            "Social User",
            "http://avatar"
        );
        var user = User.builder().email("user@example.com").build();
        var authResponse = new AuthResponse("access", "refresh", "Bearer", 3600L);

        when(oAuthStateService.consumeState(SocialProvider.GOOGLE, "state")).thenReturn(true);
        when(clientFactory.getClient(SocialProvider.GOOGLE)).thenReturn(socialProviderClient);
        when(socialProviderClient.authenticate(request)).thenReturn(socialProfile);
        when(socialAccountRepository.findByProviderAndProviderUserId(SocialProvider.GOOGLE, "google-123"))
            .thenReturn(Optional.empty());
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(authService.generateTokens(user, httpRequest)).thenReturn(authResponse);

        var result = socialAuthService.authenticate(SocialProvider.GOOGLE, request, httpRequest);

        assertThat(result).isEqualTo(authResponse);
        verify(authService).generateTokens(eq(user), eq(httpRequest));
        }

    @Test
    void listLinkedAccounts_shouldReturnMappedAccounts() {
        User user = User.builder().email("user@example.com").build();
        user.setId(1L);

        SocialAccount account = SocialAccount.builder()
                .user(user)
                .provider(SocialProvider.GOOGLE)
                .providerEmail("social@example.com")
                .providerDisplayName("Social User")
                .providerAvatarUrl("http://avatar")
                .build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(socialAccountRepository.findAllByUserId(1L)).thenReturn(List.of(account));

        List<LinkedSocialAccountResponse> result = socialAuthService.listLinkedAccounts("user@example.com");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().provider()).isEqualTo("GOOGLE");
    }

    @Test
    void unlinkAccount_shouldThrow_whenSocialAccountNotFound() {
        User user = User.builder().email("user@example.com").build();
        user.setId(1L);

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(socialAccountRepository.findByUserIdAndProvider(1L, SocialProvider.GITHUB)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> socialAuthService.unlinkAccount("user@example.com", SocialProvider.GITHUB))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SOCIAL_ACCOUNT_NOT_FOUND);
    }

    @Test
    void linkAccount_shouldThrow_whenProviderAlreadyLinked() {
        User user = User.builder().email("user@example.com").build();
        user.setId(1L);

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(socialAccountRepository.findByUserIdAndProvider(1L, SocialProvider.GOOGLE))
                .thenReturn(Optional.of(SocialAccount.builder().user(user).provider(SocialProvider.GOOGLE).providerUserId("id").build()));

        var request = new SocialLoginRequest("code", "state", null, "http://localhost/callback");

        assertThatThrownBy(() -> socialAuthService.linkAccount("user@example.com", SocialProvider.GOOGLE, request))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SOCIAL_ACCOUNT_ALREADY_LINKED);
    }
}
