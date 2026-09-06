import {LogoutOutlined, UserOutlined} from '@ant-design/icons';
import type {RunTimeLayoutConfig, RequestConfig} from '@umijs/max';
import {history, Link} from '@umijs/max';
import {App, Avatar, Dropdown, message as antdMessage} from 'antd';
import React from 'react';
import {renderIcon} from '@/components/IconSelect';
import type {BackendRoute, CurrentUser} from '@/types/api';
import {getRouters, getUserInfo, logout} from '@/services/auth';
import {clearSession, getToken} from '@/utils/auth';

export interface AmyInitialState {
    currentUser?: CurrentUser;
    roles: string[];
    permissions: string[];
    routes: BackendRoute[];
}

export async function getInitialState(): Promise<AmyInitialState> {
    if (!getToken()) {
        return {roles: [], permissions: [], routes: []};
    }

    try {
        const [userInfoResult, routeResult] = await Promise.all([getUserInfo(), getRouters()]);
        const routes = routeResult.data;
        return {
            currentUser: userInfoResult.user,
            roles: userInfoResult.roles || [],
            permissions: userInfoResult.permissions || [],
            routes: routes || []
        };
    } catch {
        clearSession();
        return {roles: [], permissions: [], routes: []};
    }
}

// axios 0.x 默认会把嵌套对象 JSON.stringify（如 params=%7B"beginTime":...%7D），
// RuoYi 后端要求的是 params[beginTime]=...&params[endTime]=...（绑定到 BaseEntity.params），
// 因此这里自定义序列化：嵌套对象展开为 bracket 记法。
function serializeParams(params: Record<string, unknown>): string {
    const parts: string[] = [];
    const append = (key: string, value: unknown) => {
        parts.push(`${encodeURIComponent(key)}=${encodeURIComponent(String(value))}`);
    };
    Object.entries(params).forEach(([key, value]) => {
        if (value === null || value === undefined) return;
        if (Array.isArray(value)) {
            value.forEach((item) => append(`${key}[]`, item));
        } else if (value instanceof Date) {
            append(key, value.toISOString());
        } else if (typeof value === 'object') {
            Object.entries(value as Record<string, unknown>).forEach(([subKey, subValue]) => {
                if (subValue === null || subValue === undefined) return;
                append(`${key}[${subKey}]`, subValue);
            });
        } else {
            append(key, value);
        }
    });
    return parts.join('&');
}

export const request: RequestConfig = {
    baseURL: API_BASE_URL,
    timeout: 10000,
    paramsSerializer: serializeParams,
    requestInterceptors: [
        (url, options) => {
            const token = getToken();
            const requiresToken = options.headers?.isToken !== 'false';
            const headers = {...options.headers};
            delete headers.isToken;
            delete headers.repeatSubmit;
            if (token && requiresToken) headers.Authorization = `Bearer ${token}`;
            return {url, options: {...options, headers}};
        }
    ],
    responseInterceptors: [
        async (response) => {
            if (response.status === 401) {
                clearSession();
                history.push(`/login?redirect=${encodeURIComponent(history.location.pathname)}`);
                return Promise.reject(new Error('未登录'));
            }
            // RuoYi 业务错误：code 非 200（如 601 菜单已分配不允许删除、500 通用失败等）。
            // umi-request 默认只对 data.success === false 触发 errorThrower，
            // RuoYi 后端无该字段，所以这里手动拦截并提示错误 + reject。
            const body = response.data as { code?: number; msg?: string } | undefined;
            if (body && typeof body === 'object' && typeof body.code === 'number' && body.code !== 200) {
                const errorMsg = body.msg || '请求失败';
                antdMessage.error(errorMsg);
                return Promise.reject(new Error(errorMsg));
            }
            return response;
        }
    ],
    errorConfig: {
        errorThrower: (response) => {
            const body = response as { code?: number; msg?: string };
            if (body.code && body.code !== 200) {
                const error = new Error(body.msg || '请求失败');
                error.name = 'RuoYiBusinessError';
                throw error;
            }
        },
        errorHandler: (error) => {
            if (error.name !== 'RuoYiBusinessError') {
                console.error(error);
            }
            throw error;
        }
    }
};

