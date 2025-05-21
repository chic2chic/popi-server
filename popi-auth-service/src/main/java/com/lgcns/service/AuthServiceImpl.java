package com.lgcns.service;

import com.lgcns.domain.Member;
import com.lgcns.domain.OauthInfo;
import com.lgcns.domain.OauthProvider;
import com.lgcns.dto.AccessTokenDto;
import com.lgcns.dto.RefreshTokenDto;
import com.lgcns.dto.request.IdTokenRequest;
import com.lgcns.dto.response.SocialLoginResponse;
import com.lgcns.dto.response.TokenReissueResponse;
import com.lgcns.error.exception.CustomException;
import com.lgcns.exception.AuthErrorCode;
import com.lgcns.exception.MemberErrorCode;
import com.lgcns.repository.MemberRepository;
import com.lgcns.repository.RefreshTokenRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

    private final JwtTokenService jwtTokenService;
    private final IdTokenVerifier idTokenVerifier;
    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    public SocialLoginResponse socialLoginMember(OauthProvider provider, IdTokenRequest request) {
        OidcUser oidcUser = idTokenVerifier.getOidcUser(request.idToken(), provider);

        Optional<Member> optionalMember = findByOidcUser(oidcUser);
        Member member = optionalMember.orElseGet(() -> saveMember(oidcUser, provider));

        return getLoginResponse(member);
    }

    @Override
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

    @Override
    public void logoutMember(String memberId) {
        refreshTokenRepository
                .findById(Long.parseLong(memberId))
                .ifPresent(refreshTokenRepository::delete);
    }

    @Override
    public void withdrawalMember(String memberId) {
        refreshTokenRepository
                .findById(Long.parseLong(memberId))
                .ifPresent(refreshTokenRepository::delete);

        Member member =
                memberRepository
                        .findById(Long.parseLong(memberId))
                        .orElseThrow(() -> new CustomException(MemberErrorCode.MEMBER_NOT_FOUND));

        member.withdrawal();
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
