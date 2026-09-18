import {ExportOutlined} from '@ant-design/icons';
import {PageContainer, ProTable} from '@ant-design/pro-components';
import type {ActionType, ProColumns} from '@ant-design/pro-components';
import {App, Modal, Table, Tabs, Tag} from 'antd';
import {useCallback, useEffect, useRef, useState} from 'react';
import {PermissionButton} from '@/components/PermissionButton';
import {downloadFile} from '@/utils/download';
import {deleteOrders, listOrders} from '@/services/sunpalaceartspace/order';
import type {CompUser, ReservationOrderRecord} from '@/services/sunpalaceartspace/order';

/** 人员信息弹窗中的行 */
interface PersonRow {
    key: string;
    name?: string;
    phone?: string;
    idType?: string;
    idNum?: string;
}

/** 状态码 -> [文案, Tag 颜色] */
const STATUS_MAP: Record<number, [string, string]> = {
    0: ['待核销', 'warning'],
    1: ['已完成', 'success'],
    2: ['已过期', 'default'],
    3: ['已取消', 'error']
};

/** 随行人证件类型码 -> 文案 */
const COMP_ID_TYPE_MAP: Record<number, string> = {
    0: '身份证',
    1: '护照',
    2: '港澳台证件',
    3: '军官证'
};

const TAB_ITEMS = [
    {key: '', label: '全部订单'},
    {key: '0', label: '待核销'},
    {key: '1', label: '已完成'},
    {key: '2', label: '已过期'},
    {key: '3', label: '已取消'}
];

/** 解析随行人 JSON 字符串（解析失败时忽略该条） */
const parseCompUsers = (compUsers?: string[]): CompUser[] =>
    (compUsers || []).map(item => {
        try {
            return JSON.parse(item) as CompUser;
        } catch {
            return null;
        }
    }).filter((u): u is CompUser => !!u);

export default function OrderPage() {
    const {message, modal} = App.useApp();
    const tableRef = useRef<ActionType>();
    const [activeTab, setActiveTab] = useState('');
    const [counts, setCounts] = useState<Record<string, number>>({});
    const [personOpen, setPersonOpen] = useState(false);
    const [persons, setPersons] = useState<PersonRow[]>([]);

    /** 各状态订单数量统计（pageSize=1 只取 total） */
    const loadCounts = useCallback(() => {
        const queries: Array<Promise<void>> = TAB_ITEMS.map(tab => listOrders({
            pageNum: 1,
            pageSize: 1,
            reservationStatus: tab.key === '' ? undefined : tab.key
        }).then(res => {
            setCounts(prev => ({...prev, [tab.key]: res.total ?? 0}));
        }));
        void Promise.all(queries);
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
            idNum: row.idNum
        }, ...parseCompUsers(row.compUsers).map((u, i) => ({
            key: `comp_${i}`,
            name: u.name,
            phone: u.phone,
            idType: u.idType != null ? (COMP_ID_TYPE_MAP[u.idType] ?? `${u.idType}`) : undefined,
            idNum: u.idNum
        }))];
        setPersons(rows);
        setPersonOpen(true);
    };

    const confirmDelete = (orderIds: React.Key[], tip: string, after: () => void) => {
        modal.confirm({
            title: tip,
            content: '删除后订单不可恢复。',
            onOk: async () => {
                await deleteOrders(orderIds);
                message.success('删除成功');
                after();
                loadCounts();
            }
        });
    };

    const columns: ProColumns<ReservationOrderRecord>[] = [{
        title: '单号',
        dataIndex: 'reservationNum',
        ellipsis: true,
        width: 180,
        copyable: true
    }, {
        title: '预约项目',
        dataIndex: 'projectName',
        ellipsis: true
    }, {
        title: '姓名',
        dataIndex: 'name'
    }, {
        title: '电话',
        dataIndex: 'phone'
    }, {
        title: '证件类型',
        dataIndex: 'idType',
        search: false
    }, {
        title: '证件号码',
        dataIndex: 'idNum',
        search: false,
        ellipsis: true
    }, {
        title: '人数',
        dataIndex: 'guestsNum',
        search: false,
        width: 70
    }, {
        title: '预约时间',
        dataIndex: 'reservationTime',
        valueType: 'date',
        width: 160,
        fieldProps: {style: {width: '100%'}},
        render: (_, row) => row.reservationTime || '-'
    }, {
        title: '状态',
        dataIndex: 'reservationStatus',
        search: false,
        render: (_, row) => {
            const [text, color] = STATUS_MAP[row.reservationStatus ?? -1] ?? ['未知', 'default'];
            return <Tag color={color}>{text}</Tag>;
        }
    }, {
        title: '创建时间',
        dataIndex: 'createTime',
        search: false,
        width: 160
    }, {
        title: '操作',
        valueType: 'option',
        fixed: 'right',
        width: 130,
        render: (_, row) => [
            <PermissionButton key="persons" type="link" size="small"
                              permission="sunpalaceartspace:reservationOrder:query"
                              onClick={() => openPersons(row)}>人员信息</PermissionButton>,
            <PermissionButton key="delete" type="link" size="small" danger
                              permission="sunpalaceartspace:reservationOrder:remove"
                              onClick={() => confirmDelete([row.reservationOrderId!],
                                  `确认删除订单「${row.reservationNum}」？`,
                                  () => tableRef.current?.reload())}>删除</PermissionButton>
        ]
    }];

    return <PageContainer>
        <Tabs activeKey={activeTab} onChange={key => {
            setActiveTab(key);
            tableRef.current?.reloadAndRest?.();
        }} items={TAB_ITEMS.map(tab => ({
            key: tab.key,
            label: `${tab.label}(${counts[tab.key] ?? 0})`
        }))}/>
        <ProTable<ReservationOrderRecord>
            rowKey="reservationOrderId"
            actionRef={tableRef}
            columns={columns}
            scroll={{x: 1500}}
            search={{labelWidth: 'auto', defaultCollapsed: false}}
            options={{reload: true, density: false, setting: false}}
            request={async ({current, pageSize, reservationTime, ...params}) => {
                const result = await listOrders({
                    ...params,
                    reservationTime: reservationTime ? String(reservationTime).slice(0, 10) : undefined,
                    reservationStatus: activeTab === '' ? undefined : activeTab,
                    pageNum: current,
                    pageSize
                });
                return {data: result.rows, total: result.total, success: result.code === 200 || result.code == 0};
            }}
            toolBarRender={() => [
                <PermissionButton key="export" icon={<ExportOutlined/>}
                                  permission="sunpalaceartspace:reservationOrder:export"
                                  onClick={() => void downloadFile(
                                      '/amyBizSunPalaceArtSpace/reservation/order/export',
                                      {},
                                      `预约订单_${Date.now()}.xlsx`
                                  ).then(() => message.success('导出成功'))}>导出数据</PermissionButton>
            ]}
        />
        <Modal title="信息" open={personOpen} footer={null} width={760} onCancel={() => setPersonOpen(false)}>
            <Table<PersonRow>
                rowKey="key"
                dataSource={persons}
                pagination={false}
                columns={[{
                    title: '姓名',
                    dataIndex: 'name',
                    align: 'center'
                }, {
                    title: '电话',
                    dataIndex: 'phone',
                    align: 'center'
                }, {
                    title: '证件类型',
                    dataIndex: 'idType',
                    align: 'center'
                }, {
                    title: '证件号码',
                    dataIndex: 'idNum',
                    align: 'center'
                }]}
            />
        </Modal>
    </PageContainer>;
}
