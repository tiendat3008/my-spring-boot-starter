package com.spring.starter.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.spring.starter.auth.entity.User;
import com.spring.starter.auth.entity.UserRole;
import com.spring.starter.auth.enums.Role;
import com.spring.starter.auth.repository.UserRepository;
import com.spring.starter.user.dto.UpdateMyProfileRequest;
import com.spring.starter.user.dto.UserMeResponse;
import com.spring.starter.user.entity.UserProfile;
import com.spring.starter.user.mapper.UserProfileMapper;
import com.spring.starter.user.repository.UserProfileRepository;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    UserProfileRepository userProfileRepository;

    @Mock
    UserProfileMapper userProfileMapper;

    @InjectMocks
    UserProfileService userProfileService;

    @Test
    void getMyProfile_shouldCreateProfileIfMissing() {
        var user = buildUser();
        var persistedProfile = UserProfile.builder().userId(1L).preferredLanguage("vi").preferredCurrency("VND").build();
        var response = new UserMeResponse(1L, "user@example.com", user.getStatus(), List.of("USER"), null, null, null, null, "vi", "VND");

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(userProfileRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(userProfileRepository.save(org.mockito.ArgumentMatchers.any(UserProfile.class))).thenReturn(persistedProfile);
        when(userProfileMapper.toUserMeResponse(user, persistedProfile, List.of("USER"))).thenReturn(response);

        var actual = userProfileService.getMyProfile("user@example.com");

        assertThat(actual).isEqualTo(response);
        verify(userProfileRepository).save(org.mockito.ArgumentMatchers.any(UserProfile.class));
    }

    @Test
    void updateMyProfile_shouldSaveAndReturnMappedResult() {
        var user = buildUser();
        var profile = UserProfile.builder().userId(1L).preferredLanguage("vi").preferredCurrency("VND").build();
        var request = new UpdateMyProfileRequest("Dat", "Tran", null, "Bio", "en", "USD");
        var response = new UserMeResponse(1L, "user@example.com", user.getStatus(), List.of("USER"), "Dat", "Tran", null, "Bio", "en", "USD");

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(userProfileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));
        when(userProfileRepository.save(profile)).thenReturn(profile);
        when(userProfileMapper.toUserMeResponse(user, profile, List.of("USER"))).thenReturn(response);

        var actual = userProfileService.updateMyProfile("user@example.com", request);

        assertThat(actual).isEqualTo(response);
        verify(userProfileMapper).updateProfile(profile, request);
        verify(userProfileRepository).save(profile);
    }

    private User buildUser() {
        User user = User.builder()
            .email("user@example.com")
            .roles(Set.of())
            .build();

        user.setId(1L);

        user.setRoles(Set.of(UserRole.builder().user(user).role(Role.USER).build()));
        return user;
    }
}
