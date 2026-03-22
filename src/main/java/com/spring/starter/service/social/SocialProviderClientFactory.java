package com.spring.starter.service.social;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.spring.starter.enums.SocialProvider;
import com.spring.starter.exception.AppException;
import com.spring.starter.exception.ErrorCode;

@Component
public class SocialProviderClientFactory {
    
    private final Map<SocialProvider, SocialProviderClient> clients;

    public SocialProviderClientFactory(List<SocialProviderClient> clientList) {
        this.clients = clientList.stream()
                .collect(Collectors.toMap(SocialProviderClient::provider, c -> c));
    }

    public SocialProviderClient getClient(SocialProvider provider) {
        return Optional.ofNullable(clients.get(provider))
                .orElseThrow(() -> new AppException(ErrorCode.OAUTH_PROVIDER_UNSUPPORTED));
    }
}
