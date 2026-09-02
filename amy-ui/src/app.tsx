import {LogoutOutlined, UserOutlined} from '@ant-design/icons';
import type {RunTimeLayoutConfig, RequestConfig} from '@umijs/max';
import {history} from '@umijs/max';
import {App, Avatar, Dropdown} from 'antd';
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
    menuDataRender: (menuData) => menuData.map((item) => item.path === '/system' ? {
        ...item,
        children: item.children?.filter((child) => !child.path || hasRoutePermission(child.path, initialState?.permissions || []))
    } : item),
    onPageChange: () => {
        if (!getToken() && history.location.pathname !== '/login') {
            history.push(`/login?redirect=${encodeURIComponent(history.location.pathname)}`);
        } else if (!hasRoutePermission(history.location.pathname, initialState?.permissions || [])) {
            history.push('/403');
        }
    }
});

const ROUTE_PERMISSIONS: Record<string, string> = {
    '/system/user': 'system:user:list',
    '/system/role': 'system:role:list',
    '/system/menu': 'system:menu:list',
    '/system/dept': 'system:dept:list',
    '/system/post': 'system:post:list',
    '/system/dict': 'system:dict:list',
    '/system/config': 'system:config:list',
    '/system/notice': 'system:notice:list',
    '/system/operlog': 'system:operlog:list',
    '/system/logininfor': 'system:logininfor:list'
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
