package com.spring.starter.user.service;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.spring.starter.auth.entity.User;
import com.spring.starter.auth.repository.UserRepository;
import com.spring.starter.common.exception.AppException;
import com.spring.starter.common.exception.ErrorCode;
import com.spring.starter.infrastructure.storage.StorageService;
import com.spring.starter.user.dto.UpdateMyProfileRequest;
import com.spring.starter.user.dto.UserMeResponse;
import com.spring.starter.user.entity.UserProfile;
import com.spring.starter.user.mapper.UserProfileMapper;
import com.spring.starter.user.repository.UserProfileRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserProfileService {

    private static final Logger logger = LoggerFactory.getLogger(UserProfileService.class);
    private static final long MAX_AVATAR_SIZE_BYTES = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_AVATAR_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif");

    UserRepository userRepository;
    UserProfileRepository userProfileRepository;
    UserProfileMapper userProfileMapper;
    StorageService storageService;

    @Transactional(readOnly = true)
    public UserMeResponse getMyProfile(String email) {
        var user = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        var profile = userProfileRepository
                .findByUserId(user.getId())
                .orElseGet(() -> userProfileRepository.save(UserProfile.builder().userId(user.getId()).build()));

        return userProfileMapper.toUserMeResponse(user, profile, extractRoles(user));
    }

    @Transactional
    public UserMeResponse updateMyProfile(String email, UpdateMyProfileRequest request) {
        var user = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        var profile = userProfileRepository
                .findByUserId(user.getId())
                .orElseGet(() -> UserProfile.builder().userId(user.getId()).build());

        userProfileMapper.updateProfile(profile, request);
        profile = userProfileRepository.save(profile);

        return userProfileMapper.toUserMeResponse(user, profile, extractRoles(user));
    }

    @Transactional
    public UserMeResponse uploadAvatar(String email, MultipartFile file) {
        validateAvatarFile(file);

        var user = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        var profile = userProfileRepository
                .findByUserId(user.getId())
                .orElseGet(() -> UserProfile.builder().userId(user.getId()).build());

        var oldAvatarObjectKey = profile.getAvatarObjectKey();
        var objectKey = buildAvatarObjectKey(user.getId(), file.getOriginalFilename());

        String avatarUrl;
        try (var inputStream = file.getInputStream()) {
            avatarUrl = storageService.upload(objectKey, inputStream, file.getContentType(), file.getSize());
        } catch (IOException ex) {
            logger.error("Failed to read avatar file from multipart request", ex);
            throw new AppException(ErrorCode.FILE_UPLOAD_FAILED);
        }

        profile.setAvatarUrl(avatarUrl);
        profile.setAvatarObjectKey(objectKey);
        profile = userProfileRepository.save(profile);

        cleanupOldAvatar(oldAvatarObjectKey, objectKey);

        return userProfileMapper.toUserMeResponse(user, profile, extractRoles(user));
    }

    private void validateAvatarFile(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > MAX_AVATAR_SIZE_BYTES) {
            throw new AppException(ErrorCode.INVALID_FILE_SIZE);
        }

        var contentType = file.getContentType();
        if (contentType == null || !ALLOWED_AVATAR_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new AppException(ErrorCode.INVALID_CONTENT_TYPE);
        }
    }

    private String buildAvatarObjectKey(Long userId, String originalFilename) {
        var safeFileName = sanitizeFileName(originalFilename);
        return "avatars/%d/%d_%s".formatted(userId, System.currentTimeMillis(), safeFileName);
    }

    private String sanitizeFileName(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "avatar";
        }
        var sanitized = originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
        return sanitized.isBlank() ? "avatar" : sanitized;
    }

    private void cleanupOldAvatar(String oldAvatarObjectKey, String newAvatarObjectKey) {
        if (oldAvatarObjectKey == null || oldAvatarObjectKey.isBlank() || oldAvatarObjectKey.equals(newAvatarObjectKey)) {
            return;
        }

        try {
            storageService.delete(oldAvatarObjectKey);
        } catch (AppException ex) {
            logger.warn("Failed to delete old avatar object: {}", oldAvatarObjectKey, ex);
        }
    }

    private List<String> extractRoles(User user) {
        return user.getRoles().stream()
                .map(ur -> ur.getRole().name())
                .toList();
    }
}
