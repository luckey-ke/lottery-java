package com.lottery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lottery.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    User findByUsername(@Param("username") String username);

    int existsByUsername(@Param("username") String username);
}
