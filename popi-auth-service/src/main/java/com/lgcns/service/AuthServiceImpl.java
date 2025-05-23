package com.lgcns.service;

import com.lgcns.client.MemberServiceClient;
import com.lgcns.domain.OauthProvider;
import com.lgcns.dto.AccessTokenDto;
import com.lgcns.dto.RefreshTokenDto;
import com.lgcns.dto.RegisterTokenDto;
import com.lgcns.dto.request.IdTokenRequest;
import com.lgcns.dto.request.MemberInternalRegisterRequest;
import com.lgcns.dto.request.MemberOauthInfoRequest;
import com.lgcns.dto.request.MemberRegisterRequest;
import com.lgcns.dto.response.MemberInternalInfoResponse;
import com.lgcns.dto.response.MemberInternalRegisterResponse;
import com.lgcns.dto.response.SocialLoginResponse;
import com.lgcns.dto.response.TokenReissueResponse;
import com.lgcns.enums.MemberRole;
import com.lgcns.error.exception.CustomException;
import com.lgcns.exception.AuthErrorCode;
import com.lgcns.repository.RefreshTokenRepository;
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
    private final MemberServiceClient memberServiceClient;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    public SocialLoginResponse socialLoginMember(OauthProvider provider, IdTokenRequest request) {
        OidcUser oidcUser = idTokenVerifier.getOidcUser(request.idToken(), provider);

        MemberInternalInfoResponse response =
                memberServiceClient.findByOauthInfo(
                        MemberOauthInfoRequest.of(
                                oidcUser.getSubject(), oidcUser.getIssuer().toString()));

        if (response != null) {
            //            if (response.status() == MemberStatus.DELETED) {
            //
            //            }

            return getLoginResponse(response.memberId(), response.role());
        }

        String registerToken =
                jwtTokenService.createRegisterToken(
                        oidcUser.getSubject(), oidcUser.getIssuer().toString());
        return SocialLoginResponse.notRegistered(registerToken);
    }

    @Override
    public SocialLoginResponse registerMember(
            String registerTokenValue, MemberRegisterRequest request) {
        RegisterTokenDto registerTokenDto =
                jwtTokenService.validateRegisterToken(registerTokenValue);

        if (registerTokenDto == null) {
            throw new CustomException(AuthErrorCode.EXPIRED_REGISTER_TOKEN);
        }

        MemberInternalRegisterRequest registerRequest =
                new MemberInternalRegisterRequest(
                        registerTokenDto.oauthId(),
                        registerTokenDto.oauthProvider(),
                        request.nickname(),
                        request.age(),
                        request.gender());

        MemberInternalRegisterResponse response =
                memberServiceClient.registerMember(registerRequest);

        return getLoginResponse(response.memberId(), response.role());
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

        MemberInternalInfoResponse response =
                memberServiceClient.findByMemberId(newRefreshTokenDto.memberId());
        AccessTokenDto newAccessTokenDto =
                jwtTokenService.reissueAccessToken(response.memberId(), response.role());

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

        //        Member member =
        //                memberRepository
        //                        .findById(Long.parseLong(memberId))
        //                        .orElseThrow(() -> new
        // CustomException(MemberErrorCode.MEMBER_NOT_FOUND));
        //
        //        member.withdrawal();
    }

    private SocialLoginResponse getLoginResponse(Long memberId, MemberRole memberRole) {
        String accessToken = jwtTokenService.createAccessToken(memberId, memberRole);
        String refreshToken = jwtTokenService.createRefreshToken(memberId);
        return SocialLoginResponse.registered(accessToken, refreshToken);
    }
}
