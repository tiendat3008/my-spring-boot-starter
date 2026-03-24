package com.spring.starter.auth.entity;

import com.spring.starter.auth.enums.SocialProvider;
import com.spring.starter.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Entity
@Table(
        name = "social_accounts",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_provider_provider_user_id",
                    columnNames = {"provider", "provider_user_id"})
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SocialAccount extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    SocialProvider provider;

    @Column(name = "provider_user_id", nullable = false, length = 128)
    String providerUserId;

    @Column(name = "provider_email", length = 255)
    String providerEmail;

    @Column(name = "provider_display_name", length = 255)
    String providerDisplayName;

    @Column(name = "provider_avatar_url", length = 500)
    String providerAvatarUrl;
}
