package com.example.study_cards.infra.security.jwt;

import com.example.study_cards.domain.user.entity.Role;
import com.example.study_cards.infra.security.exception.JwtErrorCode;
import com.example.study_cards.infra.security.exception.JwtException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;
    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(Long userId, String email, Set<Role> roles) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.getAccessTokenExpireMinutes() * 60 * 1000L);

        List<String> roleNames = roles.stream()
                .map(Role::name)
                .toList();

        return Jwts.builder()
                .issuer(jwtProperties.getIssuer())
                .subject(userId.toString())
                .claim("email", email)
                .claim("roles", roleNames)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    public String createRefreshToken(Long userId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.getRefreshTokenExpireDays() * 24 * 60 * 60 * 1000L);

        return Jwts.builder()
                .issuer(jwtProperties.getIssuer())
                .subject(userId.toString())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    public void validateToken(String token) {
        try {
            parseToken(token);
        } catch (ExpiredJwtException e) {
            throw new JwtException(JwtErrorCode.EXPIRED_TOKEN);
        } catch (MalformedJwtException e) {
            throw new JwtException(JwtErrorCode.MALFORMED_TOKEN);
        } catch (SignatureException e) {
            throw new JwtException(JwtErrorCode.INVALID_SIGNATURE);
        } catch (UnsupportedJwtException e) {
            throw new JwtException(JwtErrorCode.UNSUPPORTED_TOKEN);
        } catch (JwtException e) {
            throw e;
        } catch (Exception e) {
            throw new JwtException(JwtErrorCode.INVALID_TOKEN);
        }
    }

    public Long getUserId(String token) {
        return Long.parseLong(parseToken(token).getSubject());
    }

    public String getEmail(String token) {
        return parseToken(token).get("email", String.class);
    }

    @SuppressWarnings("unchecked")
    public Set<Role> getRoles(String token) {
        List<String> roleNames = parseToken(token).get("roles", List.class);
        return roleNames.stream()
                .map(Role::valueOf)
                .collect(Collectors.toSet());
    }

    public long getRemainingExpiration(String token) {
        Date expiration = parseToken(token).getExpiration();
        return expiration.getTime() - System.currentTimeMillis();
    }

    public long getAccessTokenExpirationMs() {
        return jwtProperties.getAccessTokenExpireMinutes() * 60 * 1000L;
    }

    public long getRefreshTokenExpirationMs() {
        return jwtProperties.getRefreshTokenExpireDays() * 24 * 60 * 60 * 1000L;
    }

    private Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
