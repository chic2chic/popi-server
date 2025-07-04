package com.lgcns.service;

import com.lgcns.domain.RefreshToken;
import com.lgcns.dto.AccessTokenDto;
import com.lgcns.dto.RefreshTokenDto;
import com.lgcns.dto.RegisterTokenDto;
import com.lgcns.enums.MemberRole;
import com.lgcns.error.exception.CustomException;
import com.lgcns.exception.AuthErrorCode;
import com.lgcns.repository.RefreshTokenRepository;
import com.lgcns.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtTokenService {

    private final JwtUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;

    public String createAccessToken(Long memberId, MemberRole memberRole) {
        return jwtUtil.generateAccessToken(memberId, memberRole);
    }

    public String createRefreshToken(Long memberId) {
        String token = jwtUtil.generateRefreshToken(memberId);
        RefreshToken refreshToken =
                RefreshToken.builder()
                        .memberId(memberId)
                        .token(token)
                        .ttl(jwtUtil.getRefreshTokenExpirationTime())
                        .build();
        refreshTokenRepository.save(refreshToken);

        return token;
    }

    public String createRegisterToken(String oauthId, String oauthProvider) {
        return jwtUtil.generateRegisterToken(oauthId, oauthProvider);
    }

    public RefreshTokenDto reissueRefreshToken(RefreshTokenDto oldRefreshTokenDto) {
        RefreshToken refreshToken =
                refreshTokenRepository
                        .findById(oldRefreshTokenDto.memberId())
                        .orElseThrow(
                                () -> new CustomException(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND));

        RefreshTokenDto refreshTokenDto =
                jwtUtil.generateRefreshTokenDto(refreshToken.getMemberId());
        refreshToken.updateRefreshToken(refreshTokenDto.refreshTokenValue(), refreshTokenDto.ttl());

        refreshTokenRepository.save(refreshToken);

        return refreshTokenDto;
    }

    public AccessTokenDto reissueAccessToken(Long memberId, MemberRole memberRole) {
        return jwtUtil.generateAccessTokenDto(memberId, memberRole);
    }

    public RefreshTokenDto validateRefreshToken(String refreshToken) {
        return jwtUtil.parseRefreshToken(refreshToken);
    }

    public RegisterTokenDto validateRegisterToken(String registerToken) {
        return jwtUtil.parseRegisterToken(registerToken);
    }
}
