package com.spring.starter.auth.oauth2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.spring.starter.auth.dto.GoogleTokenResponse;
import com.spring.starter.auth.dto.SocialLoginRequest;
import com.spring.starter.auth.dto.SocialProfile;
import com.spring.starter.auth.enums.SocialProvider;
import com.spring.starter.common.config.OAuth2Properties;
import com.spring.starter.common.exception.AppException;
import com.spring.starter.common.exception.ErrorCode;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Component
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GoogleOidcClient implements SocialProviderClient {

    private static final Logger logger = LoggerFactory.getLogger(GoogleOidcClient.class);

    JwtDecoder googleJwtDecoder;
    OAuth2Properties oAuth2Properties;
    RestClient restClient;

    public GoogleOidcClient(
            @Qualifier("googleJwtDecoder") JwtDecoder googleJwtDecoder,
            OAuth2Properties oAuth2Properties,
            RestClient restClient) {
        this.googleJwtDecoder = googleJwtDecoder;
        this.oAuth2Properties = oAuth2Properties;
        this.restClient = restClient;
    }

    @Override
    public SocialProvider provider() {
        return SocialProvider.GOOGLE;
    }

    @Override
    public SocialProfile authenticate(SocialLoginRequest request) {

        GoogleTokenResponse token = exchangeCode(request);

        Jwt jwt;
        try {
            jwt = googleJwtDecoder.decode(token.idToken());
        } catch (JwtException ex) {
            throw new AppException(ErrorCode.INVALID_TOKEN);
        }

        String providerUserId = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        boolean emailVerified = Boolean.TRUE.equals(jwt.getClaim("email_verified"));
        String displayName = jwt.getClaimAsString("name");
        String avatarUrl = jwt.getClaimAsString("picture");

        if (providerUserId == null || providerUserId.isBlank() || email == null || email.isBlank()) {
            throw new AppException(ErrorCode.INVALID_TOKEN);
        }

        if (!emailVerified) {
            throw new AppException(ErrorCode.EMAIL_NOT_VERIFIED);
        }

        return new SocialProfile(
                providerUserId,
                email,
                emailVerified,
                displayName,
                avatarUrl
        );
    }

    private GoogleTokenResponse exchangeCode(SocialLoginRequest request) {
        
        var providerProperties = oAuth2Properties
                .getProvider(provider().name().toLowerCase());

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("code", request.code());
        form.add("client_id", providerProperties.clientId());
        form.add("client_secret", providerProperties.clientSecret());
        form.add("redirect_uri", request.redirectUri());
        form.add("grant_type", "authorization_code");

        if (request.codeVerifier() != null && !request.codeVerifier().isBlank()) {
            form.add("code_verifier", request.codeVerifier());
        }

        try {
            GoogleTokenResponse response = restClient.post()
                    .uri(providerProperties.tokenUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(form)
                    .retrieve()
                    .body(GoogleTokenResponse.class);

            if (response == null || response.idToken() == null || response.idToken().isBlank()) {
                throw new AppException(ErrorCode.OAUTH_EXCHANGE_FAILED);
            }

            return response;
        } catch (RestClientResponseException ex) {
            logger.error("Google exchange failed: status={}, body={}",
                    ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new AppException(ErrorCode.OAUTH_EXCHANGE_FAILED);
        }
    }
}
