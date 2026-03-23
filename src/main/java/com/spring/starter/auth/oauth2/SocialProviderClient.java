package com.spring.starter.auth.oauth2;

import com.spring.starter.auth.dto.SocialLoginRequest;
import com.spring.starter.auth.dto.SocialProfile;
import com.spring.starter.auth.enums.SocialProvider;

public interface SocialProviderClient {

    SocialProvider provider();

    SocialProfile authenticate(SocialLoginRequest request);
}
