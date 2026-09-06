import { defineConfig } from '@umijs/max';

export default defineConfig({
  antd: {},
  access: {},
  initialState: {},
  model: {},
  request: {},
  layout: {},
  hash: true,
  history: { type: 'hash' },
  npmClient: 'npm',
  title: '若依管理系统',
  routes: [
    { path: '/login', layout: false, component: '@/pages/Login' },
    { path: '/', redirect: '/welcome' },
    { path: '/welcome', name: '首页', icon: 'DashboardOutlined', component: '@/pages/Welcome' },
    {
      path: '/system',
      name: '系统管理',
      icon: 'SettingOutlined',
      routes: [
        { path: '/system/user', name: '用户管理', component: '@/pages/System/User' },
        { path: '/system/role', name: '角色管理', component: '@/pages/System/Role' },
        { path: '/system/menu', name: '菜单管理', component: '@/pages/System/Menu' },
        { path: '/system/dept', name: '部门管理', component: '@/pages/System/Dept' },
        { path: '/system/post', name: '岗位管理', component: '@/pages/System/Post' },
        { path: '/system/dict', name: '字典管理', component: '@/pages/System/Dict' },
        { path: '/system/config', name: '参数设置', component: '@/pages/System/Config' },
        { path: '/system/notice', name: '通知公告', component: '@/pages/System/Notice' },
        {
          path: '/system/log',
          name: '日志管理',
          routes: [
            { path: '/system/log/operlog', name: '操作日志', component: '@/pages/System/Operlog' },
            { path: '/system/log/logininfor', name: '登录日志', component: '@/pages/System/Logininfor' }
          ]
        }
      ]
    },
    {
      path: '/monitor',
      name: '监控管理',
      icon: 'EyeOutlined',
      routes: [
        { path: '/monitor/job', name: '定时任务', component: '@/pages/Monitor/Job' },
        { path: '/monitor/job-log', name: '任务日志', component: '@/pages/Monitor/Job/log', hideInMenu: true },
        { path: '/monitor/operlog', name: '操作日志', component: '@/pages/PagePlaceholder' },
        { path: '/monitor/logininfor', name: '登录日志', component: '@/pages/PagePlaceholder' },
        { path: '/monitor/online', name: '在线用户', component: '@/pages/PagePlaceholder' },
        { path: '/monitor/cache', name: '缓存监控', component: '@/pages/PagePlaceholder' },
        { path: '/monitor/druid', name: '数据源', component: '@/pages/PagePlaceholder' },
        { path: '/monitor/server', name: '服务监控', component: '@/pages/PagePlaceholder' }
      ]
    },
    {
      path: '/tool',
      name: '系统工具',
      icon: 'ToolOutlined',
      routes: [
        { path: '/tool/build', name: '表单构建', component: '@/pages/PagePlaceholder' },
        { path: '/tool/gen', name: '代码生成', component: '@/pages/PagePlaceholder' },
        { path: '/tool/swagger', name: '系统接口', component: '@/pages/PagePlaceholder' }
      ]
    },
    { path: '/system/user/profile', name: '个人中心', hideInMenu: true, component: '@/pages/System/Profile' },
    { path: '/403', layout: false, component: '@/pages/403' },
    { path: '*', component: '@/pages/PagePlaceholder' }
  ],
  proxy: {
    '/dev-api': {
      target: 'http://localhost:18080',
      changeOrigin: true,
      pathRewrite: { '^/dev-api': '' }
    }
  },
  define: {
    API_BASE_URL: process.env.API_BASE_URL || '/dev-api'
  }
});
