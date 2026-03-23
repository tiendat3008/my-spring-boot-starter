package com.spring.starter.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.spring.starter.auth.entity.SocialAccount;
import com.spring.starter.auth.enums.SocialProvider;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {
    
    Optional<SocialAccount> findByProviderAndProviderUserId(SocialProvider provider, String providerUserId);
}