const UserMenu = ({currentUser}: { currentUser?: CurrentUser }) => {
    const {message} = App.useApp();

    return (
        <Dropdown
            menu={{
                items: [
                    {key: 'profile', icon: <UserOutlined/>, label: '个人中心'},
                    {type: 'divider'},
                    {key: 'logout', icon: <LogoutOutlined/>, label: '退出登录'}
                ],
                onClick: async ({key}) => {
                    if (key === 'profile') history.push('/system/user/profile');
                    if (key === 'logout') {
                        try {
                            await logout();
                        } finally {
                            clearSession();
                            message.success('已退出登录');
                            history.push('/login');
                        }
                    }
                }
            }}
        >
      <span style={{display: 'inline-flex', alignItems: 'center', gap: 8, cursor: 'pointer'}}>
        <Avatar size="small" src={currentUser?.avatar} icon={<UserOutlined/>}/>
          {currentUser?.nickName || currentUser?.userName}
      </span>
        </Dropdown>
    );
};

export const layout: RunTimeLayoutConfig = ({initialState}) => ({
    title: '若依管理系统',
    logo: false,
    fixedHeader: true,
    fixSiderbar: true,
    layout: 'mix',
    contentWidth: 'Fluid',
    rightContentRender: () => <UserMenu currentUser={initialState?.currentUser}/>,
    menuDataRender: (menuData) => {
        const backend = initialState?.routes;
        const source = backend && backend.length ? backend : mapStaticRoutesToSidebar(menuData as unknown as BackendRoute[]);
        // 后端把 sys_menu.visible='1' 映射为 RouterVo.hidden=true（SysMenuServiceImpl.buildMenus）；
        // 参考 ruoyi-ui（permission.GenerateRoutes/SidebarItem.vue）的做法，
        // 递归过滤 hidden 项，避免侧边栏仍然展示标记为隐藏的菜单及其子菜单。
        return filterHiddenRoutes(source).map((item) => toProLayoutMenu(item, '/'));
    },
    /**
     * pro-layout 的 BaseMenu 内部限制死了图标层级：
     *   `const hasIcon = level === 0 || (isGroup && level === 1);`
     *   `const icon = !hasIcon ? null : getIcon(item.icon);`
     * 也就是只有一级菜单（以及 group 模式下的二级）才会渲染 icon，
     * 后端菜单树里「系统管理(0) → 用户管理(1)」这类二级/三级子菜单的 icon 会被强行丢弃。
     * 所以这里接管 menuItemRender / subMenuItemRender，自己渲染「图标 + 标题」，
     * 使任意层级的菜单都能显示在菜单管理模块里配置的图标。
     */
    menuItemRender: (item, defaultDom, menuProps) => {
        if (item.isUrl || item.children) return defaultDom;
        const label = renderMenuLabel(item as unknown as BackendRoute, menuProps);
        // 复刻 umi 内置 menuItemRender 的跳转行为（我们用自定义实现覆盖了它）
        if (item.path && history.location.pathname !== item.path) {
            return <Link to={(item.path || '').replace('/*', '')} target={item.target}>{label}</Link>;
        }
        return label;
    },
    subMenuItemRender: (item, defaultDom, menuProps) => renderMenuLabel(item as unknown as BackendRoute, menuProps),
    onPageChange: () => {
        if (!getToken() && history.location.pathname !== '/login') {
            history.push(`/login?redirect=${encodeURIComponent(history.location.pathname)}`);
        } else if (!hasRoutePermission(history.location.pathname, initialState?.permissions || [])) {
            history.push('/403');
        }
    }
});

/**
 * 自定义渲染菜单标题（图标 + 文字），绕过 pro-layout 只给一级菜单显示 icon 的限制。
 * 结构完全对齐 BaseMenu 默认生成的 DOM（`-item-title` > `-item-icon` / `-item-text`），
 * 以便复用 pro-layout 自带的排版样式（含收起态）。
 */
function renderMenuLabel(item: BackendRoute, menuProps?: {prefixCls?: string; mode?: string}): React.ReactNode {
    const baseClassName = `${menuProps?.prefixCls || 'ant-pro'}-base-menu-${menuProps?.mode || 'inline'}`;
    const icon = item.icon as React.ReactNode;
    return (
        <span className={`${baseClassName}-item-title`}>
            {icon ? <span className={`${baseClassName}-item-icon`}>{icon}</span> : null}
            <span className={`${baseClassName}-item-text`}>{item.name}</span>
        </span>
    );
}

