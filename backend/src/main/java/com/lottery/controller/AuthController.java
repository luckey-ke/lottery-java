package com.lottery.controller;

import com.lottery.common.BusinessException;
import com.lottery.common.JwtUtils;
import com.lottery.entity.User;
import com.lottery.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 认证接口 — 注册 / 登录 / 刷新Token / 用户信息 / 管理员操作
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @Value("${lottery.auth.admin-invite-code:}")
    private String adminInviteCode;

    /** 注册（可选邀请码） */
    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        String nickname = body.getOrDefault("nickname", username);
        String inviteCode = body.get("inviteCode");

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

        // 判断邀请码
        String role = User.ROLE_USER;
        if (adminInviteCode != null && !adminInviteCode.isBlank()
                && adminInviteCode.equals(inviteCode)) {
            role = User.ROLE_ADMIN;
        }

        String now = LocalDateTime.now().format(TS_FMT);
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        user.setNickname(nickname);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        userMapper.insert(user);

        String token = jwtUtils.generateAccessToken(username, role);
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
        String refreshTokenValue = body.get("refreshToken");
        if (refreshTokenValue == null || !jwtUtils.validateToken(refreshTokenValue)) {
            throw BusinessException.badRequest("无效的刷新令牌");
        }
        if (!"refresh".equals(jwtUtils.getTokenType(refreshTokenValue))) {
            throw BusinessException.badRequest("令牌类型错误");
        }

        String username = jwtUtils.getUsername(refreshTokenValue);
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
    public Map<String, Object> me() {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userMapper.findByUsername(currentUsername);
        if (user == null) {
            throw new BusinessException(401, "用户不存在");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("user", userInfo(user));
        return result;
    }

    // ===== 管理员接口 =====

    /** 查看所有用户（仅管理员） */
    @GetMapping("/users")
    public Map<String, Object> listUsers(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        List<User> users = userMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                        .select(User::getId, User::getUsername, User::getNickname,
                                User::getRole, User::getCreatedAt)
                        .orderByDesc(User::getCreatedAt)
                        .last("LIMIT " + limit + " OFFSET " + offset));
        int total = Math.toIntExact(userMapper.selectCount(null));

        List<Map<String, Object>> list = new ArrayList<>();
        for (User u : users) list.add(userInfo(u));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("data", list);
        result.put("total", total);
        return result;
    }

    /** 提升/降级用户角色（仅管理员） */
    @PutMapping("/users/{id}/role")
    public Map<String, Object> updateRole(
            @PathVariable Integer id,
            @RequestBody Map<String, String> body) {
        String newRole = body.get("role");
        if (newRole == null || (!newRole.equals("ADMIN") && !newRole.equals("USER"))) {
            throw BusinessException.badRequest("角色只能是 ADMIN 或 USER");
        }

        User user = userMapper.selectById(id);
        if (user == null) {
            throw BusinessException.badRequest("用户不存在");
        }

        // 不能修改自己
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        if (user.getUsername().equals(currentUsername)) {
            throw BusinessException.badRequest("不能修改自己的角色");
        }

        user.setRole(newRole);
        user.setUpdatedAt(LocalDateTime.now().format(TS_FMT));
        userMapper.updateById(user);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "角色已更新为 " + newRole);
        result.put("user", userInfo(user));
        return result;
    }

    /** 是否需要邀请码注册（前端用于判断是否显示邀请码输入框） */
    @GetMapping("/config")
    public Map<String, Object> config() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("inviteCodeRequired", adminInviteCode != null && !adminInviteCode.isBlank());
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
