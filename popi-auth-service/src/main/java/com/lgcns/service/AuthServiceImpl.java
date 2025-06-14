package com.lgcns.service;

import static com.lgcns.grpc.mapper.MemberGrpcMapper.*;

import com.lgcns.client.MemberGrpcClient;
import com.lgcns.domain.OauthProvider;
import com.lgcns.dto.AccessTokenDto;
import com.lgcns.dto.RefreshTokenDto;
import com.lgcns.dto.RegisterTokenDto;
import com.lgcns.dto.request.IdTokenRequest;
import com.lgcns.dto.request.MemberRegisterRequest;
import com.lgcns.dto.response.SocialLoginResponse;
import com.lgcns.dto.response.TokenReissueResponse;
import com.lgcns.enums.MemberRole;
import com.lgcns.error.exception.CustomException;
import com.lgcns.exception.AuthErrorCode;
import com.lgcns.repository.RefreshTokenRepository;
import com.popi.common.grpc.member.*;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

    private final JwtTokenService jwtTokenService;
    private final IdTokenVerifier idTokenVerifier;
    private final MemberGrpcClient memberGrpcClient;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    public SocialLoginResponse socialLoginMember(OauthProvider provider, IdTokenRequest request) {
        OidcUser oidcUser = idTokenVerifier.getOidcUser(request.idToken(), provider);

        try {
            MemberInternalInfoResponse grpcResponse =
                    memberGrpcClient.findByOauthInfo(
                            MemberInternalOauthInfoRequest.newBuilder()
                                    .setOauthId(oidcUser.getSubject())
                                    .setOauthProvider(oidcUser.getIssuer().toString())
                                    .build());

            if (grpcResponse.getStatus() == MemberStatus.DELETED) {
                memberGrpcClient.rejoinMember(
                        MemberInternalIdRequest.newBuilder()
                                .setMemberId(grpcResponse.getMemberId())
                                .build());
            }

            return getLoginResponse(
                    grpcResponse.getMemberId(), toDomainMemberRole(grpcResponse.getRole()));

        } catch (StatusRuntimeException e) {
            Status.Code code = e.getStatus().getCode();

            if (code == Status.Code.NOT_FOUND) {
                String registerToken =
                        jwtTokenService.createRegisterToken(
                                oidcUser.getSubject(), oidcUser.getIssuer().toString());
                return SocialLoginResponse.notRegistered(registerToken);
            }
            throw e;
        }
    }

    @Override
    public SocialLoginResponse registerMember(
            String registerTokenValue, MemberRegisterRequest request) {
        RegisterTokenDto registerTokenDto =
                jwtTokenService.validateRegisterToken(registerTokenValue);

        if (registerTokenDto == null) {
            throw new CustomException(AuthErrorCode.EXPIRED_REGISTER_TOKEN);
        }

        MemberInternalRegisterRequest grpcRequest =
                MemberInternalRegisterRequest.newBuilder()
                        .setOauthId(registerTokenDto.oauthId())
                        .setOauthProvider(registerTokenDto.oauthProvider())
                        .setNickname(request.nickname())
                        .setAge(toGrpcMemberAge(request.age()))
                        .setGender(toGrpcMemberGender(request.gender()))
                        .build();

        MemberInternalRegisterResponse grpcResponse = memberGrpcClient.registerMember(grpcRequest);

        return getLoginResponse(
                grpcResponse.getMemberId(), toDomainMemberRole(grpcResponse.getRole()));
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

        MemberInternalInfoResponse grpcResponse =
                memberGrpcClient.findByMemberId(
                        MemberInternalIdRequest.newBuilder()
                                .setMemberId(newRefreshTokenDto.memberId())
                                .build());

        AccessTokenDto newAccessTokenDto =
                jwtTokenService.reissueAccessToken(
                        grpcResponse.getMemberId(), toDomainMemberRole(grpcResponse.getRole()));

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
    public void deleteRefreshToken(String memberId) {
        refreshTokenRepository
                .findById(Long.parseLong(memberId))
                .ifPresent(refreshTokenRepository::delete);
    }

    private SocialLoginResponse getLoginResponse(Long memberId, MemberRole memberRole) {
        String accessToken = jwtTokenService.createAccessToken(memberId, memberRole);
        String refreshToken = jwtTokenService.createRefreshToken(memberId);
        return SocialLoginResponse.registered(accessToken, refreshToken);
    }
}
