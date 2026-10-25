import {
    createContext,
    useContext,
    useMemo,
    useSyncExternalStore,
    type ReactNode,
} from 'react';
import { App, ConfigProvider, theme } from 'antd';
import { addConfig, listConfigs, updateConfig } from '@/services/system/config';

/**
 * 系统整体样式配置。
 * 持久化于 sys_config 表（config_key = sys.style.settings，config_value 为 JSON 字符串），
 * 与 RuoYi「参数设置」共用同一张表，无需新增表结构。
 */
export interface SystemStyle {
    /** 主题主色：按钮、选中态、聚焦边框、进度条等 */
    colorPrimary: string;
    /** 链接文字颜色（含页面里的原生 <a> 标签） */
    colorLink: string;
    /** 全局圆角（px） */
    borderRadius: number;
    /** 组件尺寸 */
    componentSize: 'small' | 'middle' | 'large';
    /** 导航模式：side=侧边菜单 / top=顶部菜单 / mix=混合菜单 */
    layout: 'side' | 'top' | 'mix';
    /** 侧边菜单类型：dark=暗色侧栏 / light=亮色侧栏 */
    navTheme: 'light' | 'dark';
}

/** sys_config 中保存整体样式的配置键 */
export const STYLE_CONFIG_KEY = 'sys.style.settings';

/** 默认样式：主色沿用登录页水墨绿 #4f7d6d，避免视觉突变 */
export const DEFAULT_SYSTEM_STYLE: SystemStyle = {
    colorPrimary: '#4f7d6d',
    colorLink: '#4f7d6d',
    borderRadius: 6,
    componentSize: 'middle',
    layout: 'mix',
    navTheme: 'dark',
};

interface SystemThemeContextValue {
    style: SystemStyle;
    /** 仅更新内存中的样式（立即全局生效，不落库），多用于页面内实时预览 */
    setStyle: (style: SystemStyle) => void;
    /** 更新样式并持久化到 sys_config 表 */
    saveStyle: (style: SystemStyle) => Promise<void>;
}

const SystemThemeContext = createContext<SystemThemeContextValue>({
    style: DEFAULT_SYSTEM_STYLE,
    setStyle: () => {},
    saveStyle: async () => {},
});

/**
 * 模块级外部 store。
 *
 * 为什么用外部 store 而不是 useModel('@@initialState')：
 * app.tsx 的 rootContainer 用 SystemThemeProvider 包裹的是 umi 传进来的 `container`，
 * 而 ModelProvider / InitialStateProvider 是在 `container` 内部（更内层）的。
 * 因此在 rootContainer 这一层直接调用 useModel 会拿到 null 的 dispatcher 而崩溃。
 * 外部 store + useSyncExternalStore 不依赖任何 React 上下文，可在该层安全订阅，
 * 并由 getInitialState（含刷新页面）和登录成功 setInitialState 主动写入，实现响应式还原。
 */
let currentStyle: SystemStyle = DEFAULT_SYSTEM_STYLE;
const listeners = new Set<() => void>();

export function getSystemStyle(): SystemStyle {
    return currentStyle;
}

export function subscribeSystemStyle(listener: () => void): () => void {
    listeners.add(listener);
    return () => {
        listeners.delete(listener);
    };
}

/** 写入外部 store 并通知所有订阅者（用于预览 / 登录态就绪 / 刷新还原） */
export function setSystemStyleExternal(style: SystemStyle): void {
    currentStyle = style;
    listeners.forEach((l) => l());
}

/**
 * 逐字段显式取值（而非整体展开）并逐个校验类型：
 * 单个字段被改坏只回退该字段，不污染整套样式；同时避免库里的脏字段被整体展开进配置。
 */
function parseStyle(value: unknown): SystemStyle {
    if (typeof value !== 'string' || !value) return DEFAULT_SYSTEM_STYLE;
    try {
        const parsed = JSON.parse(value) as Partial<SystemStyle>;
        const size = parsed.componentSize;
        const mode = parsed.layout;
        const nav = parsed.navTheme;
        return {
            colorPrimary:
                typeof parsed.colorPrimary === 'string' && parsed.colorPrimary
                    ? parsed.colorPrimary
                    : DEFAULT_SYSTEM_STYLE.colorPrimary,
            colorLink:
                typeof parsed.colorLink === 'string' && parsed.colorLink
                    ? parsed.colorLink
                    : DEFAULT_SYSTEM_STYLE.colorLink,
            borderRadius:
                typeof parsed.borderRadius === 'number'
                    ? parsed.borderRadius
                    : DEFAULT_SYSTEM_STYLE.borderRadius,
            componentSize:
                size === 'small' || size === 'middle' || size === 'large'
                    ? size
                    : DEFAULT_SYSTEM_STYLE.componentSize,
            layout:
                mode === 'side' || mode === 'top' || mode === 'mix'
                    ? mode
                    : DEFAULT_SYSTEM_STYLE.layout,
            navTheme:
                nav === 'light' || nav === 'dark' ? nav : DEFAULT_SYSTEM_STYLE.navTheme,
        };
    } catch {
        return DEFAULT_SYSTEM_STYLE;
    }
}

