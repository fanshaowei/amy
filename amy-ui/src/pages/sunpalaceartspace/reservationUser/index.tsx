import {ExportOutlined} from '@ant-design/icons';
import {PageContainer, ProTable} from '@ant-design/pro-components';
import type {ActionType, ProColumns, ProFormInstance} from '@ant-design/pro-components';
import {App, Avatar, Modal, Tag} from 'antd';
import {useRef} from 'react';
import {PermissionButton} from '@/components/PermissionButton';
import {downloadFile} from '@/utils/download';
import {changeUserType, deleteUsers, listUsers} from '@/services/sunpalaceartspace/reservationUser';
import type {ReservationUserRecord} from '@/services/sunpalaceartspace/reservationUser';

/** 会员类型（1用户 2核销员） */
const USER_TYPE_NORMAL = 1;
const USER_TYPE_VERIFIER = 2;

/** 用户类型搜索选项 */
const USER_TYPE_OPTIONS = [
    {label: '全部', value: ''},
    {label: '普通用户', value: `${USER_TYPE_NORMAL}`},
    {label: '核销员', value: `${USER_TYPE_VERIFIER}`}
];

/** 身份切换二次确认文案 */
const TYPE_CONFIRM_TEXT: Record<number, string> = {
    [USER_TYPE_NORMAL]: '确认将该用户切换为普通用户？',
    [USER_TYPE_VERIFIER]: '确认将该用户设为核销员？'
};

export default function ReservationUserPage() {
    const {message, modal} = App.useApp();
    const tableRef = useRef<ActionType>();
    /** 搜索表单实例：导出时取当前查询条件 */
    const formRef = useRef<ProFormInstance>();
    /** 当前查询结果总数：导出前判空，避免无数据时下载一个只有表头的空文件 */
    const totalRef = useRef(0);

    /** 切换核销员/普通用户身份 */
    const confirmChangeType = (row: ReservationUserRecord, nextType: number) => {
        modal.confirm({
            title: TYPE_CONFIRM_TEXT[nextType] ?? '确认切换该用户身份？',
            onOk: async () => {
                await changeUserType(row.reservationUserId!, nextType);
                message.success('身份切换成功');
                tableRef.current?.reload();
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
                tableRef.current?.reload();
            }
        });
    };

    const columns: ProColumns<ReservationUserRecord>[] = [{
        title: '用户编号',
        dataIndex: 'reservationUserId',
        // 用户编号即主键；后端列表/导出 SQL 未提供按主键筛选的条件，故暂不开搜索
        search: false,
        ellipsis: true,
        width: 110
    }, {
        title: '用户头像',
        dataIndex: 'avatarImgUrl',
        search: false,
        width: 90,
        align: 'center',
        render: (_, row) => <Avatar src={row.avatarImgUrl}>{(row.nickname || '?').slice(0, 1)}</Avatar>
    }, {
        title: '微信昵称',
        dataIndex: 'nickname',
        ellipsis: true
    }, {
        title: '姓名',
        dataIndex: 'name',
        render: (_, row) => row.name || '-'
    }, {
        title: '电话',
        dataIndex: 'phone',
        ellipsis: true
    }, {
        title: '身份证号',
        dataIndex: 'idNum',
        search: false,
        ellipsis: true,
        render: (_, row) => row.idNum || '-'
    }, {
        title: '用户类型',
        dataIndex: 'type',
        width: 90,
        align: 'center',
        valueType: 'select',
        fieldProps: {options: USER_TYPE_OPTIONS},
        render: (_, row) => row.type === USER_TYPE_VERIFIER
            ? <Tag color="success">核销员</Tag>
            : <Tag>普通用户</Tag>
    }, {
        title: 'openid',
        dataIndex: 'openId',
        search: false,
        ellipsis: true,
        width: 200,
        copyable: true
    }, {
        title: '注册时间',
        dataIndex: 'createTime',
        valueType: 'dateRange',
        width: 160,
        // 提交时把 [begin, end] 转成 RuoYi 约定的 params.beginTime / params.endTime
        search: {transform: (value) => ({params: {beginTime: value?.[0], endTime: value?.[1]}})},
        render: (_, row) => row.createTime || '-'
    }, {
        title: '操作',
        valueType: 'option',
        fixed: 'right',
        width: 130,
        render: (_, row) => [
            <PermissionButton key="type" type="link" size="small"
                              permission="sunpalaceartspace:reservationUser:edit"
                              onClick={() => confirmChangeType(row,
                                  row.type === USER_TYPE_VERIFIER ? USER_TYPE_NORMAL : USER_TYPE_VERIFIER)}>
                {row.type === USER_TYPE_VERIFIER ? '设为普通用户' : '设为核销员'}
            </PermissionButton>,
            <PermissionButton key="delete" type="link" size="small" danger
                              permission="sunpalaceartspace:reservationUser:remove"
                              onClick={() => confirmDelete(row)}>删除</PermissionButton>
        ]
    }];

    return <PageContainer>
        <ProTable<ReservationUserRecord>
            rowKey="reservationUserId"
            actionRef={tableRef}
            formRef={formRef}
            columns={columns}
            scroll={{x: 1400}}
            search={{labelWidth: 'auto', defaultCollapsed: false}}
            options={{reload: true, density: false, setting: false}}
            request={async ({current, pageSize, ...params}) => {
                const result = await listUsers({pageNum: current, pageSize, ...params});
                totalRef.current = result.data.total ?? 0;
                return {data: result.data.rows, total: result.data.total, success: result.code === 200 || result.code == 0};
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
    </PageContainer>;
}
