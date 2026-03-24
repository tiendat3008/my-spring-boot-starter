package com.spring.starter.user.mapper;

import java.util.List;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.spring.starter.auth.entity.User;
import com.spring.starter.user.dto.UpdateMyProfileRequest;
import com.spring.starter.user.dto.UserMeResponse;
import com.spring.starter.user.entity.UserProfile;

@Mapper
public interface UserProfileMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "status", source = "user.status")
    @Mapping(target = "roles", source = "roles")
    @Mapping(target = "firstName", source = "profile.firstName")
    @Mapping(target = "lastName", source = "profile.lastName")
    @Mapping(target = "avatarUrl", source = "profile.avatarUrl")
    @Mapping(target = "bio", source = "profile.bio")
    @Mapping(target = "preferredLanguage", source = "profile.preferredLanguage")
    @Mapping(target = "preferredCurrency", source = "profile.preferredCurrency")
    UserMeResponse toUserMeResponse(User user, UserProfile profile, List<String> roles);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "avatarUrl", ignore = true)
    @Mapping(target = "avatarObjectKey", ignore = true)
    void updateProfile(@MappingTarget UserProfile profile, UpdateMyProfileRequest request);
}
