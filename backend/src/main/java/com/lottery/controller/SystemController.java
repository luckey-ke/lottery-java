package com.lottery.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lottery.common.BusinessException;
import com.lottery.entity.Menu;
import com.lottery.entity.Role;
import com.lottery.entity.User;
import com.lottery.mapper.*;
import com.lottery.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 系统管理接口 — 用户管理 / 角色管理 / 菜单管理
 * 仅 ADMIN 角色可访问（SecurityConfig 中配置）
 */
@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
public class SystemController {

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final MenuMapper menuMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMenuMapper roleMenuMapper;
    private final PermissionService permissionService;
    private final PasswordEncoder passwordEncoder;

    // ================================================================
    // ==================== 用户管理 ==================================
    // ================================================================

    /** 用户列表 */
    @GetMapping("/users")
    public Map<String, Object> listUsers(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset) {

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .select(User::getId, User::getUsername, User::getNickname, User::getEmail,
                        User::getPhone, User::getStatus, User::getLoginDate, User::getCreatedAt)
                .eq(User::getDelFlag, User.DEL_FLAG_NORMAL)
                .like(username != null && !username.isBlank(), User::getUsername, username)
                .like(phone != null && !phone.isBlank(), User::getPhone, phone)
                .eq(status != null && !status.isBlank(), User::getStatus, status)
                .orderByDesc(User::getId)
                .last("LIMIT " + limit + " OFFSET " + offset);

        List<User> users = userMapper.selectList(wrapper);
        long total = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getDelFlag, User.DEL_FLAG_NORMAL));

        List<Map<String, Object>> list = new ArrayList<>();
        for (User u : users) {
            List<Role> roles = permissionService.getRoles(u.getId());
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", u.getId());
            map.put("username", u.getUsername());
            map.put("nickname", u.getNickname());
            map.put("email", u.getEmail());
            map.put("phone", u.getPhone());
            map.put("status", u.getStatus());
            map.put("roles", roles.stream().map(Role::getRoleKey).toList());
            map.put("roleIds", roles.stream().map(Role::getRoleId).toList());
            map.put("loginDate", u.getLoginDate());
            map.put("createdAt", u.getCreatedAt());
            list.add(map);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("data", list);
        result.put("total", total);
        return result;
    }

    /** 新增用户 */
    @PostMapping("/users")
    public Map<String, Object> addUser(@RequestBody Map<String, Object> body) {
        String username = (String) body.get("username");
        String password = (String) body.get("password");
        String nickname = (String) body.getOrDefault("nickname", username);
        String email = (String) body.get("email");
        String phone = (String) body.get("phone");
        String status = (String) body.getOrDefault("status", User.STATUS_NORMAL);

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw BusinessException.badRequest("用户名和密码不能为空");
        }
        if (userMapper.existsByUsername(username) > 0) {
            throw BusinessException.badRequest("用户名已存在");
        }

        String now = LocalDateTime.now().format(TS_FMT);
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setNickname(nickname);
        user.setEmail(email);
        user.setPhone(phone);
        user.setStatus(status);
        user.setDelFlag(User.DEL_FLAG_NORMAL);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        userMapper.insert(user);

        // 分配角色
        @SuppressWarnings("unchecked")
        List<Integer> roleIds = (List<Integer>) body.get("roleIds");
        if (roleIds != null && !roleIds.isEmpty()) {
            userRoleMapper.insertBatch(user.getId(), roleIds);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "用户创建成功");
        result.put("id", user.getId());
        return result;
    }

    /** 修改用户 */
    @PutMapping("/users/{id}")
    public Map<String, Object> updateUser(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        User user = userMapper.selectById(id);
        if (user == null || User.DEL_FLAG_DELETED.equals(user.getDelFlag())) {
            throw BusinessException.badRequest("用户不存在");
        }

        if (body.containsKey("nickname")) user.setNickname((String) body.get("nickname"));
        if (body.containsKey("email")) user.setEmail((String) body.get("email"));
        if (body.containsKey("phone")) user.setPhone((String) body.get("phone"));
        if (body.containsKey("status")) user.setStatus((String) body.get("status"));
        if (body.containsKey("password")) {
            String pw = (String) body.get("password");
            if (pw != null && !pw.isBlank()) {
                user.setPassword(passwordEncoder.encode(pw));
            }
        }
        user.setUpdatedAt(LocalDateTime.now().format(TS_FMT));
        userMapper.updateById(user);

        // 更新角色
        @SuppressWarnings("unchecked")
        List<Integer> roleIds = (List<Integer>) body.get("roleIds");
        if (roleIds != null) {
            userRoleMapper.deleteByUserId(id);
            if (!roleIds.isEmpty()) {
                userRoleMapper.insertBatch(id, roleIds);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "用户更新成功");
        return result;
    }

    /** 删除用户（逻辑删除） */
    @DeleteMapping("/users/{id}")
    public Map<String, Object> deleteUser(@PathVariable Integer id) {
        // 不能删除自己
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userMapper.selectById(id);
        if (user == null) throw BusinessException.badRequest("用户不存在");
        if (user.getUsername().equals(currentUsername)) {
            throw BusinessException.badRequest("不能删除自己");
        }

        user.setDelFlag(User.DEL_FLAG_DELETED);
        user.setUpdatedAt(LocalDateTime.now().format(TS_FMT));
        userMapper.updateById(user);
        userRoleMapper.deleteByUserId(id);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "用户已删除");
        return result;
    }

    // ================================================================
    // ==================== 角色管理 ==================================
    // ================================================================

    /** 角色列表 */
    @GetMapping("/roles")
    public Map<String, Object> listRoles() {
        List<Role> roles = roleMapper.selectList(
                new LambdaQueryWrapper<Role>().orderByAsc(Role::getSort));

        List<Map<String, Object>> list = new ArrayList<>();
        for (Role r : roles) {
            List<Integer> menuIds = roleMenuMapper.selectMenuIdsByRoleId(r.getRoleId());
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("roleId", r.getRoleId());
            map.put("roleName", r.getRoleName());
            map.put("roleKey", r.getRoleKey());
            map.put("sort", r.getSort());
            map.put("status", r.getStatus());
            map.put("remark", r.getRemark());
            map.put("menuIds", menuIds);
            map.put("createdAt", r.getCreatedAt());
            list.add(map);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("data", list);
        return result;
    }

    /** 新增角色 */
    @PostMapping("/roles")
    public Map<String, Object> addRole(@RequestBody Map<String, Object> body) {
        String roleName = (String) body.get("roleName");
        String roleKey = (String) body.get("roleKey");

        if (roleName == null || roleKey == null) {
            throw BusinessException.badRequest("角色名称和标识不能为空");
        }

        // 检查 roleKey 是否重复
        long count = roleMapper.selectCount(new LambdaQueryWrapper<Role>().eq(Role::getRoleKey, roleKey));
        if (count > 0) throw BusinessException.badRequest("角色标识已存在");

        String now = LocalDateTime.now().format(TS_FMT);
        Role role = new Role();
        role.setRoleName(roleName);
        role.setRoleKey(roleKey);
        role.setSort((Integer) body.getOrDefault("sort", 0));
        role.setStatus((String) body.getOrDefault("status", "0"));
        role.setRemark((String) body.get("remark"));
        role.setCreatedAt(now);
        role.setUpdatedAt(now);
        roleMapper.insert(role);

        // 分配菜单
        @SuppressWarnings("unchecked")
        List<Integer> menuIds = (List<Integer>) body.get("menuIds");
        if (menuIds != null && !menuIds.isEmpty()) {
            roleMenuMapper.insertBatch(role.getRoleId(), menuIds);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "角色创建成功");
        result.put("roleId", role.getRoleId());
        return result;
    }

    /** 修改角色 */
    @PutMapping("/roles/{id}")
    public Map<String, Object> updateRole(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        Role role = roleMapper.selectById(id);
        if (role == null) throw BusinessException.badRequest("角色不存在");

        if (body.containsKey("roleName")) role.setRoleName((String) body.get("roleName"));
        if (body.containsKey("sort")) role.setSort((Integer) body.get("sort"));
        if (body.containsKey("status")) role.setStatus((String) body.get("status"));
        if (body.containsKey("remark")) role.setRemark((String) body.get("remark"));
        role.setUpdatedAt(LocalDateTime.now().format(TS_FMT));
        roleMapper.updateById(role);

        // 更新菜单
        @SuppressWarnings("unchecked")
        List<Integer> menuIds = (List<Integer>) body.get("menuIds");
        if (menuIds != null) {
            roleMenuMapper.deleteByRoleId(id);
            if (!menuIds.isEmpty()) {
                roleMenuMapper.insertBatch(id, menuIds);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "角色更新成功");
        return result;
    }

    /** 删除角色 */
    @DeleteMapping("/roles/{id}")
    public Map<String, Object> deleteRole(@PathVariable Integer id) {
        Role role = roleMapper.selectById(id);
        if (role == null) throw BusinessException.badRequest("角色不存在");
        if ("admin".equals(role.getRoleKey())) {
            throw BusinessException.badRequest("不能删除管理员角色");
        }

        roleMapper.deleteById(id);
        roleMenuMapper.deleteByRoleId(id);
        // 同时删除用户角色关联
        userRoleMapper.deleteByRoleId(id);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "角色已删除");
        return result;
    }

    // ================================================================
    // ==================== 菜单管理 ==================================
    // ================================================================

    /** 菜单树 */
    @GetMapping("/menus")
    public Map<String, Object> listMenus(
            @RequestParam(required = false) String menuName,
            @RequestParam(required = false) String status) {

        LambdaQueryWrapper<Menu> wrapper = new LambdaQueryWrapper<Menu>()
                .like(menuName != null && !menuName.isBlank(), Menu::getMenuName, menuName)
                .eq(status != null && !status.isBlank(), Menu::getStatus, status)
                .orderByAsc(Menu::getParentId)
                .orderByAsc(Menu::getOrderNum);

        List<Menu> menus = menuMapper.selectList(wrapper);
        List<Menu> tree = buildMenuTree(menus);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("data", tree);
        return result;
    }

    /** 根据角色ID查询菜单树（带选中状态） */
    @GetMapping("/menus/role/{roleId}")
    public Map<String, Object> listMenusByRole(@PathVariable Integer roleId) {
        List<Menu> allMenus = menuMapper.selectMenuTreeAll();
        List<Integer> checkedMenuIds = roleMenuMapper.selectMenuIdsByRoleId(roleId);
        Set<Integer> checkedSet = new HashSet<>(checkedMenuIds);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("menus", buildMenuTree(allMenus));
        result.put("checkedKeys", checkedMenuIds);
        return result;
    }

    /** 新增菜单 */
    @PostMapping("/menus")
    public Map<String, Object> addMenu(@RequestBody Map<String, Object> body) {
        String now = LocalDateTime.now().format(TS_FMT);
        Menu menu = new Menu();
        menu.setMenuName((String) body.get("menuName"));
        menu.setParentId(body.get("parentId") != null ? (Integer) body.get("parentId") : 0);
        menu.setOrderNum(body.get("orderNum") != null ? (Integer) body.get("orderNum") : 0);
        menu.setPath((String) body.get("path"));
        menu.setComponent((String) body.get("component"));
        menu.setMenuType((String) body.getOrDefault("menuType", Menu.TYPE_MENU));
        menu.setPerms((String) body.get("perms"));
        menu.setIcon((String) body.get("icon"));
        menu.setVisible((String) body.getOrDefault("visible", Menu.VISIBLE_SHOW));
        menu.setStatus((String) body.getOrDefault("status", Menu.STATUS_NORMAL));
        menu.setCreatedAt(now);
        menu.setUpdatedAt(now);

        if (menu.getMenuName() == null || menu.getMenuName().isBlank()) {
            throw BusinessException.badRequest("菜单名称不能为空");
        }

        menuMapper.insert(menu);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "菜单创建成功");
        result.put("menuId", menu.getMenuId());
        return result;
    }

    /** 修改菜单 */
    @PutMapping("/menus/{id}")
    public Map<String, Object> updateMenu(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        Menu menu = menuMapper.selectById(id);
        if (menu == null) throw BusinessException.badRequest("菜单不存在");

        if (body.containsKey("menuName")) menu.setMenuName((String) body.get("menuName"));
        if (body.containsKey("parentId")) menu.setParentId((Integer) body.get("parentId"));
        if (body.containsKey("orderNum")) menu.setOrderNum((Integer) body.get("orderNum"));
        if (body.containsKey("path")) menu.setPath((String) body.get("path"));
        if (body.containsKey("component")) menu.setComponent((String) body.get("component"));
        if (body.containsKey("menuType")) menu.setMenuType((String) body.get("menuType"));
        if (body.containsKey("perms")) menu.setPerms((String) body.get("perms"));
        if (body.containsKey("icon")) menu.setIcon((String) body.get("icon"));
        if (body.containsKey("visible")) menu.setVisible((String) body.get("visible"));
        if (body.containsKey("status")) menu.setStatus((String) body.get("status"));
        menu.setUpdatedAt(LocalDateTime.now().format(TS_FMT));

        menuMapper.updateById(menu);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "菜单更新成功");
        return result;
    }

    /** 删除菜单 */
    @DeleteMapping("/menus/{id}")
    public Map<String, Object> deleteMenu(@PathVariable Integer id) {
        // 检查是否有子菜单
        long childCount = menuMapper.selectCount(
                new LambdaQueryWrapper<Menu>().eq(Menu::getParentId, id));
        if (childCount > 0) {
            throw BusinessException.badRequest("存在子菜单，不能删除");
        }

        menuMapper.deleteById(id);
        roleMenuMapper.deleteByMenuId(id);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "菜单已删除");
        return result;
    }

    // ===== 工具方法 =====

    private List<Menu> buildMenuTree(List<Menu> menus) {
        Map<Integer, Menu> map = new LinkedHashMap<>();
        List<Menu> roots = new ArrayList<>();
        for (Menu m : menus) {
            m.setChildren(new ArrayList<>());
            map.put(m.getMenuId(), m);
        }
        for (Menu m : menus) {
            if (m.getParentId() == null || m.getParentId() == 0) {
                roots.add(m);
            } else {
                Menu parent = map.get(m.getParentId());
                if (parent != null) {
                    parent.getChildren().add(m);
                } else {
                    roots.add(m);
                }
            }
        }
        return roots;
    }
}