/** 把 config.ts 里的静态路由转成 pro-layout 的菜单数据格式 */
function mapStaticRoutesToSidebar(routes: BackendRoute[]): BackendRoute[] {
    return filterHiddenRoutes(
        routes
            .filter((r) => r.path !== '/login' && r.path !== '/403' && r.path !== '*' && r.path !== '/')
            .map((r) => ({
                name: r.name || r.meta?.title,
                path: r.path,
                component: r.component,
                redirect: r.redirect,
                hidden: r.hidden,
                meta: r.meta ? {...r.meta, icon: r.meta?.icon} : undefined,
                children: r.children ? mapStaticRoutesToSidebar(r.children) : undefined
            }))
    );
}

/**
 * 递归移除 hidden === true 的菜单项。
 * 后端把 sys_menu.visible='1' 映射为 RouterVo.hidden=true；
 * 即便父菜单隐藏了，也得连带剔除它的子菜单（参考 ruoyi-ui 递归遍历 children 的逻辑），
 * 否则 pro-layout 会保留子菜单但丢失父级标题。
 */
function filterHiddenRoutes(routes: BackendRoute[]): BackendRoute[] {
    return routes
        .filter((r) => !r.hidden)
        .map((r) => ({
            ...r,
            children: r.children ? filterHiddenRoutes(r.children) : undefined
        }));
}

/**
 * 把后端 RouterVo（或静态路由）转成 pro-layout 能正确显示的菜单数据。
 * 后端 buildMenus 把路由名（英文驼峰）放在 `name` 字段、把中文菜单名放在 `meta.title`，
 * 而 pro-layout 的 BaseMenu.getIntlName 只读 `item.name`、不读 `meta.title`，
 * 所以这里把 `name` 重写为 `meta.title`，并把图标从 meta 里提到顶级 icon 字段。
 * 同时把字符串 icon（后端存的 'user'/'system' 等 ruoyi svg 名，或 antd 图标名）解析为 React 元素；
 * 解析不出来时返回 undefined。
 *
 * 路径处理：后端 sys_menu.path 是相对路径（'operlog'、'logininfor'），没有 '/' 前缀；
 * pro-layout 要求完整路径（'/system/operlog'）。这里按 umi 的 mergePath 规则递归拼接父级 path，
 * 否则菜单点击会落到 catch-all '*'，组件渲染失败。
 */
function toProLayoutMenu(item: BackendRoute, parentPath: string): BackendRoute {
    const title = item.meta?.title || item.name || item.path;
    const iconName = (item.icon || item.meta?.icon) as string | undefined;
    const IconComp = iconName ? renderIcon(iconName) : null;
    const mergedPath = mergePath(item.path, parentPath);
    return {
        ...item,
        name: title,
        path: mergedPath,
        icon: IconComp ? React.createElement(IconComp) : undefined,
        children: item.children?.map((c) => toProLayoutMenu(c, mergedPath))
    };
}

/**
 * 模仿 umi route-utils 的 mergePath：相对路径 + 父路径拼成完整路径，绝对路径保持不变。
 */
function mergePath(path: string | undefined, parentPath: string): string {
    if (!path) return parentPath;
    if (path === '/' || path === '*' || path === '/*') return path;
    if (path.startsWith('/')) return path;
    if (/^https?:\/\//.test(path)) return path;
    const parent = parentPath.endsWith('/') ? parentPath.slice(0, -1) : parentPath;
    return `${parent}/${path}`.replace(/\/+/g, '/');
}

const ROUTE_PERMISSIONS: Record<string, string> = {
    '/system/user': 'system:user:list',
    '/system/role': 'system:role:list',
    '/system/menu': 'system:menu:list',
    '/system/dept': 'system:dept:list',
    '/system/post': 'system:post:list',
    '/system/dict': 'system:dict:list',
    '/system/config': 'system:config:list',
    '/system/notice': 'system:notice:list',
    '/system/log/operlog': 'system:operlog:list',
    '/system/log/logininfor': 'system:logininfor:list',
    '/monitor/job': 'monitor:job:list',
    '/monitor/job-log': 'monitor:job:list',
    '/monitor/online': 'monitor:online:list'
};

function hasRoutePermission(path: string, permissions: string[]) {
    const permission = ROUTE_PERMISSIONS[path];
    return !permission || permissions.includes('*:*:*') || permissions.includes(permission);
}

export function rootContainer(container: React.ReactNode) {
    return <App>{container}</App>;
}

declare global {
    namespace App {
        interface InitialState extends AmyInitialState {
        }
    }
}
