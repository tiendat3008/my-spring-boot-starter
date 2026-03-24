package com.spring.starter.user.entity;

import com.spring.starter.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "user_profiles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserProfile extends BaseEntity {

    /** References auth_users.id — no JPA FK. */
    @Column(name = "user_id", nullable = false, unique = true)
    Long userId;

    @Column(name = "first_name")
    String firstName;

    @Column(name = "last_name")
    String lastName;

    @Column(name = "avatar_url", length = 512)
    String avatarUrl;

    @Column(name = "avatar_object_key", length = 512)
    String avatarObjectKey;

    @Column(name = "bio", columnDefinition = "TEXT")
    String bio;

    @Column(name = "preferred_language", length = 10)
    @Builder.Default
    String preferredLanguage = "vi";

    @Column(name = "preferred_currency", length = 10)
    @Builder.Default
    String preferredCurrency = "VND";
}
