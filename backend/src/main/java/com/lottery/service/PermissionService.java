package com.lottery.service;

import com.lottery.entity.Menu;
import com.lottery.entity.Role;
import com.lottery.mapper.MenuMapper;
import com.lottery.mapper.RoleMapper;
import com.lottery.mapper.UserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 权限服务 — 加载用户角色、权限、菜单树
 */
@Service
@RequiredArgsConstructor
public class PermissionService {

    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final MenuMapper menuMapper;

    /** 查询用户的角色ID列表 */
    public List<Integer> getRoleIds(Integer userId) {
        return userRoleMapper.selectRoleIdsByUserId(userId);
    }

    /** 查询用户的角色列表 */
    public List<Role> getRoles(Integer userId) {
        List<Integer> roleIds = getRoleIds(userId);
        if (roleIds.isEmpty()) return List.of();
        return roleMapper.selectBatchIds(roleIds);
    }

    /** 查询用户的权限标识集合 */
    public Set<String> getPermissions(Integer userId) {
        List<Integer> roleIds = getRoleIds(userId);
        if (roleIds.isEmpty()) return Set.of();
        List<String> perms = menuMapper.selectPermsByRoleIds(roleIds);
        return new HashSet<>(perms);
    }

    /** 查询用户的菜单树（目录+菜单，不含按钮，用于前端路由） */
    public List<Menu> getMenus(Integer userId) {
        List<Integer> roleIds = getRoleIds(userId);
        if (roleIds.isEmpty()) return List.of();
        List<Menu> menus = menuMapper.selectMenusByRoleIds(roleIds);
        return buildMenuTree(menus);
    }

    /** 查询所有菜单树（管理界面用） */
    public List<Menu> getAllMenuTree() {
        List<Menu> menus = menuMapper.selectMenuTreeAll();
        return buildMenuTree(menus);
    }

    /** 判断用户是否为超级管理员（拥有 admin 角色） */
    public boolean isAdmin(Integer userId) {
        List<Role> roles = getRoles(userId);
        return roles.stream().anyMatch(r -> "admin".equals(r.getRoleKey()));
    }

    /** 构建菜单树 */
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

        // 排序
        sortMenuTree(roots);
        return roots;
    }

    private void sortMenuTree(List<Menu> menus) {
        menus.sort(Comparator.comparing(Menu::getOrderNum, Comparator.nullsLast(Comparator.naturalOrder())));
        for (Menu m : menus) {
            if (m.getChildren() != null && !m.getChildren().isEmpty()) {
                sortMenuTree(m.getChildren());
            }
        }
    }
}
