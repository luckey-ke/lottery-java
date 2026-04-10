package com.lottery.controller;

import com.lottery.common.BusinessException;
import com.lottery.common.JwtUtils;
import com.lottery.entity.User;
import com.lottery.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 认证接口 — 注册 / 登录 / 刷新Token / 获取当前用户信息
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    /** 注册 */
    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        String nickname = body.getOrDefault("nickname", username);

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw BusinessException.badRequest("用户名和密码不能为空");
        }
        if (username.length() < 3 || username.length() > 32) {
            throw BusinessException.badRequest("用户名长度需在 3-32 字符之间");
        }
        if (password.length() < 6) {
            throw BusinessException.badRequest("密码长度不能少于 6 位");
        }

        if (userMapper.existsByUsername(username) > 0) {
            throw BusinessException.badRequest("用户名已存在");
        }

        String now = LocalDateTime.now().format(TS_FMT);
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(User.ROLE_USER);
        user.setNickname(nickname);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        userMapper.insert(user);

        String token = jwtUtils.generateAccessToken(username, User.ROLE_USER);
        String refreshToken = jwtUtils.generateRefreshToken(username);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "注册成功");
        result.put("token", token);
        result.put("refreshToken", refreshToken);
        result.put("user", userInfo(user));
        return result;
    }

    /** 登录 */
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        if (username == null || password == null) {
            throw BusinessException.badRequest("用户名和密码不能为空");
        }

        User user = userMapper.findByUsername(username);
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw BusinessException.badRequest("用户名或密码错误");
        }

        String token = jwtUtils.generateAccessToken(username, user.getRole());
        String refreshToken = jwtUtils.generateRefreshToken(username);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "登录成功");
        result.put("token", token);
        result.put("refreshToken", refreshToken);
        result.put("user", userInfo(user));
        return result;
    }

    /** 刷新 Token */
    @PostMapping("/refresh")
    public Map<String, Object> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null || !jwtUtils.validateToken(refreshToken)) {
            throw BusinessException.badRequest("无效的刷新令牌");
        }
        if (!"refresh".equals(jwtUtils.getTokenType(refreshToken))) {
            throw BusinessException.badRequest("令牌类型错误");
        }

        String username = jwtUtils.getUsername(refreshToken);
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw BusinessException.badRequest("用户不存在");
        }

        String newToken = jwtUtils.generateAccessToken(username, user.getRole());
        String newRefreshToken = jwtUtils.generateRefreshToken(username);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("token", newToken);
        result.put("refreshToken", newRefreshToken);
        return result;
    }

    /** 获取当前登录用户信息 */
    @GetMapping("/me")
    public Map<String, Object> me(@RequestAttribute(required = false) String username) {
        // 从 SecurityContext 获取
        var auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        if (auth == null) {
            throw new BusinessException(401, "未登录");
        }
        String currentUsername = auth.getName();
        User user = userMapper.findByUsername(currentUsername);
        if (user == null) {
            throw new BusinessException(401, "用户不存在");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("user", userInfo(user));
        return result;
    }

    private Map<String, Object> userInfo(User user) {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("id", user.getId());
        info.put("username", user.getUsername());
        info.put("nickname", user.getNickname());
        info.put("role", user.getRole());
        info.put("createdAt", user.getCreatedAt());
        return info;
    }
}
