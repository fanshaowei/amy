import {
    createContext,
    useCallback,
    useContext,
    useEffect,
    useMemo,
    useState,
    type ReactNode,
} from 'react';
import { useModel } from '@umijs/max';
import { App, ConfigProvider } from 'antd';
import { addConfig, listConfigs, updateConfig } from '@/services/system/config';

/**
 * 系统整体样式配置。
 * 持久化于 sys_config 表（config_key = sys.style.settings，config_value 为 JSON 字符串），
 * 与 RuoYi「参数设置」共用同一张表，无需新增表结构。
 */
export interface SystemStyle {
    /** 主题主色：按钮、链接、选中态、聚焦边框等 */
    colorPrimary: string;
    /** 链接文字颜色 */
    colorLink: string;
    /** 全局圆角（px） */
    borderRadius: number;
    /** 组件尺寸 */
    componentSize: 'small' | 'middle' | 'large';
}

/** sys_config 中保存整体样式的配置键 */
export const STYLE_CONFIG_KEY = 'sys.style.settings';

/** 默认样式：与登录页历史主色（水墨绿 #4f7d6d）保持一致，避免升级后视觉突变 */
export const DEFAULT_SYSTEM_STYLE: SystemStyle = {
    colorPrimary: '#4f7d6d',
    colorLink: '#4f7d6d',
    borderRadius: 6,
    componentSize: 'middle',
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

function parseStyle(value: unknown): SystemStyle {
    if (typeof value !== 'string' || !value) return DEFAULT_SYSTEM_STYLE;
    try {
        const parsed = JSON.parse(value) as Partial<SystemStyle>;
        return { ...DEFAULT_SYSTEM_STYLE, ...parsed };
    } catch {
        return DEFAULT_SYSTEM_STYLE;
    }
}

/**
 * 从 sys_config 读取系统样式。
 * 用 listConfigs 按 config_key 过滤（like 精确命中），而不是 /configKey/{key} 路径变量——
 * 后者 key 含点号（sys.style.settings），在 Spring 路径变量里可能被后缀匹配截断。
 * 未配置 / 无权限 / 网络异常时回退默认值，保证页面与主题永远可用。
 *
 * 导出给 app.tsx 的 getInitialState 复用，使登录态就绪后（含刷新页面）即带上样式，
 * 避免「保存后能看到、重新登录又回到默认」的问题。
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
            remark: '系统整体样式配置（主题色/链接色/圆角/组件尺寸）',
        });
    }
}

/**
 * 把 initialState 里的系统样式同步到全局主题。
 *
 * 为什么要做成「挂在 children 之后的独立子组件」：
 * app.tsx 的 rootContainer 返回的内容整体被 umi 的 InitialStateProvider 包裹，
 * 因此这个子组件（位于 rootContainer 返回的 JSX 树内）必然能 useModel('@@initialState')，
 * 从而在「刷新页面（已登录）」「登录成功（setInitialState）」等场景下自动拿到最新样式。
 * 它把样式写回外层 SystemThemeProvider 暴露的 setStyle，驱动 ConfigProvider 重渲染。
 */
function ThemeSyncFromInitialState() {
    const initialState = useModel('@@initialState');
    const { setStyle } = useSystemTheme();
    const style = (initialState as { systemStyle?: SystemStyle } | undefined)?.systemStyle;

    useEffect(() => {
        if (style) setStyle(style);
    }, [style, setStyle]);

    return null;
}

/**
 * 全局主题提供者：包裹 antd 的 ConfigProvider + App，使整个 Amy 后台（含登录页、
 * 各业务页、ProTable 等所有 antd / Pro 组件）统一应用同一套主题 token。
 *
 * 样式来源统一由 initialState.systemStyle 驱动（见 ThemeSyncFromInitialState），
 * 而 initialState 在 getInitialState / 登录成功 setInitialState 时都会带上最新样式，
 * 因此「保存后刷新或重新登录」都能正确还原，不会再回退默认绿色。
 */
export function SystemThemeProvider({ children }: { children: ReactNode }) {
    const [style, setStyleState] = useState<SystemStyle>(DEFAULT_SYSTEM_STYLE);

    const setStyle = useCallback((s: SystemStyle) => setStyleState(s), []);
    const saveStyle = useCallback(async (s: SystemStyle) => {
        setStyleState(s);
        await persistStyle(s);
    }, []);

    const theme = useMemo(
        () => ({
            token: {
                colorPrimary: style.colorPrimary,
                colorLink: style.colorLink,
                borderRadius: style.borderRadius,
            },
        }),
        [style]
    );

    return (
        <SystemThemeContext.Provider value={{ style, setStyle, saveStyle }}>
            <ConfigProvider theme={theme} componentSize={style.componentSize}>
                <App>
                    {children}
                    <ThemeSyncFromInitialState />
                </App>
            </ConfigProvider>
        </SystemThemeContext.Provider>
    );
}

/** 在页面 / 组件中获取并修改系统整体样式 */
export function useSystemTheme(): SystemThemeContextValue {
    return useContext(SystemThemeContext);
}
