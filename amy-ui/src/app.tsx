import type {RunTimeLayoutConfig, RequestConfig} from '@umijs/max';
import {history} from '@umijs/max';
import {message as antdMessage} from 'antd';
import React from 'react';
import AvatarDropdown from '@/components/RightContent/AvatarDropdown';
import {SystemThemeProvider, fetchSystemStyle} from '@/theme/SystemStyle';
import type {SystemStyle} from '@/theme/SystemStyle';
import {renderIcon} from '@/components/IconSelect';
import type {BackendRoute, CurrentUser} from '@/types/api';
import {getRouters, getUserInfo} from '@/services/auth';
import {clearSession, getToken} from '@/utils/auth';

export interface AmyInitialState {
    currentUser?: CurrentUser;
    roles: string[];
    permissions: string[];
    routes: BackendRoute[];
    /** 系统整体样式（主题色 / 链接色 / 圆角 / 组件尺寸），登录态就绪后携带 */
    systemStyle?: SystemStyle;
}

export async function getInitialState(): Promise<AmyInitialState> {
    if (!getToken()) {
        return {roles: [], permissions: [], routes: []};
    }

    try {
        const [userInfoResult, routeResult, styleResult] = await Promise.all([
            getUserInfo(),
            getRouters(),
            fetchSystemStyle(),
        ]);
        const routes = routeResult.data;
        return {
            currentUser: userInfoResult.user,
            roles: userInfoResult.roles || [],
            permissions: userInfoResult.permissions || [],
            routes: routes || [],
            systemStyle: styleResult,
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
            if (body && typeof body === 'object' && typeof body.code === 'number' && body.code !== 200 && body.code !== 0) {
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
            // 业务异常：后端以 HttpStatus.BUSINESS_ERROR(500) 返回 {code,msg} 结构，
            // 会被 umi 默认 validateStatus 当作 HTTP 错误、绕过响应拦截器，故在此补提示。
            const data = (error as { data?: { code?: number; msg?: string } })?.data
                ?? (error as { response?: { data?: { code?: number; msg?: string } } })?.response?.data;
            if (data && typeof data.msg === 'string' && data.msg) {
                antdMessage.error(data.msg);
            } else if (error.name !== 'RuoYiBusinessError') {
                console.error(error);
            }
            throw error;
        }
    }
};

export const layout: RunTimeLayoutConfig = ({initialState}) => ({
    title: '太阳宫艺术空间',
    logo: false,
    fixedHeader: true,
    fixSiderbar: true,
    layout: 'mix',
    contentWidth: 'Fluid',
    rightContentRender: () => <AvatarDropdown currentUser={initialState?.currentUser}/>,
    menuDataRender: (menuData) => {
        const backend = initialState?.routes;
        const source = backend && backend.length ? backend : mapStaticRoutesToSidebar(menuData as unknown as BackendRoute[]);
        // 后端把 sys_menu.visible='1' 映射为 RouterVo.hidden=true（SysMenuServiceImpl.buildMenus）；
        // 参考 ruoyi-ui（permission.GenerateRoutes/SidebarItem.vue）的做法，
        // 递归过滤 hidden 项，避免侧边栏仍然展示标记为隐藏的菜单及其子菜单。
        return filterHiddenRoutes(source).map((item) => toProLayoutMenu(item, '/'));
    },
    /**
     * 这里不覆写 menuItemRender / subMenuItemRender，沿用 Ant Design Pro 官方实现。
     * 官方写法见 src/.umi/plugin-layout/Layout.tsx（menuItemRender）：保留框架渲染好的
     * defaultDom，只在外层包 <Link> 做路由跳转。收起态只有图标、图标居中、悬浮 tooltip、
     * 弹出子菜单显示「图标 + 文字」等行为都由 pro-layout 内部按 defaultDom 的结构控制。
     * 一旦像之前那样整个丢弃 defaultDom 自己渲染，就会丢失它内部的收起态类名
     * （-item-title-collapsed / -item-text-has-icon / hashId），导致上述行为全部失效。
     *
     * 代价：pro-layout 约定只有一级菜单渲染 icon（BaseMenu 内部 hasIcon = level === 0 ...），
     * 二级及更深的子菜单图标不再强行显示——这是 Ant Design Pro 的默认形态。
     */
    onPageChange: () => {
        if (!getToken() && history.location.pathname !== '/login') {
            history.push(`/login?redirect=${encodeURIComponent(history.location.pathname)}`);
        } else if (!hasRoutePermission(history.location.pathname, initialState?.permissions || [])) {
            history.push('/403');
        }
    }
});

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
    '/system/sysstyle': 'system:sysstyle:list',
    '/system/notice': 'system:notice:list',
    '/system/log/operlog': 'system:operlog:list',
    '/system/log/logininfor': 'system:logininfor:list',
    '/sunpalaceartspace/project': 'sunpalaceartspace:project:list',
    '/monitor/job': 'monitor:job:list',
    '/monitor/job-log': 'monitor:job:list',
    '/monitor/online': 'monitor:online:list'
};

function hasRoutePermission(path: string, permissions: string[]) {
    const permission = ROUTE_PERMISSIONS[path];
    return !permission || permissions.includes('*:*:*') || permissions.includes(permission);
}

export function rootContainer(container: React.ReactNode) {
    // 全局主题：用 SystemThemeProvider 包裹整个应用，使所有 antd / Pro 组件统一应用
    // 系统整体样式（主色 / 圆角 / 组件尺寸等）。Provider 内部已包含 antd 的 <App>，
    // 因此这里不再单独包一层 <App>。
    return <SystemThemeProvider>{container}</SystemThemeProvider>;
}

declare global {
    namespace App {
        interface InitialState extends AmyInitialState {
        }
    }
}
