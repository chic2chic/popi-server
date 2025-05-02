package com.lgcns.service;

import com.lgcns.domain.MemberRole;
import com.lgcns.domain.RefreshToken;
import com.lgcns.dto.RefreshTokenDto;
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

    public RefreshTokenDto validateRefreshToken(String refreshToken) {
        return jwtUtil.parseRefreshToken(refreshToken);
    }
}
