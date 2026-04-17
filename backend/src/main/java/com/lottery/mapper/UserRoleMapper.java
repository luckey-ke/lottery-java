package com.lottery.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * sys_user_role 关联表 Mapper
 */
@Mapper
public interface UserRoleMapper {

    /** 查询用户的角色ID列表 */
    List<Integer> selectRoleIdsByUserId(@Param("userId") Integer userId);

    /** 批量插入用户角色 */
    int insertBatch(@Param("userId") Integer userId, @Param("roleIds") List<Integer> roleIds);

    /** 删除用户的所有角色 */
    int deleteByUserId(@Param("userId") Integer userId);

    /** 删除指定角色的所有关联 */
    int deleteByRoleId(@Param("roleId") Integer roleId);

    /** 查询分配了指定角色的用户数 */
    int countUsersByRoleId(@Param("roleId") Integer roleId);
}
