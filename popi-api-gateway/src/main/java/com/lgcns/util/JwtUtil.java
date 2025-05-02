package com.lgcns.util;

import static com.lgcns.constants.SecurityConstants.TOKEN_ROLE_NAME;

import com.lgcns.domain.MemberRole;
import com.lgcns.dto.AccessTokenDto;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

    @Value("${jwt.issuer}")
    private String issuer;

    @Value("${jwt.access-token-secret}")
    private String accessTokenSecret;

    public AccessTokenDto parseAccessToken(String accessTokenValue) throws ExpiredJwtException {
        try {
            Jws<Claims> claims = getClaims(accessTokenValue, getAccessTokenKey());

            return AccessTokenDto.of(
                    Long.parseLong(claims.getBody().getSubject()),
                    MemberRole.valueOf(claims.getBody().get(TOKEN_ROLE_NAME, String.class)),
                    accessTokenValue);
        } catch (ExpiredJwtException e) {
            throw e;
        } catch (Exception e) {
            return null;
        }
    }

    private Jws<Claims> getClaims(String token, Key key) {
        return Jwts.parserBuilder()
                .requireIssuer(issuer)
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);
    }

    private Key getAccessTokenKey() {
        return Keys.hmacShaKeyFor(accessTokenSecret.getBytes());
    }
}
