import {
    ApartmentOutlined,
    AppstoreOutlined,
    AuditOutlined,
    BankOutlined,
    BellOutlined,
    BookOutlined,
    BuildOutlined,
    BulbOutlined,
    CalendarOutlined,
    CarOutlined,
    CarryOutOutlined,
    CheckCircleOutlined,
    CheckSquareOutlined,
    ClockCircleOutlined,
    CloseCircleOutlined,
    CloudOutlined,
    CodeOutlined,
    CompassOutlined,
    ContactsOutlined,
    ControlOutlined,
    DashboardOutlined,
    DatabaseOutlined,
    DisconnectOutlined,
    DollarOutlined,
    DownloadOutlined,
    EditOutlined,
    EnvironmentOutlined,
    ExclamationCircleOutlined,
    EyeOutlined,
    FileImageOutlined,
    FileOutlined,
    FileTextOutlined,
    FilterOutlined,
    FlagOutlined,
    FolderOpenOutlined,
    FolderOutlined,
    GiftOutlined,
    GlobalOutlined,
    HeartOutlined,
    HistoryOutlined,
    HomeOutlined,
    IdcardOutlined,
    InfoCircleOutlined,
    KeyOutlined,
    LaptopOutlined,
    LayoutOutlined,
    LinkOutlined,
    LockOutlined,
    MailOutlined,
    ManOutlined,
    MenuOutlined,
    MessageOutlined,
    MobileOutlined,
    NotificationOutlined,
    PaperClipOutlined,
    PhoneOutlined,
    PictureOutlined,
    PieChartOutlined,
    PlayCircleOutlined,
    PoweroffOutlined,
    PrinterOutlined,
    ProfileOutlined,
    ProjectOutlined,
    PushpinOutlined,
    QrcodeOutlined,
    QuestionCircleOutlined,
    ReadOutlined,
    RedoOutlined,
    ReloadOutlined,
    RocketOutlined,
    SafetyCertificateOutlined,
    SafetyOutlined,
    SaveOutlined,
    SearchOutlined,
    SecurityScanOutlined,
    SelectOutlined,
    SettingOutlined,
    ShareAltOutlined,
    ShopOutlined,
    ShoppingCartOutlined,
    SmileOutlined,
    SolutionOutlined,
    StarOutlined,
    StopOutlined,
    SyncOutlined,
    TableOutlined,
    TagOutlined,
    TagsOutlined,
    TeamOutlined,
    ThunderboltOutlined,
    ToolOutlined,
    TrophyOutlined,
    UndoOutlined,
    UnlockOutlined,
    UploadOutlined,
    UserOutlined,
    VideoCameraOutlined,
    WalletOutlined,
    WarningOutlined,
    WifiOutlined,
    WomanOutlined,
    ZoomInOutlined,
    ZoomOutOutlined
} from '@ant-design/icons';
import type {ComponentType} from 'react';
import {Form, Input, Popover, Tag} from 'antd';
import {useMemo, useState} from 'react';

const ICONS: Record<string, ComponentType> = {
    ApartmentOutlined, AppstoreOutlined, AuditOutlined, BankOutlined, BellOutlined, BookOutlined,
    BuildOutlined, BulbOutlined, CalendarOutlined, CarOutlined, CarryOutOutlined,
    CheckCircleOutlined, CheckSquareOutlined, ClockCircleOutlined, CloseCircleOutlined, CloudOutlined,
    CodeOutlined, CompassOutlined, ContactsOutlined, ControlOutlined, DashboardOutlined,
    DatabaseOutlined, DisconnectOutlined, DollarOutlined, DownloadOutlined, EditOutlined,
    EnvironmentOutlined, ExclamationCircleOutlined, EyeOutlined, FileImageOutlined,
    FileOutlined, FileTextOutlined, FilterOutlined, FlagOutlined, FolderOpenOutlined,
    FolderOutlined, GiftOutlined, GlobalOutlined, HeartOutlined, HistoryOutlined, HomeOutlined,
    IdcardOutlined, InfoCircleOutlined, KeyOutlined, LaptopOutlined, LayoutOutlined,
    LinkOutlined, LockOutlined, MailOutlined, ManOutlined, MenuOutlined, MessageOutlined,
    MobileOutlined, NotificationOutlined, PaperClipOutlined, PhoneOutlined, PictureOutlined,
    PieChartOutlined, PlayCircleOutlined, PoweroffOutlined, PrinterOutlined, ProfileOutlined,
    ProjectOutlined, PushpinOutlined, QrcodeOutlined, QuestionCircleOutlined, ReadOutlined,
    RedoOutlined, ReloadOutlined, RocketOutlined, SafetyCertificateOutlined, SafetyOutlined,
    SaveOutlined, SearchOutlined, SecurityScanOutlined, SelectOutlined, SettingOutlined,
    ShareAltOutlined, ShopOutlined, ShoppingCartOutlined, SmileOutlined, SolutionOutlined,
    StarOutlined, StopOutlined, SyncOutlined, TableOutlined, TagOutlined, TagsOutlined,
    TeamOutlined, ThunderboltOutlined, ToolOutlined, TrophyOutlined,
    UndoOutlined, UnlockOutlined, UploadOutlined, UserOutlined, VideoCameraOutlined,
    WalletOutlined, WarningOutlined, WifiOutlined, WomanOutlined, ZoomInOutlined, ZoomOutOutlined
};

