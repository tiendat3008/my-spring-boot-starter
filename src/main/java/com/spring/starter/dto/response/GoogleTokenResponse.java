package com.spring.starter.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GoogleTokenResponse(
        
        @JsonProperty("access_token")
        String accessToken,

        @JsonProperty("expires_in")
        Long expiresIn,

        @JsonProperty("id_token")
        String idToken,

        @JsonProperty("token_type")
        String tokenType,

        String scope
) {
}
