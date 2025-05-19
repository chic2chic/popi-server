package com.lgcns.service;

import com.lgcns.domain.OauthProvider;
import com.lgcns.error.exception.CustomException;
import com.lgcns.exception.AuthErrorCode;
import com.lgcns.infra.oidc.OidcProperties;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IdTokenVerifier {

    private final OidcProperties oidcProperties;
    private final Map<OauthProvider, JwtDecoder> decoders =
            Map.of(
                    OauthProvider.GOOGLE, buildDecoder(OauthProvider.GOOGLE.getJwkSetUrl()),
                    OauthProvider.KAKAO, buildDecoder(OauthProvider.KAKAO.getJwkSetUrl()));

    private JwtDecoder buildDecoder(String jwkSetUrl) {
        return NimbusJwtDecoder.withJwkSetUri(jwkSetUrl).build();
    }

    public OidcUser getOidcUser(String idToken, OauthProvider provider) {
        Jwt jwt = getJwt(idToken, provider);
        OidcIdToken oidcIdToken = getOidcIdToken(jwt);

        String audience =
                switch (provider) {
                    case KAKAO -> oidcProperties.kakao().audience();
                    case GOOGLE -> oidcProperties.google().audience();
                };

        validateAudience(oidcIdToken, audience);
        validateIssuer(oidcIdToken, provider.getIssuer());
        validateExpiresAt(oidcIdToken);

        return new DefaultOidcUser(null, oidcIdToken);
    }

    private Jwt getJwt(String idToken, OauthProvider provider) {
        return decoders.get(provider).decode(idToken);
    }

    private OidcIdToken getOidcIdToken(Jwt jwt) {
        return new OidcIdToken(
                jwt.getTokenValue(), jwt.getIssuedAt(), jwt.getExpiresAt(), jwt.getClaims());
    }

    private void validateAudience(OidcIdToken oidcIdToken, String audience) {
        String idTokenAudience = oidcIdToken.getAudience().get(0);

        if (idTokenAudience == null || !idTokenAudience.equals(audience)) {
            throw new CustomException(AuthErrorCode.ID_TOKEN_VERIFICATION_FAILED);
        }
    }

    private void validateIssuer(OidcIdToken oidcIdToken, String issuer) {
        String idTokenIssuer = oidcIdToken.getIssuer().toString();

        if (idTokenIssuer == null || !idTokenIssuer.equals(issuer)) {
            throw new CustomException(AuthErrorCode.ID_TOKEN_VERIFICATION_FAILED);
        }
    }

    private void validateExpiresAt(OidcIdToken oidcIdToken) {
        Instant expiresAt = oidcIdToken.getExpiresAt();

        if (expiresAt == null || expiresAt.isBefore(Instant.now())) {
            throw new CustomException(AuthErrorCode.ID_TOKEN_VERIFICATION_FAILED);
        }
    }
}
