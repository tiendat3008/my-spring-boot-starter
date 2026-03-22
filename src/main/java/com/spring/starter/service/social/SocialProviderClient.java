package com.spring.starter.service.social;

import com.spring.starter.dto.request.SocialLoginRequest;
import com.spring.starter.enums.SocialProvider;

public interface SocialProviderClient {

    SocialProvider provider();

    SocialProfile authenticate(SocialLoginRequest request);
}
