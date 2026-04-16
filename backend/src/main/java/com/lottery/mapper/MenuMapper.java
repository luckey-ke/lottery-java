package com.lottery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lottery.entity.Menu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Set;

@Mapper
public interface MenuMapper extends BaseMapper<Menu> {

    /** 根据角色ID列表查询菜单 */
    List<Menu> selectMenusByRoleIds(@Param("roleIds") List<Integer> roleIds);

    /** 根据角色ID列表查询权限标识 */
    List<String> selectPermsByRoleIds(@Param("roleIds") List<Integer> roleIds);

    /** 查询所有正常状态的菜单树 */
    List<Menu> selectMenuTreeAll();

    /** 根据菜单ID列表查询 */
    List<Menu> selectByMenuIds(@Param("menuIds") List<Integer> menuIds);
}
