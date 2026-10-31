-- =====================================================================
-- 项目预约时段配置（按「项目 + 时段」配置每天通用的开放规则）
--
-- 业务口径：
--   1) 本表【不区分日期】，一条配置对该项目的【每一天】都生效，长期有效；
--      不需要也不应该为每一天各插一条记录。
--   2) 项目自身的 reservation_start_time ~ reservation_end_time 是「默认开放时段」，
--      没有配置命中的时段，全部照默认规则开放；
--   3) 配置了时段的：
--        reservation_enabled = 1 → 该时段开放
--        reservation_enabled = 0 → 该时段关闭（黑名单）
--   4) 同一时段同时命中开放/关闭两条记录时，按【关闭优先】（安全默认）。
--
-- 优先级：不可预约(false) > 可预约(true) > 无配置(按项目默认规则)
--
-- 放号链路：ReservationRulesService#release 每天放号时读取本表，
--           只对「判定为开放」的时段建库存行并放号，被关闭的时段不放号。
-- =====================================================================

-- ---------------------------- 1. 建表 ----------------------------
CREATE TABLE `biz_spas_project_reservation_config` (
    `config_id`           bigint      NOT NULL AUTO_INCREMENT COMMENT '配置ID',
    `project_id`          bigint      NOT NULL COMMENT '项目ID',
    `start_time`          char(8)     NOT NULL COMMENT '时段开始时间（HH:mm:ss），每天生效',
    `end_time`            char(8)     NOT NULL COMMENT '时段结束时间（HH:mm:ss，不含，左闭右开）',
    `reservation_enabled` tinyint(1)  NOT NULL DEFAULT 1 COMMENT '是否可预约（1=可预约 0=不可预约）',
    `del_flag`            char(1)     NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
    `create_by`           varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
    `create_time`         datetime    DEFAULT NULL COMMENT '创建时间',
    `update_by`           varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
    `update_time`         datetime    DEFAULT NULL COMMENT '更新时间',
    `remark`              varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`config_id`),
    -- 唯一键：同一项目、同一时段只能有一条配置（不区分日期，因为本表就是每天通用的）。
    --   必须带 project_id：否则项目A配了 09:00~10:00，项目B就永远配不了同一时段。
    --   末尾带 del_flag 是为了兼容软删除，见下方「软删除与唯一键」说明。
    UNIQUE KEY `uk_project_time` (`project_id`, `start_time`, `end_time`, `del_flag`),
    -- 时段本身必须左闭右开且非空；HH:mm:ss 定长零填充，字典序即时间序
    CONSTRAINT `chk_config_time_range` CHECK (`start_time` < `end_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='项目预约时段配置表';

-- -------------------- 软删除与唯一键（重要） --------------------
-- 表沿用 BaseEntity 的 @TableLogic(delval='1') 软删除，直接在 (project_id, start, end) 上加唯一键会出事：
--   1. 插入 A(del_flag=0)
--   2. 删除 A → A(del_flag=1)
--   3. 重新插入同键 B(del_flag=0)   → OK
--   4. 再删除 B → B(del_flag=1)     → 与 A 同键同 del_flag，1062 Duplicate entry，删除失败
-- 解法：唯一键末列带上 del_flag，并且每次执行删除前先物理清掉 del_flag='1' 的历史行
--      （ProjectReservationConfigServiceImpl#removeConfigByIds 调用 mapper.purgeDeleted()）。
--      已删除的行不参与任何查询，物理清理无副作用；这样任意次「删除→重建」都不会撞键。
--
-- 关于 end_time 要不要进唯一键：
--   现在这条键允许「同起点、不同终点」两条并存，例如 09:00~10:00 关闭 + 09:00~12:00 开放，
--   判定上按【关闭优先】不会出错，但列表里看着矛盾，且通常是误操作。
--   若想严格到「同一开始时间只能有一条」，把唯一键改成 3 列即可：
--     UNIQUE KEY `uk_project_time` (`project_id`, `start_time`, `del_flag`)
--   Service 层 validateConfig() 目前按【project_id + start_time】校验（比唯一键更严），
--   即业务入口已按 3 列口径拦截，唯一键只作并发/直接写库的兜底。

-- ---------------------------- 2. 菜单 ----------------------------
-- 挂在「艺术空间」(menu_id=2001) 目录下。已占用 id：2001~2012，故从 2013 起。
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2013, '预约时段配置', 2001, 4, 'reservationConfig', 'sunpalaceartspace/reservationConfig/index', '', '', 1, 0, 'C', '0', '0', 'sunpalaceartspace:reservationConfig:list', 'CalendarOutlined', 'admin', sysdate(), '', NULL, '项目预约时段配置菜单');

-- ---------------------------- 3. 按钮/接口权限 ----------------------------
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES
    (2014, '预约时段配置查询', 2013, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'sunpalaceartspace:reservationConfig:query',  '#', 'admin', sysdate(), '', NULL, ''),
    (2015, '预约时段配置新增', 2013, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'sunpalaceartspace:reservationConfig:add',    '#', 'admin', sysdate(), '', NULL, ''),
    (2016, '预约时段配置修改', 2013, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'sunpalaceartspace:reservationConfig:edit',   '#', 'admin', sysdate(), '', NULL, ''),
    (2017, '预约时段配置删除', 2013, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'sunpalaceartspace:reservationConfig:remove', '#', 'admin', sysdate(), '', NULL, ''),
    (2018, '预约时段配置导出', 2013, 5, '#', '', '', '', 1, 0, 'F', '0', '0', 'sunpalaceartspace:reservationConfig:export', '#', 'admin', sysdate(), '', NULL, '');

-- admin 角色在 getRouters 中是特例（返回全部菜单），无需额外插入 sys_role_menu；
-- 非管理员角色请在「角色管理」里勾选「预约时段配置」及其按钮权限。
