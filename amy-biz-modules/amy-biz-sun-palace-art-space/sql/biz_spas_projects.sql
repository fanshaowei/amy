-- ----------------------------
-- 项目管理表 biz_spas_projects
-- 功能：管理每个项目的预约信息（项目名称、封面、预约时间规则、预约须知等）
-- ----------------------------
drop table if exists biz_spas_projects;
create table biz_spas_projects (
  project_id                 bigint(20)      not null auto_increment      comment '项目ID',
  project_name               varchar(100)    not null                     comment '项目名称',
  cover_art_url              varchar(255)    not null                     comment '封面图URL（200*200）',
  cut_off_time               char(8)         default null                 comment '每日可预约截止时间（HH:mm:ss），过了该时间只能预约第二天的',
  reservation_stay_second    int(4)          default null                 comment '预约停留秒数（用户在预约界面可停留的时长）',
  reservation_start_time     char(8)         default null                 comment '预约开始时间（HH:mm:ss）',
  reservation_end_time       char(8)         default null                 comment '预约结束时间（HH:mm:ss）',
  reservation_interval_second int(4)         default null                 comment '预约间隔',
  reservation_count          int(4)          default null                 comment '预约人数（每个时间段可预约的人数）',
  advance_reservation_days   int(4)          default null                 comment '可提前预约天数',
  traveler_count             int(4)          default null                 comment '随行人数',
  reservation_notes          text                                         comment '预约须知（富文本）',
  sort                       int(4)          default 0                    comment '排序',
  status                     char(1)         default '0'                 comment '状态（0正常/启用 1停用）',
  create_by                  varchar(64)     default ''                   comment '创建者',
  create_time                datetime                                     comment '创建时间',
  update_by                  varchar(64)     default ''                   comment '更新者',
  update_time                datetime                                     comment '更新时间',
  remark                     varchar(500)    default null                 comment '备注',
  del_flag                   char(1)         default '0'                 comment '删除标志（0代表存在 1代表删除）',
  primary key (project_id),
  key idx_project_name (project_name)
) engine=innodb auto_increment=100 comment='项目管理表';

-- ----------------------------
-- 项目管理菜单（sys_menu）
-- 注意：parent_id 默认挂到 0（顶级），如需挂到已有目录请修改 @parentId 的赋值，
--       例如：select @parentId := <目录menu_id>;
-- ----------------------------
insert into sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
values ('项目管理', 0, 1, 'project', 'sunpalaceartspace/project/index', '', '', 1, 0, 'C', '0', '0', 'sunpalaceartspace:project:list', '#', 'admin', sysdate(), '项目管理菜单');

select @parentId := last_insert_id();

insert into sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
values
('项目管理查询', @parentId, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'sunpalaceartspace:project:query',  '#', 'admin', sysdate(), ''),
('项目管理新增', @parentId, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'sunpalaceartspace:project:add',    '#', 'admin', sysdate(), ''),
('项目管理修改', @parentId, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'sunpalaceartspace:project:edit',   '#', 'admin', sysdate(), ''),
('项目管理删除', @parentId, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'sunpalaceartspace:project:remove', '#', 'admin', sysdate(), ''),
('项目管理导出', @parentId, 5, '#', '', '', '', 1, 0, 'F', '0', '0', 'sunpalaceartspace:project:export', '#', 'admin', sysdate(), '');
