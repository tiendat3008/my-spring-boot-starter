package com.spring.starter.entity;

import java.util.HashSet;
import java.util.Set;

import com.spring.starter.enums.Role;
import com.spring.starter.enums.UserStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class User extends BaseEntity {
    
    @Column(unique = true)
    String email;

    String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    UserStatus status = UserStatus.ACTIVE;

    @OneToMany(
        mappedBy = "user",
        fetch = FetchType.LAZY,
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    @Builder.Default
    Set<UserRole> roles = new HashSet<>();

    public void addRole(Role role) {
        var userRole = UserRole.builder().user(this).role(role).build();
        roles.add(userRole);
    }

    public boolean hasRole(Role role) {
        return roles.stream().anyMatch(ur -> ur.getRole() == role);
    }
}
