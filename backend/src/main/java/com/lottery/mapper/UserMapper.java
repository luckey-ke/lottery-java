package com.lottery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lottery.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    User findByUsername(@Param("username") String username);

    int existsByUsername(@Param("username") String username);

    /** 分页查询用户列表（带搜索） */
    List<User> selectUserList(@Param("username") String username,
                              @Param("phone") String phone,
                              @Param("status") String status);

    /** 查询用户详情（含角色） */
    User selectUserWithRoles(@Param("userId") Integer userId);
}