export const ICON_NAMES: string[] = Object.keys(ICONS);

interface IconSelectProps {
    /** 表单字段名，默认 'icon'。组件内通过 Form.useFormInstance + Form.useWatch 直接订阅并回写。 */
    name?: string;
}

/**
 * RuoYi 原 svg-icons（数据库里的旧 icon 字符串）→ antd 图标组件 fallback 映射。
 * 新数据存的是 antd 图标名（UserOutlined 这种），无需走 fallback。
 */
const RUOYI_ICON_FALLBACK: Record<string, string> = {
    'system': 'SettingOutlined',
    'user': 'UserOutlined',
    'peoples': 'TeamOutlined',
    'tree': 'ApartmentOutlined',
    'tree-table': 'TableOutlined',
    'table': 'TableOutlined',
    'btn': 'AppstoreOutlined',
    'button': 'AppstoreOutlined',
    'list': 'UnorderedListOutlined',
    'dict': 'BookOutlined',
    'form': 'FormOutlined',
    'menu': 'MenuOutlined',
    'eye': 'EyeOutlined',
    'eye-open': 'EyeOutlined',
    'star': 'StarOutlined',
    'edit': 'EditOutlined',
    'message': 'MessageOutlined',
    'log': 'FileTextOutlined',
    'logininfor': 'HistoryOutlined',
    'operlog': 'HistoryOutlined',
    'monitor': 'DashboardOutlined',
    'job': 'ClockCircleOutlined',
    'joblog': 'FileTextOutlined',
    'tool': 'ToolOutlined',
    'build': 'BuildOutlined',
    'code': 'CodeOutlined',
    'skill': 'RocketOutlined',
    'guide': 'CompassOutlined',
    'online': 'WifiOutlined',
    'redis': 'DatabaseOutlined',
    'cache': 'RocketOutlined',
    'druid': 'DatabaseOutlined',
    'server': 'CloudOutlined',
    'chart': 'PieChartOutlined',
    'email': 'MailOutlined',
    'sms': 'MessageOutlined',
    'wechat': 'WechatOutlined',
    'nested': 'ApartmentOutlined',
    '404': 'QuestionCircleOutlined',
    'bug': 'BugOutlined',
    'swagger': 'FileTextOutlined',
    'druid-monitor': 'DatabaseOutlined',
    'component': 'AppstoreOutlined',
    'pass': 'SafetyCertificateOutlined',
    'redis-list': 'DatabaseOutlined',
    'redis-info': 'InfoCircleOutlined',
    'doc': 'FileTextOutlined',
    'date': 'CalendarOutlined',
    'size': 'AppstoreOutlined',
    'theme': 'BgColorsOutlined',
    'international': 'GlobalOutlined',
    'language': 'GlobalOutlined',
    'download': 'DownloadOutlined',
    'upload': 'UploadOutlined',
    'zip': 'FileZipOutlined',
    'excel': 'FileExcelOutlined',
    'pdf': 'FilePdfOutlined',
    'word': 'FileWordOutlined',
    'ppt': 'FilePptOutlined',
    'image': 'PictureOutlined',
    'video': 'VideoCameraOutlined',
    'audio': 'SoundOutlined',
    'lock': 'LockOutlined',
    'unlock': 'UnlockOutlined',
    'search': 'SearchOutlined',
    'poweroff': 'PoweroffOutlined',
    'reload': 'ReloadOutlined',
    'refresh': 'SyncOutlined',
    'sync': 'SyncOutlined',
    'copy': 'CopyOutlined',
    'delete': 'DeleteOutlined',
    'save': 'SaveOutlined',
    'link': 'LinkOutlined',
    'drag': 'DragOutlined',
    'fullscreen': 'FullscreenOutlined',
    'fullscreen-exit': 'FullscreenExitOutlined',
    'bell': 'BellOutlined',
    'calendar': 'CalendarOutlined',
    'clock': 'ClockCircleOutlined',
    'book': 'BookOutlined',
    'home': 'HomeOutlined',
    'heart': 'HeartOutlined',
    'flag': 'FlagOutlined',
    'gift': 'GiftOutlined',
    'shop': 'ShopOutlined',
    'wallet': 'WalletOutlined',
    'car': 'CarOutlined',
    'rocket': 'RocketOutlined',
    'profile': 'ProfileOutlined',
    'project': 'ProjectOutlined',
    'team': 'TeamOutlined',
    'woman': 'WomanOutlined',
    'man': 'ManOutlined',
    'idcard': 'IdcardOutlined',
    'phone': 'PhoneOutlined',
    'mobile': 'MobileOutlined',
    'cloud': 'CloudOutlined',
    'laptop': 'LaptopOutlined',
    'database': 'DatabaseOutlined',
    'share': 'ShareAltOutlined',
    'safety': 'SafetyOutlined',
    'select': 'SelectOutlined',
    'solution': 'SolutionOutlined',
    'star-fill': 'StarOutlined',
    'tag': 'TagOutlined',
    'tags': 'TagsOutlined',
    'thunderbolt': 'ThunderboltOutlined',
    'trophy': 'TrophyOutlined',
    'wallet-fill': 'WalletOutlined',
    'warning': 'WarningOutlined'
};

