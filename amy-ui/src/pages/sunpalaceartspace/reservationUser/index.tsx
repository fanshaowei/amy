import {
    ExportOutlined,
    PhoneOutlined,
    SafetyCertificateOutlined,
    SwapOutlined,
    DeleteOutlined,
    TeamOutlined,
    UserAddOutlined,
    UserOutlined
} from '@ant-design/icons';
import {PageContainer, ProTable} from '@ant-design/pro-components';
import type {ActionType, ProColumns, ProFormInstance} from '@ant-design/pro-components';
import {App, Avatar, Badge, Space, Typography} from 'antd';
import dayjs from 'dayjs';
import {useCallback, useEffect, useRef, useState} from 'react';
import PageHero from '@/components/PageHero';
import {PermissionButton} from '@/components/PermissionButton';
import {downloadFile} from '@/utils/download';
import {changeUserType, deleteUsers, listUsers} from '@/services/sunpalaceartspace/reservationUser';
import type {ReservationUserRecord} from '@/services/sunpalaceartspace/reservationUser';
import {StatFilterBar} from '../StatFilterBar';
import type {StatFilterItem} from '../StatFilterBar';
import styles from '../spas.less';

/** 会员类型（1用户 2核销员） */
const USER_TYPE_NORMAL = 1;
const USER_TYPE_VERIFIER = 2;

/** 身份切换二次确认文案 */
const TYPE_CONFIRM_TEXT: Record<number, string> = {
    [USER_TYPE_NORMAL]: '确认将该用户切换为普通用户？',
    [USER_TYPE_VERIFIER]: '确认将该用户设为核销员？'
};

const pad = (value: number) => String(value).padStart(2, '0');

/** 日期换成 dayjs 对象（后端返回 'yyyy-MM-dd HH:mm:ss'，补 'T' 保证各浏览器一致解析） */
const toDayjs = (value?: string) => {
    if (!value) return undefined;
    const parsed = dayjs(value.includes('T') ? value : value.replace(' ', 'T'));
    return parsed.isValid() ? parsed : undefined;
};

/** 注册时间的相对描述，如「今天注册」「3 天前注册」 */
const relativeLabel = (value?: string) => {
    const date = toDayjs(value);
    if (!date) return '';
    const days = dayjs().startOf('day').diff(date.startOf('day'), 'day');
    if (days <= 0) return '今天注册';
    if (days === 1) return '昨天注册';
    return `${days} 天前注册`;
};

const splitDateTime = (value?: string) => {
    if (!value) return {date: '-', time: ''};
    const [date, time] = value.split(' ');
    return {date: date || '-', time: time || ''};
};

