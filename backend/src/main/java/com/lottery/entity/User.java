package com.lottery.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.util.List;

@Data
@TableName("sys_user")
public class User {
    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 用户账号 */
    private String username;

    /** 密码（BCrypt） */
    private String password;

    /** 用户昵称 */
    private String nickname;

    /** 用户邮箱 */
    private String email;

    /** 手机号码 */
    private String phone;

    /** 用户性别：0=男 1=女 2=未知 */
    private String sex;

    /** 头像地址 */
    private String avatar;

    /** 帐号状态：0=正常 1=停用 */
    private String status;

    /** 删除标志：0=存在 1=删除 */
    private String delFlag;

    /** 最后登录IP */
    private String loginIp;

    /** 最后登录时间 */
    private String loginDate;

    private String createdAt;
    private String updatedAt;

    // ===== 非数据库字段 =====

    /** 用户拥有的角色ID列表（查询时填充） */
    @TableField(exist = false)
    private List<Integer> roleIds;

    /** 用户拥有的角色列表 */
    @TableField(exist = false)
    private List<Role> roles;

    /** 用户权限标识集合 */
    @TableField(exist = false)
    private List<String> permissions;

    // ===== 常量 =====

    public static final String STATUS_NORMAL = "0";
    public static final String STATUS_DISABLE = "1";
    public static final String DEL_FLAG_NORMAL = "0";
    public static final String DEL_FLAG_DELETED = "1";
}
