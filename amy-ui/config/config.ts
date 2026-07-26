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
        { path: '/system/role', name: '角色管理', component: '@/pages/System/Role' }
      ]
    },
    { path: '*', component: '@/pages/404' }
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
