package com.mogak.npec.auth.domain;


import com.mogak.npec.auth.exception.InvalidTokenException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Component
public class TokenProvider {
    private final Key secretKey;
    private final Long accessValidityMilliSeconds;
    private final Long refreshValidityMilliSeconds;
    private final JwtParser jwtParser;

    public TokenProvider(@Value("${jwt.key}") String secretKey,
                         @Value("${jwt.expire-milliseconds.access}") Long accessValidityMilliSeconds,
                         @Value("${jwt.expire-milliseconds.refresh}") Long refreshValidityMilliSeconds) {
        this.secretKey = Keys.hmacShaKeyFor(secretKey.getBytes());
        this.accessValidityMilliSeconds = accessValidityMilliSeconds;
        this.refreshValidityMilliSeconds = refreshValidityMilliSeconds;
        this.jwtParser = Jwts.parserBuilder().setSigningKey(this.secretKey).build();
    }

    public String createAccessToken(Long memberId, Date issuedAt) {
        Date expireDate = new Date(issuedAt.getTime() + accessValidityMilliSeconds);

        return Jwts.builder()
                .claim("memberId", memberId)
                .setIssuedAt(issuedAt)
                .setExpiration(expireDate)
                .signWith(secretKey)
                .compact();
    }

    public String createRefreshToken(Date issuedAt) {
        Date expireDate = new Date(issuedAt.getTime() + refreshValidityMilliSeconds);

        return Jwts.builder()
                .setIssuedAt(issuedAt)
                .setExpiration(expireDate)
                .signWith(secretKey)
                .compact();
    }

    public Long getMemberIdFromToken(String token) {
        try {
            Claims claims = jwtParser
                    .parseClaimsJws(token)
                    .getBody();

            return claims.get("memberId", Long.class);

        } catch (ExpiredJwtException ex) {
            throw new InvalidTokenException("만료기간이 지난 토큰입니다.");
        } catch (SignatureException ex) {
            throw new InvalidTokenException("믿을 수 없는 토큰입니다.");
        } catch (MalformedJwtException ex) {
            throw new InvalidTokenException("올바른 토큰 형태가 아닙니다.");
        } catch (UnsupportedJwtException | IllegalArgumentException exception) {
            throw new InvalidTokenException("지원하지 않는 토큰 형식입니다.");
        }
    }

    public long getLeftExpireTimeFromToken(String token) {
        try {
            Date expiration = jwtParser
                    .parseClaimsJws(token)
                    .getBody()
                    .getExpiration();

            LocalDateTime expireDateTime = LocalDateTime.ofInstant(expiration.toInstant(), ZoneId.systemDefault());
            LocalDateTime now = LocalDateTime.now();

            return Duration.between(now, expireDateTime).getSeconds();

        } catch (ExpiredJwtException ex) {
            throw new InvalidTokenException("만료기간이 지난 토큰입니다.");
        } catch (SignatureException ex) {
            throw new InvalidTokenException("믿을 수 없는 토큰입니다.");
        } catch (MalformedJwtException ex) {
            throw new InvalidTokenException("올바른 토큰 형태가 아닙니다.");
        } catch (UnsupportedJwtException | IllegalArgumentException exception) {
            throw new InvalidTokenException("지원하지 않는 토큰 형식입니다.");
        }
    }

    public Long forcedGetParsedClaims(String token) {
        try {
            Claims claims = jwtParser
                    .parseClaimsJws(token)
                    .getBody();

            return claims.get("memberId", Long.class);

        } catch (ExpiredJwtException ex) {
            return ex.getClaims().get("memberId", Long.class);
        } catch (SignatureException ex) {
            throw new InvalidTokenException("믿을 수 없는 토큰입니다.");
        } catch (MalformedJwtException ex) {
            throw new InvalidTokenException("올바른 토큰 형태가 아닙니다.");
        } catch (UnsupportedJwtException | IllegalArgumentException exception) {
            throw new InvalidTokenException("지원하지 않는 토큰 형식입니다.");
        }
    }

    public boolean isValid(String token) {
        try {
            jwtParser.parseClaimsJws(token);

            return true;
        } catch (ExpiredJwtException | MalformedJwtException | SignatureException | UnsupportedJwtException | IllegalArgumentException exception) {
            return false;
        }
    }
}
