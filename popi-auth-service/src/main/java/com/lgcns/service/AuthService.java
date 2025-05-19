package com.lgcns.service;

import com.lgcns.domain.Member;
import com.lgcns.domain.OauthInfo;
import com.lgcns.domain.OauthProvider;
import com.lgcns.dto.AccessTokenDto;
import com.lgcns.dto.RefreshTokenDto;
import com.lgcns.dto.response.SocialLoginResponse;
import com.lgcns.dto.response.TokenReissueResponse;
import com.lgcns.error.exception.CustomException;
import com.lgcns.exception.AuthErrorCode;
import com.lgcns.exception.MemberErrorCode;
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

    private final JwtTokenService jwtTokenService;
    private final IdTokenVerifier idTokenVerifier;
    private final MemberRepository memberRepository;

    public SocialLoginResponse socialLoginMember(OauthProvider provider) {
        String idToken = "";

        OidcUser oidcUser = idTokenVerifier.getOidcUser(idToken, provider);

        Optional<Member> optionalMember = findByOidcUser(oidcUser);
        Member member = optionalMember.orElseGet(() -> saveMember(oidcUser, provider));

        return getLoginResponse(member);
    }

    public TokenReissueResponse reissueToken(String refreshTokenValue) {
        RefreshTokenDto oldRefreshTokenDto =
                jwtTokenService.validateRefreshToken(refreshTokenValue);

        if (oldRefreshTokenDto == null) {
            throw new CustomException(AuthErrorCode.EXPIRED_REFRESH_TOKEN);
        }

        RefreshTokenDto newRefreshTokenDto =
                jwtTokenService.reissueRefreshToken(oldRefreshTokenDto);
        AccessTokenDto newAccessTokenDto =
                jwtTokenService.reissueAccessToken(getMember(newRefreshTokenDto));

        return TokenReissueResponse.of(
                newAccessTokenDto.accessTokenValue(), newRefreshTokenDto.refreshTokenValue());
    }

    private SocialLoginResponse getLoginResponse(Member member) {
        String accessToken = jwtTokenService.createAccessToken(member.getId(), member.getRole());
        String refreshToken = jwtTokenService.createRefreshToken(member.getId());
        return SocialLoginResponse.of(accessToken, refreshToken);
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

    private Member getMember(RefreshTokenDto refreshTokenDto) {
        return memberRepository
                .findById(refreshTokenDto.memberId())
                .orElseThrow(() -> new CustomException(MemberErrorCode.MEMBER_NOT_FOUND));
    }
}
