package com.lgcns.service;

import com.lgcns.domain.Member;
import com.lgcns.domain.MemberStatus;
import com.lgcns.domain.OauthInfo;
import com.lgcns.domain.OauthProvider;
import com.lgcns.dto.AccessTokenDto;
import com.lgcns.dto.RefreshTokenDto;
import com.lgcns.dto.RegisterTokenDto;
import com.lgcns.dto.request.IdTokenRequest;
import com.lgcns.dto.request.RegisterTokenRequest;
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

        if (optionalMember.isPresent()) {
            Member member = optionalMember.get();

            if (member.getStatus() == MemberStatus.DELETED) {
                member.reEnroll();
            }

            return getLoginResponse(member);
        }

        String registerToken =
                jwtTokenService.createRegisterToken(
                        oidcUser.getSubject(), oidcUser.getIssuer().toString());
        return SocialLoginResponse.notRegistered(registerToken);
    }

    @Override
    public SocialLoginResponse registerMember(
            String registerTokenValue, RegisterTokenRequest request) {
        RegisterTokenDto registerTokenDto =
                jwtTokenService.validateRegisterToken(registerTokenValue);

        if (registerTokenDto == null) {
            throw new CustomException(AuthErrorCode.EXPIRED_REGISTER_TOKEN);
        }

        if (memberRepository.existsByOauthInfo(
                OauthInfo.createOauthInfo(
                        registerTokenDto.oauthId(), registerTokenDto.oauthProvider()))) {
            throw new CustomException(AuthErrorCode.ALREADY_REGISTERED);
        }

        Member member =
                Member.createMember(
                        OauthInfo.createOauthInfo(
                                registerTokenDto.oauthId(), registerTokenDto.oauthProvider()),
                        request.nickname(),
                        request.gender(),
                        request.age());
        memberRepository.save(member);

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
        return SocialLoginResponse.registered(accessToken, refreshToken);
    }

    private Optional<Member> findByOidcUser(OidcUser oidcUser) {
        OauthInfo oauthInfo = extractOauthInfo(oidcUser);
        return memberRepository.findByOauthInfo(oauthInfo);
    }

    private OauthInfo extractOauthInfo(OidcUser oidcUser) {
        return OauthInfo.createOauthInfo(oidcUser.getSubject(), oidcUser.getIssuer().toString());
    }

    private Member getMember(RefreshTokenDto refreshTokenDto) {
        return memberRepository
                .findById(refreshTokenDto.memberId())
                .orElseThrow(() -> new CustomException(MemberErrorCode.MEMBER_NOT_FOUND));
    }
}
