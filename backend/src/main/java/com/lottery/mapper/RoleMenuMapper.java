package com.lottery.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * sys_role_menu 关联表 Mapper
 */
@Mapper
public interface RoleMenuMapper {

    /** 查询角色的菜单ID列表 */
    List<Integer> selectMenuIdsByRoleId(@Param("roleId") Integer roleId);

    /** 批量插入角色菜单 */
    int insertBatch(@Param("roleId") Integer roleId, @Param("menuIds") List<Integer> menuIds);

    /** 删除角色的所有菜单关联 */
    int deleteByRoleId(@Param("roleId") Integer roleId);

    /** 删除指定菜单的所有关联 */
    int deleteByMenuId(@Param("menuId") Integer menuId);
}
