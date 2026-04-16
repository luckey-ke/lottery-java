package com.lottery.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

/**
 * 系统角色
 */
@Data
@TableName("sys_role")
public class Role {

    @TableId(type = IdType.AUTO)
    private Integer roleId;

    /** 角色名称 */
    private String roleName;

    /** 角色权限标识，如 admin / editor / viewer */
    private String roleKey;

    /** 显示顺序 */
    private Integer sort;

    /** 状态：0=正常 1=停用 */
    private String status;

    /** 备注 */
    private String remark;

    private String createdAt;
    private String updatedAt;
}
