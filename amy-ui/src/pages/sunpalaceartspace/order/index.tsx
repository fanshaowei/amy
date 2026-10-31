import {
    AppstoreOutlined,
    CheckCircleOutlined,
    ClockCircleOutlined,
    CloseCircleOutlined,
    DeleteOutlined,
    ExportOutlined,
    HistoryOutlined,
    IdcardOutlined,
    PhoneOutlined,
    TeamOutlined,
    UserOutlined
} from '@ant-design/icons';
import {PageContainer, ProTable} from '@ant-design/pro-components';
import type {ActionType, ProColumns} from '@ant-design/pro-components';
import {App, Button, Modal, Space, Tag, Typography} from 'antd';
import type {ReactNode} from 'react';
import {useCallback, useEffect, useRef, useState} from 'react';
import PageHero from '@/components/PageHero';
import {PermissionButton} from '@/components/PermissionButton';
import {downloadFile} from '@/utils/download';
import {pruneQueryParams} from '@/utils/exportParams';
import {deleteOrders, listOrders} from '@/services/sunpalaceartspace/order';
import type {CompUser, ReservationOrderRecord} from '@/services/sunpalaceartspace/order';
import {StatFilterBar} from '../StatFilterBar';
import type {StatFilterItem} from '../StatFilterBar';
import styles from '../spas.less';

/** 人员信息弹窗中的行 */
interface PersonRow {
    key: string;
    name?: string;
    phone?: string;
    idType?: string;
    idNum?: string;
    /** 是否预约人本人 */
    self: boolean;
    index?: number;
}

interface StatusStyle {
    text: string;
    color: string;
    background: string;
    icon: ReactNode;
}

/** 状态码 -> 展示样式（表格状态列与顶部统计卡共用） */
const STATUS_STYLE: Record<number, StatusStyle> = {
    0: {text: '待核销', color: '#f59e0b', background: '#fef4e2', icon: <ClockCircleOutlined/>},
    1: {text: '已完成', color: '#10b981', background: '#e7f8f1', icon: <CheckCircleOutlined/>},
    2: {text: '已过期', color: '#6b7385', background: '#f2f4f8', icon: <HistoryOutlined/>},
    3: {text: '已取消', color: '#f0525f', background: '#fdecee', icon: <CloseCircleOutlined/>}
};

/** 顶部统计 / 筛选卡项 */
const STATUS_ITEMS: Array<{ key: string; label: string; color: string; background: string; icon: ReactNode }> = [
    {key: '', label: '全部订单', color: '#4f6ef7', background: '#eef1fe', icon: <AppstoreOutlined/>},
    ...([0, 1, 2, 3] as const).map(status => {
        const style = STATUS_STYLE[status]!;
        return {
            key: `${status}`,
            label: style.text,
            icon: style.icon,
            color: style.color,
            background: style.background
        };
    })
];

/** 随行人证件类型码 -> 文案 */
const COMP_ID_TYPE_MAP: Record<number, string> = {
    0: '身份证',
    1: '护照',
    2: '港澳台证件',
    3: '军官证'
};

/** 解析随行人 JSON 字符串（解析失败时忽略该条） */
const parseCompUsers = (compUsers?: string[]): CompUser[] =>
    (compUsers || []).map(item => {
        try {
            return JSON.parse(item) as CompUser;
        } catch {
            return null;
        }
    }).filter((u): u is CompUser => !!u);

const pad = (value: number) => String(value).padStart(2, '0');

/** 日期时间拆成主副两行内容 */
const splitDateTime = (value?: string) => {
    if (!value) return {date: '-', time: ''};
    const [date, time] = value.split(' ');
    return {date: date || '-', time: time || ''};
};

/** 卡片-/单元格内的状态胶囊 */
function StatusPill({style}: { style?: StatusStyle }) {
    if (!style) {
        return <Tag bordered={false} color="default" style={{margin: 0, borderRadius: 20}}>未知状态</Tag>;
    }
    return (
        <span className={styles.pill} style={{color: style.color, background: style.background}}>
            <span className={styles.pillDot} style={{background: style.color}}/>
            {style.icon}
            {style.text}
        </span>
    );
}

