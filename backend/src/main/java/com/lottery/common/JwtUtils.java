package com.lottery.common;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类 — 生成/解析/验证 Token
 */
@Slf4j
@Component
public class JwtUtils {

    @Value("${lottery.jwt.secret:lottery-java-default-secret-key-at-least-32-chars!}")
    private String secret;

    @Value("${lottery.jwt.expiration:86400000}")
    private long expiration; // 默认 24h

    @Value("${lottery.jwt.refresh-expiration:604800000}")
    private long refreshExpiration; // 默认 7d

    private SecretKey key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** 生成 Access Token */
    public String generateAccessToken(String username, String role) {
        return buildToken(username, role, expiration, "access");
    }

    /** 生成 Refresh Token */
    public String generateRefreshToken(String username) {
        return buildToken(username, null, refreshExpiration, "refresh");
    }

    /** 解析 Token 获取用户名 */
    public String getUsername(String token) {
        return parseClaims(token).getSubject();
    }

    /** 解析 Token 获取角色 */
    public String getRole(String token) {
        Claims claims = parseClaims(token);
        return claims.get("role", String.class);
    }

    /** 获取 Token 类型 */
    public String getTokenType(String token) {
        return parseClaims(token).get("type", String.class);
    }

    /** 验证 Token */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT 验证失败: {}", e.getMessage());
            return false;
        }
    }

    private String buildToken(String username, String role, long expirationMs, String type) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        JwtBuilder builder = Jwts.builder()
                .subject(username)
                .claim("type", type)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key);

        if (role != null) {
            builder.claim("role", role);
        }

        return builder.compact();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