export default function ReservationUserPage() {
    const {message, modal} = App.useApp();
    const tableRef = useRef<ActionType>();
    /** 搜索表单实例：导出时取当前查询条件 */
    const formRef = useRef<ProFormInstance>();
    /** 当前查询结果总数：导出前判空，避免无数据时下载一个只有表头的空文件 */
    const totalRef = useRef(0);
    /** 身份筛选（由顶部统计卡控制，同时用于列表查询与导出） */
    const [typeFilter, setTypeFilter] = useState('');
    const [counts, setCounts] = useState<Record<string, number>>({});
    const [updatedAt, setUpdatedAt] = useState('');

    const loadCounts = useCallback(() => {
        const today = dayjs().format('YYYY-MM-DD');
        const queries = [
            listUsers({pageNum: 1, pageSize: 1}),
            listUsers({pageNum: 1, pageSize: 1, type: USER_TYPE_VERIFIER}),
            listUsers({pageNum: 1, pageSize: 1, type: USER_TYPE_NORMAL}),
            // 今日新增：走 params.beginTime / params.endTime（见 ReservationUserMapper.xml）
            listUsers({pageNum: 1, pageSize: 1, params: {beginTime: today, endTime: today}})
        ];
        void Promise.all(queries).then(result => {
            setCounts({
                total: result[0]?.data?.total ?? 0,
                verifier: result[1]?.data?.total ?? 0,
                normal: result[2]?.data?.total ?? 0,
                today: result[3]?.data?.total ?? 0
            });
            setUpdatedAt(`${pad(new Date().getHours())}:${pad(new Date().getMinutes())}`);
        });
    }, []);

    useEffect(() => {
        loadCounts();
    }, [loadCounts]);

    /** 刷新列表与统计 */
    const refreshAll = useCallback(() => {
        tableRef.current?.reload();
        loadCounts();
    }, [loadCounts]);

    /** 切换核销员/普通用户身份 */
    const confirmChangeType = (row: ReservationUserRecord, nextType: number) => {
        modal.confirm({
            title: TYPE_CONFIRM_TEXT[nextType] ?? '确认切换该用户身份？',
            onOk: async () => {
                await changeUserType(row.reservationUserId!, nextType);
                message.success('身份切换成功');
                refreshAll();
            }
        });
    };

    const confirmDelete = (row: ReservationUserRecord) => {
        modal.confirm({
            title: `确认删除用户「${row.nickname || row.reservationUserId}」？`,
            content: '删除后不可恢复。',
            onOk: async () => {
                await deleteUsers([row.reservationUserId!]);
                message.success('删除成功');
                refreshAll();
            }
        });
    };

    const columns: ProColumns<ReservationUserRecord>[] = [{
        title: '会员',
        dataIndex: 'nickname',
        width: 220,
        render: (_, row) => <Space size={10}>
            <Avatar size={40} src={row.avatarImgUrl} icon={<UserOutlined/>} alt={`${row.nickname || '会员'}头像`}/>
            <div className={styles.cellStack}>
                <span className={styles.cellMain}>{row.nickname || '未设置昵称'}</span>
                <span className={styles.cellSub}>ID {row.reservationUserId}</span>
            </div>
        </Space>
    }, {
        title: '姓名',
        dataIndex: 'name',
        width: 110,
        render: (_, row) => row.name || <span className={styles.cellMuted}>-</span>
    }, {
        title: '联系电话',
        dataIndex: 'phone',
        width: 150,
        render: (_, row) => row.phone
            ? <Space size={4}>
                <PhoneOutlined className={styles.cellSub}/>
                <Typography.Text copyable={{text: row.phone, tooltips: ['复制电话', '已复制']}}>{row.phone}</Typography.Text>
            </Space>
            : <span className={styles.cellMuted}>-</span>
    }, {
        title: '身份证号',
        dataIndex: 'idNum',
        search: false,
        width: 180,
        ellipsis: true,
        render: (_, row) => row.idNum
            ? <span className={styles.mono}>{row.idNum}</span>
            : <span className={styles.cellMuted}>未实名登记</span>
    }, {
        title: '身份',
        dataIndex: 'type',
        width: 120,
        // 筛选入口已由顶部统计卡承担，避免与表单内的条件互相冲突
        search: false,
        render: (_, row) => row.type === USER_TYPE_VERIFIER
            ? <span className={styles.pill} style={{color: '#10b981', background: '#e7f8f1'}}>
                <span className={styles.pillDot} style={{background: '#10b981'}}/>
                <SafetyCertificateOutlined/>核销员
            </span>
            : <span className={styles.pill} style={{color: '#6b7385', background: '#f2f4f8'}}>
                <span className={styles.pillDot} style={{background: '#6b7385'}}/>
                <UserOutlined/>普通用户
            </span>
    }, {
        title: '实名状态',
        dataIndex: 'isVerified',
        width: 110,
        search: false,
        render: (_, row) => row.isVerified
            ? <Badge status="success" text={<span className={styles.cellSub}>已实名</span>}/>
            : <Badge status="default" text={<span className={styles.cellSub}>未实名</span>}/>
    }, {
        title: 'openid',
        dataIndex: 'openId',
        search: false,
        ellipsis: true,
        width: 190,
        render: (_, row) => row.openId
            ? <Typography.Text className={styles.mono} copyable={{text: row.openId, tooltips: ['复制 openid', '已复制']}}>
                {row.openId}
            </Typography.Text>
            : <span className={styles.cellMuted}>-</span>
    }, {
        title: '注册时间',
        dataIndex: 'createTime',
        valueType: 'dateRange',
        width: 170,
        // 提交时把 [begin, end] 转成 RuoYi 约定的 params.beginTime / params.endTime
        search: {transform: (value) => ({params: {beginTime: value?.[0], endTime: value?.[1]}})},
        render: (_, row) => {
            const {date, time} = splitDateTime(row.createTime);
            return <div className={styles.cellStack}>
                <span>{date}</span>
                <span className={styles.cellSub}>{time ? `${time} · ` : ''}{relativeLabel(row.createTime)}</span>
            </div>;
        }
    }, {
        title: '操作',
        valueType: 'option',
        fixed: 'right',
        width: 160,
        render: (_, row) => [
            <PermissionButton key="type" type="link" size="small" icon={<SwapOutlined/>}
                              permission="sunpalaceartspace:reservationUser:edit"
                              onClick={() => confirmChangeType(row,
                                  row.type === USER_TYPE_VERIFIER ? USER_TYPE_NORMAL : USER_TYPE_VERIFIER)}>
                {row.type === USER_TYPE_VERIFIER ? '转为会员' : '设为核销员'}
            </PermissionButton>,
            <PermissionButton key="delete" type="link" size="small" danger icon={<DeleteOutlined/>}
                              permission="sunpalaceartspace:reservationUser:remove"
                              onClick={() => confirmDelete(row)}>删除</PermissionButton>
        ]
    }];

    const statItems: StatFilterItem[] = [{
        key: '',
        label: '全部会员',
        value: counts.total,
        icon: <TeamOutlined/>,
        color: '#4f6ef7',
        background: '#eef1fe'
    }, {
        key: `${USER_TYPE_VERIFIER}`,
        label: '核销员',
        value: counts.verifier,
        icon: <SafetyCertificateOutlined/>,
        color: '#10b981',
        background: '#e7f8f1'
    }, {
        key: `${USER_TYPE_NORMAL}`,
        label: '普通会员',
        value: counts.normal,
        icon: <UserOutlined/>,
        color: '#8b5cf6',
        background: '#f1ecfd'
    }, {
        key: 'today',
        label: '今日新增',
        value: counts.today,
        icon: <UserAddOutlined/>,
        color: '#06b6d4',
        background: '#e5f8fb',
        clickable: false
    }];

    return <PageContainer pageHeaderRender={false}
                          token={{paddingInlinePageContainerContent: 0, paddingBlockPageContainerContent: 0}}>
        <div className={styles.page}>
            <PageHero
                crumb={<>艺术空间&nbsp;›&nbsp;<b>会员列表</b></>}
                title="会员列表"
                description="管理小程序端注册会员、身份权限与实名信息"
                updatedAt={updatedAt}
                onRefresh={refreshAll}
            />
            <div className={styles.content}>
                <StatFilterBar
                    items={statItems}
                    activeKey={typeFilter}
                    onChange={setTypeFilter}
                />
                <ProTable<ReservationUserRecord>
                    rowKey="reservationUserId"
                    actionRef={tableRef}
                    formRef={formRef}
                    columns={columns}
                    scroll={{x: 1400}}
                    cardProps={{bodyStyle: {padding: '14px 16px'}}}
                    // 身份筛选作为额外参数传入：变化后自动回到第一页并重新请求
                    params={{type: typeFilter ? Number(typeFilter) : undefined}}
                    search={{labelWidth: 'auto', defaultCollapsed: false}}
                    options={{reload: true, density: false, setting: false}}
                    request={async ({current, pageSize, ...params}) => {
                        const result = await listUsers({pageNum: current, pageSize, ...params});
                        totalRef.current = result.data.total ?? 0;
                        return {
                            data: result.data.rows,
                            total: result.data.total,
                            success: result.code === 200 || result.code == 0
                        };
                    }}
                    toolBarRender={() => [
                        <PermissionButton key="export" icon={<ExportOutlined/>}
                                          permission="sunpalaceartspace:reservationUser:export"
                                          onClick={() => {
                                              // 查询结果为 0 条时直接提示，不发起导出请求（否则会下载一个只有表头的空文件）
                                              if (!totalRef.current) {
                                                  message.warning('当前查询条件下没有数据，无需导出');
                                                  return;
                                              }
                                              const values = formRef.current?.getFieldsValue() ?? {};
                                              // 过滤空值：「用户类型=全部」的 value 是空字符串，传下去会导致查不到数据
                                              const params: Record<string, unknown> = {};
                                              Object.entries(values).forEach(([key, value]) => {
                                                  if (value !== undefined && value !== null && value !== '') {
                                                      params[key] = value;
                                                  }
                                              });
                                              // 身份筛选来自顶部统计卡，需要显式带上
                                              if (typeFilter) params.type = Number(typeFilter);
                                              // 注册时间：表单值是 [begin, end] 数组，而后端实体的 createTime 是 Date，
                                              // 直接传数组会反序列化失败，故转成 params.beginTime / params.endTime 并剔除原字段
                                              const timeRange = params.createTime as string[] | undefined;
                                              delete params.createTime;
                                              if (Array.isArray(timeRange) && timeRange[0]) {
                                                  params.params = {beginTime: timeRange[0], endTime: timeRange[1]};
                                              }
                                              void downloadFile(
                                                  '/spas/reservation/user/export',
                                                  params,
                                                  `会员列表_${Date.now()}.xlsx`
                                              ).then(() => message.success('导出成功'));
                                          }}>导出数据</PermissionButton>
                    ]}
                />
            </div>
        </div>
    </PageContainer>;
}
