package com.spring.starter.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.spring.starter.common.dto.ApiResponse;
import com.spring.starter.user.dto.UpdateMyProfileRequest;
import com.spring.starter.user.dto.UserMeResponse;
import com.spring.starter.user.service.UserProfileService;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserController {

    UserProfileService userProfileService;

    @GetMapping("/me")
    ResponseEntity<ApiResponse<UserMeResponse>> me(Authentication authentication) {
        var result = userProfileService.getMyProfile(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PutMapping("/me")
    ResponseEntity<ApiResponse<UserMeResponse>> updateMe(
            Authentication authentication,
            @Valid @RequestBody UpdateMyProfileRequest request
    ) {
        var result = userProfileService.updateMyProfile(authentication.getName(), request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated", result));
    }
}
