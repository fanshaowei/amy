import {DeleteOutlined} from '@ant-design/icons';
import {PageContainer, ProTable} from '@ant-design/pro-components';
import type {ActionType, ProColumns} from '@ant-design/pro-components';
import {App} from 'antd';
import {useRef, useState} from 'react';
import {PermissionButton} from '@/components/PermissionButton';
import {forceLogout, listOnline} from '@/services/monitor/online';
import type {UserOnlineRecord} from '@/services/monitor/online';

/** 把毫秒时间戳格式化为 YYYY-MM-DD HH:mm:ss */
function formatLoginTime(ts?: number): string {
    if (!ts) return '-';
    const d = new Date(ts);
    const pad = (n: number) => String(n).padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
}

export default function OnlinePage() {
    const {message, modal} = App.useApp();
    const ref = useRef<ActionType>();
    const [selected, setSelected] = useState<React.Key[]>([]);

    const columns: ProColumns<UserOnlineRecord>[] = [
        {
            title: '序号',
            valueType: 'indexBorder',
            width: 60,
            align: 'center'
        },
        {title: '会话编号', dataIndex: 'tokenId', ellipsis: true, copyable: true},
        {title: '登录名称', dataIndex: 'userName', ellipsis: true},
        {title: '主机', dataIndex: 'ipaddr', ellipsis: true},
        {title: '登录地点', dataIndex: 'loginLocation', ellipsis: true, render: (_, r) => r.loginLocation || '内网IP'},
        {title: '浏览器', dataIndex: 'browser', ellipsis: true},
        {title: '操作系统', dataIndex: 'os', ellipsis: true},
        {
            title: '登录时间',
            dataIndex: 'loginTime',
            width: 180,
            render: (_, r) => formatLoginTime(r.loginTime)
        },
        {
            title: '操作',
            valueType: 'option',
            width: 100,
            render: (_, r) => [
                <PermissionButton
                    key="force"
                    type="link"
                    danger
                    permission="monitor:online:forceLogout"
                    icon={<DeleteOutlined />}
                    onClick={() =>
                        modal.confirm({
                            title: `是否确认强退名称为"${r.userName}"的用户？`,
                            okButtonProps: {danger: true},
                            onOk: async () => {
                                await forceLogout(r.tokenId!);
                                message.success('强退成功');
                                ref.current?.reload();
                            }
                        })
                    }
                >
                    强退
                </PermissionButton>
            ]
        }
    ];

    return (
        <PageContainer>
            <ProTable<UserOnlineRecord>
                rowKey="tokenId"
                actionRef={ref}
                columns={columns}
                rowSelection={{selectedRowKeys: selected, onChange: setSelected}}
                request={async ({current, pageSize, ipaddr, userName}) => {
                    const r = await listOnline({ipaddr: ipaddr as string, userName: userName as string});
                    // 后端按 list 全部返回，前端自行分页
                    const ps = pageSize ?? 10;
                    const start = ((current ?? 1) - 1) * ps;
                    const slice = r.rows.slice(start, start + ps);
                    return {data: slice, total: r.total, success: r.code === 200};
                }}
                pagination={{pageSize: 10, showSizeChanger: true}}
            />
        </PageContainer>
    );
}