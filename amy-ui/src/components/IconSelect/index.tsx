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

export function renderIcon(name: string | undefined): ComponentType | null {
    if (!name || name === '#' || name === '') return null;
    // 只认 antd 图标名（如 UserOutlined）。RuoYi 旧的 svg 图标名（user / system / peoples 等）
    // 不再做兼容映射，这类菜单需要在「菜单管理」里重新选择图标。
    return ICONS[name] || null;
}

/**
 * 返回 icon 在菜单列表里的展示文本（中文标签或字母首字母），
 * 与 renderIcon 配套用于菜单列表/面包屑等位置，避免数据库里 '#'/空 字符串渲染成 #/空格。
 */
export function iconLabel(name: string | undefined): string {
    if (!name || name === '#' || name === '') return '';
    // 只处理 antd 图标名；未识别的（含 RuoYi 旧 svg 名）原样返回，便于排查
    return ICONS[name] ? name.replace(/Outlined$/, '') : name;
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