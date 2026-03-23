package com.spring.starter.user.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.spring.starter.auth.repository.UserRepository;
import com.spring.starter.common.exception.AppException;
import com.spring.starter.common.exception.ErrorCode;
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

    UserRepository userRepository;
    UserProfileRepository userProfileRepository;
    UserProfileMapper userProfileMapper;

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

    private List<String> extractRoles(com.spring.starter.auth.entity.User user) {
        return user.getRoles().stream()
                .map(ur -> ur.getRole().name())
                .toList();
    }
}