/**
 * 从 sys_config 读取系统样式。
 * 用 listConfigs 按 config_key 过滤（like 精确命中），而不是 /configKey/{key} 路径变量——
 * 后者 key 含点号（sys.style.settings），在 Spring 路径变量里可能被后缀匹配截断。
 * 未配置 / 无权限 / 网络异常时回退默认值，保证页面与主题永远可用。
 */
export async function fetchSystemStyle(): Promise<SystemStyle> {
    try {
        const list = await listConfigs({ configKey: STYLE_CONFIG_KEY, pageSize: 10, pageNum: 1 });
        const row = list.rows.find((r) => r.configKey === STYLE_CONFIG_KEY);
        if (row?.configValue) return parseStyle(row.configValue);
    } catch {
        // 忽略：使用默认样式
    }
    return DEFAULT_SYSTEM_STYLE;
}

/** 按 config_key upsert 一条 sys_config 记录（存在则改、不存在则增） */
async function persistStyle(style: SystemStyle): Promise<void> {
    const value = JSON.stringify(style);
    const list = await listConfigs({ configKey: STYLE_CONFIG_KEY, pageSize: 10, pageNum: 1 });
    const exist = list.rows.find((r) => r.configKey === STYLE_CONFIG_KEY);
    if (exist?.configId) {
        await updateConfig({ ...exist, configValue: value });
    } else {
        await addConfig({
            configName: '系统整体样式',
            configKey: STYLE_CONFIG_KEY,
            configValue: value,
            configType: 'N',
            remark: '系统整体样式配置（主题色/圆角/组件尺寸/导航模式/侧边菜单类型）',
        });
    }
}

/**
 * 让原生 <a> 标签也跟随系统配置的链接色。
 *
 * ConfigProvider 的 token 只作用于 antd 组件；页面里手写的原生 <a>
 * （如登录页「忘记密码 ?」）拿不到 colorLink，会退回浏览器默认蓝色，
 * 因此这里读取 antd 计算后的链接 token，注入一条全局 CSS 变量 + a 规则统一收口。
 */
function NativeLinkStyle() {
    const { token } = theme.useToken();
    return (
        <style>{`
:root {
  --amy-color-link: ${token.colorLink};
  --amy-color-link-hover: ${token.colorLinkHover};
  --amy-color-link-active: ${token.colorLinkActive};
}
a { color: ${token.colorLink}; }
a:hover { color: ${token.colorLinkHover}; }
a:active { color: ${token.colorLinkActive}; }
`}</style>
    );
}

/**
 * 全局主题提供者：包裹 antd 的 ConfigProvider + App，使整个 Amy 后台（含登录页、
 * 各业务页、ProTable 等所有 antd / Pro 组件）统一应用同一套主题 token。
 *
 * 样式来源统一由模块级外部 store 驱动（useSyncExternalStore），
 * 而 store 在 getInitialState / 登录成功 setInitialState 时都会写入最新样式，
 * 因此「保存后刷新或重新登录」都能正确还原，不会再回退默认绿色。
 */
export function SystemThemeProvider({ children }: { children: ReactNode }) {
    const style = useSyncExternalStore(subscribeSystemStyle, getSystemStyle);

    const setStyle = useMemo(() => (s: SystemStyle) => setSystemStyleExternal(s), []);
    const saveStyle = useMemo(
        () => async (s: SystemStyle) => {
            setSystemStyleExternal(s);
            await persistStyle(s);
        },
        []
    );

    const themeConfig = useMemo(
        () => ({
            token: {
                colorPrimary: style.colorPrimary,
                // 注意：antd 的 colorLink 默认是 colorInfo（固定蓝 #1677ff），并不会跟随主色，
                // 所以这里必须显式传入配置的链接色，否则链接色设置不生效。
                colorLink: style.colorLink,
                borderRadius: style.borderRadius,
            },
        }),
        [style]
    );

    return (
        <SystemThemeContext.Provider value={{ style, setStyle, saveStyle }}>
            <ConfigProvider theme={themeConfig} componentSize={style.componentSize}>
                <NativeLinkStyle />
                <App>{children}</App>
            </ConfigProvider>
        </SystemThemeContext.Provider>
    );
}

/** 在页面 / 组件中获取并修改系统整体样式 */
export function useSystemTheme(): SystemThemeContextValue {
    return useContext(SystemThemeContext);
}
