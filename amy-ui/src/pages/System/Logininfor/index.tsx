import {DeleteOutlined, DownloadOutlined, UnlockOutlined} from '@ant-design/icons';
import {PageContainer, ProTable} from '@ant-design/pro-components';
import type {ActionType, ProColumns} from '@ant-design/pro-components';
import {App, Tag} from 'antd';
import {useRef, useState} from 'react';
import {PermissionButton} from '@/components/PermissionButton';
import {useDict} from '@/hooks/useDict';
import {cleanLoginInfo, deleteLoginInfo, listLoginInfo, unlockLogin} from '@/services/system/logininfor';
import type {LoginInfoRecord} from '@/services/system/logininfor';
import {downloadFileWithQuery} from '@/utils/download';
import {pruneQueryParams} from '@/utils/exportParams';

export default function LoginInfoPage() {
    const {message, modal} = App.useApp();
    const ref = useRef<ActionType>();
    /** 最近一次列表查询参数（已剔除分页），导出时原样复用，保证「查到什么就导出什么」 */
    const lastQueryRef = useRef<Record<string, unknown>>({});
    const [selected, setSelected] = useState<React.Key[]>([]);
    const [selectedRows, setSelectedRows] = useState<LoginInfoRecord[]>([]);
    const statusDict = useDict('sys_common_status');
    const columns: ProColumns<LoginInfoRecord>[] = [{
        title: '访问编号',
        dataIndex: 'infoId',
        search: false
    }, {title: '用户名称', dataIndex: 'userName', sorter: true}, {
        title: '登录地址',
        dataIndex: 'ipaddr'
    }, {
        title: '登录状态',
        dataIndex: 'status',
        valueType: 'select',
        valueEnum: Object.fromEntries(statusDict.options.map((i) => [i.value, {text: i.label}])),
        render: (_, r) => <Tag color={r.status === '0' ? 'success' : 'error'}>{r.status === '0' ? '成功' : '失败'}</Tag>
    }, {title: '描述', dataIndex: 'msg', search: false, ellipsis: true}, {
        title: '访问时间',
        dataIndex: 'accessTime',
        valueType: 'dateRange',
        sorter: true,
        search: {
            transform: (value) => ({
                params: {
                    beginTime: value?.[0],
                    endTime: value?.[1] ? `${value[1]} 23:59:59` : undefined
                }
            })
        },
        render: (_, r) => r.accessTime || '-'
    }];
    return <PageContainer header={{title: false}}><ProTable<LoginInfoRecord> rowKey="infoId" actionRef={ref} columns={columns} rowSelection={{
        selectedRowKeys: selected,
        onChange: (keys, rows) => {
            setSelected(keys);
            setSelectedRows(rows);
        }
    }} request={async ({current, pageSize, ...p}, sort) => {
        // 记录本次查询条件：访问时间是 dateRange，经 search.transform 已转成 params.beginTime/endTime，
        // 且 ProTable 已按 dateFormatter 格式化——导出直接复用它，不要去取表单原始值（那是 dayjs 对象）
        lastQueryRef.current = p;
        const sortEntry = Object.entries(sort || {})[0];
        const r = await listLoginInfo({
            ...p,
            pageNum: current,
            pageSize,
            orderByColumn: sortEntry?.[0],
            isAsc: sortEntry?.[1]
        });
        return {data: r.rows, total: r.total, success: r.code === 200};
    }} toolBarRender={() => [<PermissionButton key="d" danger icon={<DeleteOutlined/>}
                                               permission="system:logininfor:remove" disabled={!selected.length}
                                               onClick={() => modal.confirm({
                                                   title: `确认删除访问编号「${selected.join(',')}」？`,
                                                   onOk: async () => {
                                                       await deleteLoginInfo(selected);
                                                       setSelected([]);
                                                       setSelectedRows([]);
                                                       message.success('删除成功');
                                                       ref.current?.reload();
                                                   }
                                               })}>删除</PermissionButton>,
        <PermissionButton key="c" danger permission="system:logininfor:remove" onClick={() => modal.confirm({
            title: '确认清空所有登录日志？', onOk: async () => {
                await cleanLoginInfo();
                message.success('清空成功');
                ref.current?.reload();
            }
        })}>清空</PermissionButton>,
        <PermissionButton key="u" icon={<UnlockOutlined/>} permission="system:logininfor:unlock"
                          disabled={selectedRows.length !== 1 || !selectedRows[0]?.userName}
                          onClick={() => modal.confirm({
                              title: `确认解锁用户「${selectedRows[0]?.userName}」？`,
                              onOk: async () => {
                                  await unlockLogin(selectedRows[0]!.userName!);
                                  message.success(`用户 ${selectedRows[0]!.userName} 解锁成功`);
                              }
                          })}>解锁</PermissionButton>,
        <PermissionButton key="x" icon={<DownloadOutlined/>} permission="system:logininfor:export"
                          onClick={() => void downloadFileWithQuery('/system/logininfor/export', pruneQueryParams(lastQueryRef.current), `logininfor_${Date.now()}.xlsx`)}>导出</PermissionButton>]}/></PageContainer>;
}
