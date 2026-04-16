package com.lottery.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

/**
 * 系统菜单 / 权限
 * menu_type: M=目录 C=菜单 F=按钮
 */
@Data
@TableName("sys_menu")
public class Menu {

    @TableId(type = IdType.AUTO)
    private Integer menuId;

    /** 菜单名称 */
    private String menuName;

    /** 父菜单ID，0=顶级 */
    private Integer parentId;

    /** 显示顺序 */
    private Integer orderNum;

    /** 路由地址 */
    private String path;

    /** 组件路径（Vue component） */
    private String component;

    /** 菜单类型：M=目录 C=菜单 F=按钮 */
    private String menuType;

    /** 权限标识，如 lottery:lottery:fetch */
    private String perms;

    /** 菜单图标 */
    private String icon;

    /** 是否可见：0=显示 1=隐藏 */
    private String visible;

    /** 状态：0=正常 1=停用 */
    private String status;

    private String createdAt;
    private String updatedAt;

    // ===== 非数据库字段 =====

    @TableField(exist = false)
    private java.util.List<Menu> children;

    // 常量
    public static final String TYPE_DIR = "M";
    public static final String TYPE_MENU = "C";
    public static final String TYPE_BUTTON = "F";

    public static final String STATUS_NORMAL = "0";
    public static final String STATUS_DISABLE = "1";

    public static final String VISIBLE_SHOW = "0";
    public static final String VISIBLE_HIDE = "1";
}
