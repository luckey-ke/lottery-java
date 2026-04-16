package com.lottery.controller;

import com.lottery.common.BusinessException;
import com.lottery.common.JwtUtils;
import com.lottery.entity.Menu;
import com.lottery.entity.Role;
import com.lottery.entity.User;
import com.lottery.mapper.RoleMapper;
import com.lottery.mapper.UserMapper;
import com.lottery.mapper.UserRoleMapper;
import com.lottery.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 认证接口 — 注册 / 登录 / 刷新Token / 用户信息
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final PermissionService permissionService;

    @Value("${lottery.auth.admin-invite-code:}")
    private String adminInviteCode;

    /** 注册 */
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

        String now = LocalDateTime.now().format(TS_FMT);
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setNickname(nickname);
        user.setStatus(User.STATUS_NORMAL);
        user.setDelFlag(User.DEL_FLAG_NORMAL);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        userMapper.insert(user);

        // 分配角色：邀请码 → admin，否则 → user
        String roleKey = "user";
        if (adminInviteCode != null && !adminInviteCode.isBlank()
                && adminInviteCode.equals(inviteCode)) {
            roleKey = "admin";
        }
        assignRole(user.getId(), roleKey);

        // 生成 token
        List<String> roleKeys = List.of(roleKey);
        Set<String> perms = permissionService.getPermissions(user.getId());
        String token = jwtUtils.generateAccessToken(username, roleKeys, new ArrayList<>(perms));
        String refreshToken = jwtUtils.generateRefreshToken(username);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "注册成功");
        result.put("token", token);
        result.put("refreshToken", refreshToken);
        result.put("user", userInfo(user, roleKeys, perms));
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
        if (User.STATUS_DISABLE.equals(user.getStatus())) {
            throw BusinessException.badRequest("账号已停用");
        }

        // 加载角色和权限
        List<Integer> roleIds = permissionService.getRoleIds(user.getId());
        List<Role> roles = permissionService.getRoles(user.getId());
        List<String> roleKeys = roles.stream().map(Role::getRoleKey).collect(Collectors.toList());
        Set<String> perms = permissionService.getPermissions(user.getId());

        // 更新登录信息
        user.setLoginDate(LocalDateTime.now().format(TS_FMT));
        userMapper.updateById(user);

        String token = jwtUtils.generateAccessToken(username, roleKeys, new ArrayList<>(perms));
        String refreshToken = jwtUtils.generateRefreshToken(username);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "登录成功");
        result.put("token", token);
        result.put("refreshToken", refreshToken);
        result.put("user", userInfo(user, roleKeys, perms));
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

        List<Role> roles = permissionService.getRoles(user.getId());
        List<String> roleKeys = roles.stream().map(Role::getRoleKey).collect(Collectors.toList());
        Set<String> perms = permissionService.getPermissions(user.getId());

        String token = jwtUtils.generateAccessToken(username, roleKeys, new ArrayList<>(perms));
        String newRefreshToken = jwtUtils.generateRefreshToken(username);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("token", token);
        result.put("refreshToken", newRefreshToken);
        return result;
    }

    /** 获取当前登录用户信息（含角色、权限、菜单） */
    @GetMapping("/me")
    public Map<String, Object> me() {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userMapper.findByUsername(currentUsername);
        if (user == null) {
            throw new BusinessException(401, "用户不存在");
        }

        List<Role> roles = permissionService.getRoles(user.getId());
        List<String> roleKeys = roles.stream().map(Role::getRoleKey).collect(Collectors.toList());
        Set<String> perms = permissionService.getPermissions(user.getId());
        List<Menu> menus = permissionService.getMenus(user.getId());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("user", userInfo(user, roleKeys, perms));
        result.put("roles", roleKeys);
        result.put("permissions", perms);
        result.put("menus", menuTreeToList(menus));
        return result;
    }

    /** 是否需要邀请码注册 */
    @GetMapping("/config")
    public Map<String, Object> config() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("inviteCodeRequired", adminInviteCode != null && !adminInviteCode.isBlank());
        return result;
    }

    // ===== 内部方法 =====

    /** 给用户分配角色 */
    private void assignRole(Integer userId, String roleKey) {
        List<Role> roles = roleMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Role>()
                        .eq(Role::getRoleKey, roleKey));
        if (!roles.isEmpty()) {
            userRoleMapper.insertBatch(userId, List.of(roles.get(0).getRoleId()));
        }
    }

    /** 构建用户信息 */
    private Map<String, Object> userInfo(User user, List<String> roleKeys, Set<String> permissions) {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("id", user.getId());
        info.put("username", user.getUsername());
        info.put("nickname", user.getNickname());
        info.put("email", user.getEmail());
        info.put("phone", user.getPhone());
        info.put("avatar", user.getAvatar());
        info.put("status", user.getStatus());
        info.put("roles", roleKeys);
        info.put("permissions", permissions);
        info.put("createdAt", user.getCreatedAt());
        return info;
    }

    /** 菜单树转平铺列表（前端需要的格式） */
    private List<Map<String, Object>> menuTreeToList(List<Menu> tree) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Menu m : tree) {
            if (Menu.TYPE_BUTTON.equals(m.getMenuType())) continue;
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("menuId", m.getMenuId());
            map.put("menuName", m.getMenuName());
            map.put("parentId", m.getParentId());
            map.put("orderNum", m.getOrderNum());
            map.put("path", m.getPath());
            map.put("component", m.getComponent());
            map.put("menuType", m.getMenuType());
            map.put("perms", m.getPerms());
            map.put("icon", m.getIcon());
            map.put("visible", m.getVisible());
            if (m.getChildren() != null && !m.getChildren().isEmpty()) {
                map.put("children", menuTreeToList(m.getChildren()));
            }
            list.add(map);
        }
        return list;
    }
}
