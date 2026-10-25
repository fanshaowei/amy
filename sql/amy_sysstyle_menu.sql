-- ----------------------------
-- 初始化-系统样式菜单（挂在「系统管理」目录下）
-- 功能：amy-ui 新增「系统样式设置」页，用于配置系统整体主题色 / 链接色 / 圆角 / 组件尺寸，
--       配置持久化于 sys_config 表（config_key = sys.style.settings，config_value 为 JSON）。
-- 说明：admin 角色在 SysMenuServiceImpl.getRouters 中会被特殊放行返回全部菜单，
--       因此超级管理员无需额外关联 sys_role_menu 即可看到本菜单；
--       其余角色如需看到，请自行在「角色管理」里分配 menu_id = 117。
-- ----------------------------
insert into sys_menu values('117', '系统样式', '1', '10', 'sysstyle', 'system/sysstyle/index', '', '', 1, 0, 'C', '0', '0', 'system:sysstyle:list', 'ControlOutlined', 'admin', sysdate(), '', null, '系统样式菜单');
-- 按钮权限（保存配置）
insert into sys_menu values('1034', '样式修改', '117', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'system:sysstyle:edit', '#', 'admin', sysdate(), '', null, '');

-- 如需初始化一条默认样式配置（可选，不插入也会使用前端默认值 #4f7d6d / 圆角6 / 默认尺寸）：
-- insert into sys_config values(?, '系统整体样式', 'sys.style.settings', '{"colorPrimary":"#4f7d6d","colorLink":"#4f7d6d","borderRadius":6,"componentSize":"middle"}', 'N', 'admin', sysdate(), '', null, '系统整体样式配置（主题色/链接色/圆角/组件尺寸）');
