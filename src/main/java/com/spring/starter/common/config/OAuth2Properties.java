package com.spring.starter.common.config;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

@ConfigurationProperties(prefix = "app.oauth2")
public record OAuth2Properties(@NotEmpty Map<String, @Valid ProviderProperties> providers) {

    public ProviderProperties getProvider(String provider) {
        ProviderProperties properties = providers.get(provider);

        if (properties == null) {
            throw new IllegalArgumentException("Unsupported provider: " + provider);
        }

        return properties;
    }

    public record ProviderProperties(String clientId, String clientSecret, String tokenUri, String userInfoUri) {}
}
