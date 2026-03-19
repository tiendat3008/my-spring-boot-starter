package com.spring.starter.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.spring.starter.entity.SocialAccount;
import com.spring.starter.enums.SocialProvider;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {
    
    Optional<SocialAccount> findByProviderAndProviderUserId(SocialProvider provider, String providerUserId);
}
