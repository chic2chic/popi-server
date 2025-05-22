package com.lgcns.service;

import com.lgcns.domain.OauthProvider;
import com.lgcns.dto.request.IdTokenRequest;
import com.lgcns.dto.request.RegisterTokenRequest;
import com.lgcns.dto.response.SocialLoginResponse;
import com.lgcns.dto.response.TokenReissueResponse;

public interface AuthService {
    SocialLoginResponse socialLoginMember(OauthProvider provider, IdTokenRequest request);

    SocialLoginResponse registerMember(String registerTokenValue, RegisterTokenRequest request);

    TokenReissueResponse reissueToken(String refreshTokenValue);

    void logoutMember(String memberId);

    void withdrawalMember(String memberId);
}
