package com.lgcns.util;

import static com.lgcns.constants.SecurityConstants.TOKEN_ROLE_NAME;

import com.lgcns.domain.MemberRole;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtUtil {

    @Value("${jwt.access-token-secret}")
    private String accessTokenSecret;

    @Value("${jwt.refresh-token-secret}")
    private String refreshTokenSecret;

    @Value("${jwt.access-token-expiration-time}")
    private Long accessTokenExpirationTime;

    @Value("${jwt.refresh-token-expiration-time}")
    private Long refreshTokenExpirationTime;

    @Value("${jwt.issuer}")
    private String issuer;

    private Long accessTokenExpirationMilliTime() {
        return accessTokenExpirationTime * 1000;
    }

    private Long refreshTokenExpirationMilliTime() {
        return refreshTokenExpirationTime * 1000;
    }

    public String generateAccessToken(Long memberId, MemberRole memberRole) {
        Date issuedAt = new Date();
        Date expiredAt = new Date(issuedAt.getTime() + accessTokenExpirationMilliTime());
        return buildAccessToken(memberId, memberRole, issuedAt, expiredAt);
    }

    public String generateRefreshToken(Long memberId) {
        Date issuedAt = new Date();
        Date expiredAt = new Date(issuedAt.getTime() + refreshTokenExpirationMilliTime());
        return buildRefreshToken(memberId, issuedAt, expiredAt);
    }

    public long getRefreshTokenExpirationTime() {
        return refreshTokenExpirationTime;
    }

    private Key getAccessTokenKey() {
        return Keys.hmacShaKeyFor(accessTokenSecret.getBytes());
    }

    private Key getRefreshTokenKey() {
        return Keys.hmacShaKeyFor(refreshTokenSecret.getBytes());
    }

    private String buildAccessToken(
            Long memberId, MemberRole memberRole, Date issuedAt, Date expiredAt) {
        return Jwts.builder()
                .setIssuer(issuer)
                .setSubject(memberId.toString())
                .claim(TOKEN_ROLE_NAME, memberRole.name())
                .setIssuedAt(issuedAt)
                .setExpiration(expiredAt)
                .signWith(getAccessTokenKey())
                .compact();
    }

    private String buildRefreshToken(Long memberId, Date issuedAt, Date expiredAt) {
        return Jwts.builder()
                .setIssuer(issuer)
                .setSubject(memberId.toString())
                .setIssuedAt(issuedAt)
                .setExpiration(expiredAt)
                .signWith(getRefreshTokenKey())
                .compact();
    }
}
