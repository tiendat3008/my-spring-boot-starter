package com.spring.starter.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
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
import org.springframework.mock.web.MockMultipartFile;

import com.spring.starter.auth.entity.User;
import com.spring.starter.auth.entity.UserRole;
import com.spring.starter.auth.enums.Role;
import com.spring.starter.auth.repository.UserRepository;
import com.spring.starter.common.exception.AppException;
import com.spring.starter.common.exception.ErrorCode;
import com.spring.starter.infrastructure.storage.StorageService;
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

    @Mock
    StorageService storageService;

    @InjectMocks
    UserProfileService userProfileService;

    @Test
    void getMyProfile_shouldCreateProfileIfMissing() {
        var user = buildUser();
        var persistedProfile = UserProfile.builder()
                .userId(1L)
                .preferredLanguage("vi")
                .preferredCurrency("VND")
                .build();
        var response = new UserMeResponse(
                1L, "user@example.com", user.getStatus(), List.of("USER"), null, null, null, null, "vi", "VND");

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(userProfileRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(userProfileRepository.save(org.mockito.ArgumentMatchers.any(UserProfile.class)))
                .thenReturn(persistedProfile);
        when(userProfileMapper.toUserMeResponse(user, persistedProfile, List.of("USER")))
                .thenReturn(response);

        var actual = userProfileService.getMyProfile("user@example.com");

        assertThat(actual).isEqualTo(response);
        verify(userProfileRepository).save(org.mockito.ArgumentMatchers.any(UserProfile.class));
    }

    @Test
    void updateMyProfile_shouldSaveAndReturnMappedResult() {
        var user = buildUser();
        var profile = UserProfile.builder()
                .userId(1L)
                .preferredLanguage("vi")
                .preferredCurrency("VND")
                .build();
        var request = new UpdateMyProfileRequest("Dat", "Tran", "Bio", "en", "USD");
        var response = new UserMeResponse(
                1L, "user@example.com", user.getStatus(), List.of("USER"), "Dat", "Tran", null, "Bio", "en", "USD");

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(userProfileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));
        when(userProfileRepository.save(profile)).thenReturn(profile);
        when(userProfileMapper.toUserMeResponse(user, profile, List.of("USER"))).thenReturn(response);

        var actual = userProfileService.updateMyProfile("user@example.com", request);

        assertThat(actual).isEqualTo(response);
        verify(userProfileMapper).updateProfile(profile, request);
        verify(userProfileRepository).save(profile);
    }

    @Test
    void uploadAvatar_shouldUploadAndReturnMappedResult() {
        var user = buildUser();
        var profile = UserProfile.builder()
                .userId(1L)
                .preferredLanguage("vi")
                .preferredCurrency("VND")
                .build();
        var file = new MockMultipartFile("file", "avatar.png", "image/png", "avatar-bytes".getBytes());
        var response = new UserMeResponse(
                1L,
                "user@example.com",
                user.getStatus(),
                List.of("USER"),
                null,
                null,
                "http://localhost:9000/hdcamp/avatars/1/avatar.png",
                null,
                "vi",
                "VND");

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(userProfileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));
        when(storageService.upload(anyString(), any(), anyString(), anyLong()))
                .thenReturn("http://localhost:9000/hdcamp/avatars/1/avatar.png");
        when(userProfileRepository.save(profile)).thenReturn(profile);
        when(userProfileMapper.toUserMeResponse(user, profile, List.of("USER"))).thenReturn(response);

        var actual = userProfileService.uploadAvatar("user@example.com", file);

        assertThat(actual).isEqualTo(response);
        assertThat(profile.getAvatarUrl()).isEqualTo("http://localhost:9000/hdcamp/avatars/1/avatar.png");
        assertThat(profile.getAvatarObjectKey()).startsWith("avatars/1/");
        verify(storageService).upload(anyString(), any(), anyString(), anyLong());
        verify(userProfileRepository).save(profile);
    }

    @Test
    void uploadAvatar_shouldThrowWhenContentTypeInvalid() {
        var file = new MockMultipartFile("file", "avatar.txt", "text/plain", "avatar".getBytes());

        assertThatThrownBy(() -> userProfileService.uploadAvatar("user@example.com", file))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_CONTENT_TYPE);

        verify(storageService, never()).upload(anyString(), any(), anyString(), anyLong());
    }

    @Test
    void uploadAvatar_shouldThrowWhenFileTooLarge() {
        var oversized = new byte[5 * 1024 * 1024 + 1];
        var file = new MockMultipartFile("file", "avatar.png", "image/png", oversized);

        assertThatThrownBy(() -> userProfileService.uploadAvatar("user@example.com", file))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_FILE_SIZE);

        verify(storageService, never()).upload(anyString(), any(), anyString(), anyLong());
    }

    @Test
    void uploadAvatar_shouldThrowWhenStorageUploadFails() {
        var user = buildUser();
        var profile = UserProfile.builder().userId(1L).build();
        var file = new MockMultipartFile("file", "avatar.png", "image/png", "avatar".getBytes());

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(userProfileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));
        when(storageService.upload(anyString(), any(), anyString(), anyLong()))
                .thenThrow(new AppException(ErrorCode.FILE_UPLOAD_FAILED));

        assertThatThrownBy(() -> userProfileService.uploadAvatar("user@example.com", file))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.FILE_UPLOAD_FAILED);
    }

    @Test
    void uploadAvatar_shouldDeleteOldAvatarWhenReplacing() {
        var user = buildUser();
        var profile = UserProfile.builder()
                .userId(1L)
                .avatarObjectKey("avatars/1/old_avatar.png")
                .build();
        var file = new MockMultipartFile("file", "avatar-new.png", "image/png", "avatar".getBytes());
        var response = new UserMeResponse(
                1L,
                "user@example.com",
                user.getStatus(),
                List.of("USER"),
                null,
                null,
                "http://localhost/new",
                null,
                "vi",
                "VND");

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(userProfileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));
        when(storageService.upload(anyString(), any(), anyString(), anyLong())).thenReturn("http://localhost/new");
        when(userProfileRepository.save(profile)).thenReturn(profile);
        when(userProfileMapper.toUserMeResponse(user, profile, List.of("USER"))).thenReturn(response);

        userProfileService.uploadAvatar("user@example.com", file);

        verify(storageService).delete("avatars/1/old_avatar.png");
    }

    private User buildUser() {
        User user = User.builder().email("user@example.com").roles(Set.of()).build();

        user.setId(1L);

        user.setRoles(Set.of(UserRole.builder().user(user).role(Role.USER).build()));
        return user;
    }
}
