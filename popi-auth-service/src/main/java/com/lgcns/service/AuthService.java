package com.lgcns.service;

import com.lgcns.domain.Member;
import com.lgcns.domain.OauthInfo;
import com.lgcns.domain.OauthProvider;
import com.lgcns.dto.request.AuthCodeRequest;
import com.lgcns.dto.response.SocialLoginResponse;
import com.lgcns.repository.MemberRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Service
@RequiredArgsConstructor
public class AuthService {

    private final KakaoService kakaoService;
    private final GoogleService googleService;
    private final JwtTokenService jwtTokenService;
    private final IdTokenVerifier idTokenVerifier;
    private final MemberRepository memberRepository;

    public SocialLoginResponse socialLoginMember(AuthCodeRequest request, OauthProvider provider) {
        String idToken = getIdToken(request.code(), provider);

        OidcUser oidcUser = idTokenVerifier.getOidcUser(idToken, provider);

        Optional<Member> optionalMember = findByOidcUser(oidcUser);
        Member member = optionalMember.orElseGet(() -> saveMember(oidcUser, provider));

        return getLoginResponse(member);
    }

    private SocialLoginResponse getLoginResponse(Member member) {
        String accessToken = jwtTokenService.createAccessToken(member.getId(), member.getRole());
        String refreshToken = jwtTokenService.createRefreshToken(member.getId());
        return SocialLoginResponse.of(accessToken, refreshToken);
    }

    private String getIdToken(String code, OauthProvider provider) {
        return switch (provider) {
            case GOOGLE -> googleService.getIdToken(code);
            case KAKAO -> kakaoService.getIdToken(code);
        };
    }

    private Optional<Member> findByOidcUser(OidcUser oidcUser) {
        OauthInfo oauthInfo = extractOauthInfo(oidcUser);
        return memberRepository.findByOauthInfo(oauthInfo);
    }

    private Member saveMember(OidcUser oidcUser, OauthProvider provider) {
        OauthInfo oauthInfo = extractOauthInfo(oidcUser);
        String nickname = getDisplayName(oidcUser, provider);

        Member member = Member.createMember(nickname, oauthInfo);
        return memberRepository.save(member);
    }

    private OauthInfo extractOauthInfo(OidcUser oidcUser) {
        return OauthInfo.createOauthInfo(oidcUser.getSubject(), oidcUser.getIssuer().toString());
    }

    private String getDisplayName(OidcUser oidcUser, OauthProvider provider) {
        return switch (provider) {
            case GOOGLE -> (String) oidcUser.getClaims().get("name");
            case KAKAO -> (String) oidcUser.getClaims().get("nickname");
        };
    }
}