export default function OrderPage() {
    const {message, modal} = App.useApp();
    const tableRef = useRef<ActionType>();
    /**
     * 最近一次列表查询的参数（已由 ProTable 完成日期格式化，并已剔除分页字段）。
     * 导出时直接复用它，保证「查到什么就导出什么」——原先这里传的是 `{}`，
     * 导致无论怎么筛选，导出的都是全量订单。
     */
    const lastQueryRef = useRef<Record<string, unknown>>({});
    /** 当前查询结果总数：导出前判空，避免无数据时下载一个只有表头的空文件 */
    const totalRef = useRef(0);
    const [activeStatus, setActiveStatus] = useState('');
    const [counts, setCounts] = useState<Record<string, number>>({});
    const [personOpen, setPersonOpen] = useState(false);
    const [persons, setPersons] = useState<PersonRow[]>([]);
    const [orderNum, setOrderNum] = useState('');
    const [updatedAt, setUpdatedAt] = useState('');

    /** 各状态订单数量统计（pageSize=1 只取 total） */
    const loadCounts = useCallback(() => {
        void Promise.all(STATUS_ITEMS.map(item => listOrders({
            pageNum: 1,
            pageSize: 1,
            reservationStatus: item.key === '' ? undefined : item.key
        }).then(res => {
            setCounts(prev => ({...prev, [item.key]: res.data?.total ?? 0}));
        }))).then(() => {
            setUpdatedAt(`${pad(new Date().getHours())}:${pad(new Date().getMinutes())}`);
        });
    }, []);

    useEffect(() => {
        loadCounts();
    }, [loadCounts]);

    const openPersons = (row: ReservationOrderRecord) => {
        const rows: PersonRow[] = [{
            key: 'self',
            name: row.name,
            phone: row.phone,
            idType: row.idType,
            idNum: row.idNum,
            self: true
        }, ...parseCompUsers(row.compUsers).map((u, i) => ({
            key: `comp_${i}`,
            name: u.name,
            phone: u.phone,
            idType: u.idType != null ? (COMP_ID_TYPE_MAP[u.idType] ?? `${u.idType}`) : undefined,
            idNum: u.idNum,
            self: false,
            index: i + 1
        }))];
        setPersons(rows);
        setOrderNum(row.reservationNum || '');
        setPersonOpen(true);
    };

    const confirmDelete = (orderIds: React.Key[], tip: string) => {
        modal.confirm({
            title: tip,
            content: '删除后订单不可恢复。',
            onOk: async () => {
                await deleteOrders(orderIds);
                message.success('删除成功');
                tableRef.current?.reload();
                loadCounts();
            }
        });
    };

    const columns: ProColumns<ReservationOrderRecord>[] = [{
        title: '订单号',
        dataIndex: 'reservationNum',
        width: 180,
        render: (_, row) => row.reservationNum
            ? <Typography.Text className={styles.mono}
                               copyable={{text: row.reservationNum, tooltips: ['复制订单号', '已复制']}}>
                {row.reservationNum}
            </Typography.Text>
            : '-'
    }, {
        title: '预约项目',
        dataIndex: 'projectName',
        width: 150,
        ellipsis: true,
        render: (_, row) => <span className={styles.cellMain}>{row.projectName || '-'}</span>
    }, {
        title: '预约人',
        dataIndex: 'name',
        width: 120,
        render: (_, row) => <div className={styles.cellStack}>
            <span className={styles.cellMain}>{row.name || '-'}</span>
            <span className={styles.cellSub}><TeamOutlined/>{row.guestsNum ?? 1} 人</span>
        </div>
    }, {
        title: '联系电话',
        dataIndex: 'phone',
        width: 140,
        render: (_, row) => row.phone
            ? <Space size={4}>
                <PhoneOutlined className={styles.cellSub}/>
                <Typography.Text copyable={{text: row.phone, tooltips: ['复制电话', '已复制']}}>{row.phone}</Typography.Text>
            </Space>
            : '-'
    }, {
        title: '证件信息',
        search: false,
        width: 190,
        render: (_, row) => <div className={styles.cellStack}>
            <span>{row.idType || '证件类型未知'}</span>
            <span className={styles.cellSub}>{row.idNum || '未填写证件号'}</span>
        </div>
    }, {
        title: '预约时间',
        dataIndex: 'reservationTime',
        valueType: 'date',
        width: 150,
        fieldProps: {style: {width: '100%'}},
        render: (_, row) => {
            const {date, time} = splitDateTime(row.reservationTime);
            return <div className={styles.cellStack}>
                <span>{date}</span>
                {time ? <span className={styles.cellSub}><ClockCircleOutlined/>{time}</span> : null}
            </div>;
        }
    }, {
        title: '状态',
        dataIndex: 'reservationStatus',
        search: false,
        width: 110,
        render: (_, row) => <StatusPill style={STATUS_STYLE[row.reservationStatus ?? -1]}/>
    }, {
        title: '下单时间',
        dataIndex: 'createTime',
        search: false,
        width: 150,
        render: (_, row) => {
            const {date, time} = splitDateTime(row.createTime);
            return <div className={styles.cellStack}>
                <span>{date}</span>
                {time ? <span className={styles.cellSub}>{time}</span> : null}
            </div>;
        }
    }, {
        title: '操作',
        valueType: 'option',
        fixed: 'right',
        width: 150,
        render: (_, row) => [
            <PermissionButton key="persons" type="link" size="small" icon={<TeamOutlined/>}
                              permission="sunpalaceartspace:reservationOrder:query"
                              onClick={() => openPersons(row)}>人员</PermissionButton>,
            <PermissionButton key="delete" type="link" size="small" danger icon={<DeleteOutlined/>}
                              permission="sunpalaceartspace:reservationOrder:remove"
                              onClick={() => confirmDelete([row.reservationOrderId!],
                                  `确认删除订单「${row.reservationNum}」？`)}>删除</PermissionButton>
        ]
    }];

    const statItems: StatFilterItem[] = STATUS_ITEMS.map(item => ({
        key: item.key,
        label: item.label,
        value: counts[item.key],
        icon: item.icon,
        color: item.color,
        background: item.background
    }));

    return <PageContainer pageHeaderRender={false}
                          token={{paddingInlinePageContainerContent: 0, paddingBlockPageContainerContent: 0}}>
        <div className={styles.page}>
            <PageHero
                crumb={<>艺术空间&nbsp;›&nbsp;<b>预约订单</b></>}
                title="预约订单"
                description="查看会员提交的预约记录、出行人信息与核销状态"
                updatedAt={updatedAt}
                onRefresh={() => {
                    tableRef.current?.reload();
                    loadCounts();
                }}
            />
            <div className={styles.content}>
                <StatFilterBar
                    items={statItems}
                    activeKey={activeStatus}
                    onChange={setActiveStatus}
                />
                <ProTable<ReservationOrderRecord>
                    rowKey="reservationOrderId"
                    actionRef={tableRef}
                    columns={columns}
                    scroll={{x: 1400}}
                    cardProps={{bodyStyle: {padding: '14px 16px'}}}
                    // 状态筛选作为额外参数传入：变化后自动回到第一页并重新请求
                    params={{reservationStatus: activeStatus || undefined}}
                    search={{labelWidth: 'auto', defaultCollapsed: false}}
                    options={{reload: true, density: false, setting: false}}
                    request={async ({current, pageSize, reservationTime, ...params}) => {
                        const queryParams = {
                            ...params,
                            // valueType:'date' 经 dateFormatter 后已是 'YYYY-MM-DD'，slice 只为兜底
                            reservationTime: reservationTime ? String(reservationTime).slice(0, 10) : undefined
                        };
                        // 记录本次查询条件（不含分页），导出时原样复用，保证「查到什么就导出什么」
                        lastQueryRef.current = queryParams;
                        const result = await listOrders({...queryParams, pageNum: current, pageSize});
                        totalRef.current = result.data.total ?? 0;
                        return {
                            data: result.data.rows,
                            total: result.data.total,
                            success: result.code === 200 || result.code == 0
                        };
                    }}
                    toolBarRender={() => [
                        <PermissionButton key="export" icon={<ExportOutlined/>}
                                          permission="sunpalaceartspace:reservationOrder:export"
                                          onClick={() => {
                                              // 查询结果为 0 条时直接提示，不发起导出请求（否则会下载一个只有表头的空文件）
                                              if (!totalRef.current) {
                                                  message.warning('当前查询条件下没有数据，无需导出');
                                                  return;
                                              }
                                              // 复用最近一次列表请求的参数（含项目 / 预约日期 / 状态 / 姓名 / 电话等筛选，
                                              // 以及顶部统计卡带出的 reservationStatus），与查询口径完全一致。
                                              // 后端 export 接口签名是 @RequestBody ReservationOrderReq，支持带条件导出。
                                              const params = pruneQueryParams(lastQueryRef.current);
                                              void downloadFile(
                                                  '/spas/reservation/order/export',
                                                  params,
                                                  `预约订单_${Date.now()}.xlsx`
                                              ).then(() => message.success('导出成功'));
                                          }}>导出数据</PermissionButton>
                    ]}
                />
            </div>
        </div>
        <Modal
            title={<Space size={8}>
                <span style={{
                    display: 'inline-flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    width: 22,
                    height: 22,
                    color: '#4f6ef7',
                    background: '#eef1fe',
                    borderRadius: 6,
                    fontSize: 13
                }}><IdcardOutlined/></span>
                <span style={{fontWeight: 600}}>预约人员信息</span>
                <Typography.Text type="secondary" style={{fontSize: 12, fontWeight: 400}}>
                    单号 {orderNum} · 共 {persons.length} 人
                </Typography.Text>
            </Space>}
            open={personOpen}
            width={760}
            destroyOnHidden
            onCancel={() => setPersonOpen(false)}
            footer={<Button onClick={() => setPersonOpen(false)}>关闭</Button>}
        >
            <div className={styles.personGrid}>
                {persons.map(person => <div key={person.key} className={styles.personCard}>
                    <span className={`${styles.personAvatar} ${person.self ? '' : styles.personAvatarComp}`}>
                        {person.self ? <UserOutlined/> : (person.index ?? 1)}
                    </span>
                    <div className={styles.personInfo}>
                        <div className={styles.personName}>
                            {person.name || '未填写姓名'}
                            <Tag bordered={false} color={person.self ? 'processing' : 'cyan'}
                                 style={{margin: 0, borderRadius: 20, fontSize: 11, fontWeight: 600}}>
                                {person.self ? '预约人' : `随行 ${person.index}`}
                            </Tag>
                        </div>
                        <div className={styles.personRow}>
                            <span className={styles.personRowLabel}>电话</span>
                            {person.phone || '未填写'}
                        </div>
                        <div className={styles.personRow}>
                            <span className={styles.personRowLabel}>证件</span>
                            {person.idType ? `${person.idType} · ` : ''}{person.idNum || '未填写'}
                        </div>
                    </div>
                </div>)}
                {!persons.length ? <div className={styles.cellSub}>无人员信息</div> : null}
            </div>
        </Modal>
    </PageContainer>;
}
