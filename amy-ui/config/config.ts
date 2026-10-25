import { defineConfig } from '@umijs/max';

export default defineConfig({
  antd: {},
  access: {},
  initialState: {},
  model: {},
  request: {},
  layout: {},
  // 修复 max build 在压缩阶段报 "Found conflicts in esbuild helpers" 的问题：
  // 多个 chunk 的 esbuild IIFE helper 变量名（Xc/Qc）冲突，开启此选项让 esbuild
  // 在 minify 时对 helper 做 IIFE 包裹，消除冲突。源码编译本身无问题。
  esbuildMinifyIIFE: true,
  hash: true,
  history: { type: 'hash' },
  npmClient: 'npm',
  title: '太阳宫艺术空间',
  routes: [
    { path: '/login', layout: false, component: '@/pages/Login' },
    { path: '/', redirect: '/welcome' },
    { path: '/welcome', name: '首页', icon: 'DashboardOutlined', component: '@/pages/Welcome' },
    // 目录级菜单（只有子路由、本身没有页面组件）必须配重定向：
    // 否则访问父路径时 umi 只渲染空的路由出口 → 白屏，且不会落到 '*' 占位页。
    // 注意 umi 4 会**丢弃**「父路由 + routes」上的 redirect（生成的 route.tsx 里没有），
    // 因此这里按 umi 支持的写法：把「父路径 + redirect」作为第一个子路由（索引路由），
    // 并 hideInMenu 避免在菜单里多出一个同名空项。
    {
      path: '/system',
      name: '系统管理',
      icon: 'SettingOutlined',
      routes: [
        { path: '/system', redirect: '/system/user', hideInMenu: true },
        { path: '/system/user', name: '用户管理', component: '@/pages/System/User' },
        { path: '/system/role', name: '角色管理', component: '@/pages/System/Role' },
        { path: '/system/menu', name: '菜单管理', component: '@/pages/System/Menu' },
        { path: '/system/dept', name: '部门管理', component: '@/pages/System/Dept' },
        { path: '/system/post', name: '岗位管理', component: '@/pages/System/Post' },
        { path: '/system/dict', name: '字典管理', component: '@/pages/System/Dict' },
        { path: '/system/config', name: '参数设置', component: '@/pages/System/Config' },
        { path: '/system/sysstyle', name: '系统样式', component: '@/pages/System/SysStyle' },
        { path: '/system/notice', name: '通知公告', component: '@/pages/System/Notice' },
        {
          path: '/system/log',
          name: '日志管理',
          routes: [
            { path: '/system/log', redirect: '/system/log/operlog', hideInMenu: true },
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
        { path: '/monitor', redirect: '/monitor/job', hideInMenu: true },
        { path: '/monitor/job', name: '定时任务', component: '@/pages/Monitor/Job' },
        { path: '/monitor/job-log', name: '任务日志', component: '@/pages/Monitor/Job/log', hideInMenu: true },
        { path: '/monitor/operlog', name: '操作日志', component: '@/pages/PagePlaceholder' },
        { path: '/monitor/logininfor', name: '登录日志', component: '@/pages/PagePlaceholder' },
        { path: '/monitor/online', name: '在线用户', component: '@/pages/Monitor/Online' },
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
        { path: '/tool', redirect: '/tool/build', hideInMenu: true },
        { path: '/tool/build', name: '表单构建', component: '@/pages/PagePlaceholder' },
        { path: '/tool/gen', name: '代码生成', component: '@/pages/PagePlaceholder' },
        { path: '/tool/swagger', name: '系统接口', component: '@/pages/PagePlaceholder' }
      ]
    },
    {
      path: '/sunpalaceartspace',
      name: '艺术空间',
      icon: 'ProjectOutlined',
      routes: [
        { path: '/sunpalaceartspace', redirect: '/sunpalaceartspace/project', hideInMenu: true },
        { path: '/sunpalaceartspace/project', name: '项目管理', component: '@/pages/sunpalaceartspace/project' },
        { path: '/sunpalaceartspace/order', name: '预约订单', component: '@/pages/sunpalaceartspace/order' },
        { path: '/sunpalaceartspace/reservationUser', name: '会员列表', component: '@/pages/sunpalaceartspace/reservationUser' }
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