export function renderIcon(name: string | undefined): ComponentType | null {
    if (!name || name === '#' || name === '') return null;
    if (ICONS[name]) return ICONS[name];
    // 兼容 RuoYi 原 svg icon 名（user / system / tree / peoples ...）
    const mapped = RUOYI_ICON_FALLBACK[name];
    if (mapped && ICONS[mapped]) return ICONS[mapped];
    // 兜底：小写的 antd 图标名（如手工填了 'user' / 'setting'）→ 首字母大写 + Outlined
    const guessed = name.charAt(0).toUpperCase() + name.slice(1);
    const outlined = ICONS[`${guessed}Outlined`];
    if (outlined) return outlined;
    return ICONS[guessed] || null;
}

/**
 * 返回 icon 在菜单列表里的展示文本（中文标签或字母首字母），
 * 与 renderIcon 配套用于菜单列表/面包屑等位置，避免数据库里 '#'/空 字符串渲染成 #/空格。
 */
export function iconLabel(name: string | undefined): string {
    if (!name || name === '#' || name === '') return '';
    if (ICONS[name]) return name.replace(/Outlined$/, '');
    const mapped = RUOYI_ICON_FALLBACK[name];
    if (mapped) return mapped.replace(/Outlined$/, '');
    return name;
}

/**
 * 必须放在 antd Form 上下文中（即被 ProFormItem/Form.Item 包裹且带 name）。
 * 通过 Form.useFormInstance + Form.useWatch 自管理字段读写。
 */
export default function IconSelect({name = 'icon'}: IconSelectProps) {
    const form = Form.useFormInstance();
    const value = Form.useWatch(name, form);
    const [keyword, setKeyword] = useState('');

    const filtered = useMemo(() => {
        const k = keyword.trim().toLowerCase();
        return ICON_NAMES.filter((n) => !k || n.toLowerCase().includes(k));
    }, [keyword]);

    const ActiveIcon = renderIcon(value);
    const display = (value && value !== '#') ? String(value) : '';

    const trigger = (
        <Input
            value={display}
            placeholder="点击选择图标"
            readOnly
            prefix={ActiveIcon ? <ActiveIcon/> : null}
            style={{cursor: 'pointer'}}
        />
    );

    return <Popover
        trigger="click" placement="bottomLeft"
        content={
            <div style={{width: 360}}>
                <Input
                    placeholder="搜索图标" allowClear
                    value={keyword}
                    onChange={(e) => setKeyword(e.target.value)}
                    style={{marginBottom: 8}}
                />
                <div style={{
                    maxHeight: 240, overflowY: 'auto', display: 'grid',
                    gridTemplateColumns: 'repeat(4, 1fr)', gap: 4
                }}>
                    {filtered.map((iconName) => {
                        const IconComp = ICONS[iconName];
                        if (!IconComp) return null;
                        const active = value === iconName;
                        const Icon = IconComp;
                        return (
                            <div
                                key={iconName}
                                onClick={() => form.setFieldValue(name, iconName)}
                                style={{
                                    cursor: 'pointer', padding: '6px 4px',
                                    borderRadius: 4, textAlign: 'center',
                                    background: active ? '#e6f4ff' : 'transparent',
                                    border: active ? '1px solid #1677ff' : '1px solid transparent'
                                }}
                                title={iconName}
                            >
                                <div style={{fontSize: 18, lineHeight: 1}}><Icon/></div>
                                <div style={{
                                    fontSize: 11, marginTop: 2, overflow: 'hidden',
                                    textOverflow: 'ellipsis', whiteSpace: 'nowrap'
                                }}>
                                    {iconName.replace(/Outlined$/, '')}
                                </div>
                            </div>
                        );
                    })}
                    {filtered.length === 0 && <Tag color="default">无匹配图标</Tag>}
                </div>
            </div>
        }
    >
        <span style={{display: 'inline-block', width: '100%'}}>{trigger}</span>
    </Popover>;
}