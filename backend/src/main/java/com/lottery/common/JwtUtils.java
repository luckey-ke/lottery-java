package com.lottery.common;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Date;
import java.util.List;

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

    /** 生成 Access Token（含角色 key 和权限列表） */
    public String generateAccessToken(String username, List<String> roleKeys, List<String> permissions) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expiration);

        JwtBuilder builder = Jwts.builder()
                .subject(username)
                .claim("type", "access")
                .claim("roles", roleKeys != null ? roleKeys : List.of())
                .claim("perms", permissions != null ? permissions : List.of())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key);

        return builder.compact();
    }

    /** 兼容旧接口：单角色生成 token */
    public String generateAccessToken(String username, String role) {
        return generateAccessToken(username, List.of(role != null ? role.toLowerCase() : "user"), List.of());
    }

    /** 生成 Refresh Token */
    public String generateRefreshToken(String username) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + refreshExpiration);

        return Jwts.builder()
                .subject(username)
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    /** 解析 Token 获取用户名 */
    public String getUsername(String token) {
        return parseClaims(token).getSubject();
    }

    /** 解析 Token 获取角色 key 列表 */
    @SuppressWarnings("unchecked")
    public List<String> getRoleKeys(String token) {
        Claims claims = parseClaims(token);
        Object roles = claims.get("roles");
        if (roles instanceof List) {
            return (List<String>) roles;
        }
        // 兼容旧单角色 token
        String oldRole = claims.get("role", String.class);
        if (oldRole != null) {
            return List.of(oldRole.toLowerCase());
        }
        return List.of();
    }

    /** 获取 Token 类型 */
    public String getTokenType(String token) {
        return parseClaims(token).get("type", String.class);
    }

    /** 解析 Token 获取权限标识列表 */
    @SuppressWarnings("unchecked")
    public List<String> getPermissions(String token) {
        Claims claims = parseClaims(token);
        Object perms = claims.get("perms");
        if (perms instanceof List) {
            return (List<String>) perms;
        }
        return List.of();
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

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
