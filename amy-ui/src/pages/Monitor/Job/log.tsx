import {ClearOutlined, CloseOutlined, DeleteOutlined, DownloadOutlined, EyeOutlined} from '@ant-design/icons';
import {PageContainer, ProTable} from '@ant-design/pro-components';
import type {ActionType, ProColumns} from '@ant-design/pro-components';
import {App, Button, Tag} from 'antd';
import {useEffect, useRef, useState} from 'react';
import {history, useSearchParams} from '@umijs/max';
import {PermissionButton} from '@/components/PermissionButton';
import {useDict} from '@/hooks/useDict';
import {cleanJobLogs, deleteJobLogs, getJob, listJobLogs} from '@/services/monitor/job';
import type {JobLogRecord} from '@/services/monitor/job';
import {downloadFile} from '@/utils/download';
import JobDetail from './detail';

export default function JobLogPage() {
    const {message, modal} = App.useApp();
    const [searchParams] = useSearchParams();
    const ref = useRef<ActionType>();
    const [selected, setSelected] = useState<React.Key[]>([]);
    const [detail, setDetail] = useState<JobLogRecord>();
    const [preset, setPreset] = useState<Record<string, unknown>>({});
    const groupDict = useDict('sys_job_group');
    const statusDict = useDict('sys_common_status');

    // 从任务页携带 ?jobId= 跳转过来时，按任务名称 / 分组预筛
    const jobId = searchParams.get('jobId');
    useEffect(() => {
        if (jobId && jobId !== '0') {
            getJob(Number(jobId)).then((res) => {
                setPreset({jobName: res.data.jobName, jobGroup: res.data.jobGroup});
            });
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [jobId]);

    const columns: ProColumns<JobLogRecord>[] = [
        {title: '日志编号', dataIndex: 'jobLogId', search: false, width: 90},
        {title: '任务名称', dataIndex: 'jobName'},
        {
            title: '任务组名',
            dataIndex: 'jobGroup',
            valueType: 'select',
            valueEnum: Object.fromEntries(groupDict.options.map((i) => [i.value, {text: i.label}])),
            render: (_, r) => groupDict.options.find((i) => i.value === r.jobGroup)?.label || r.jobGroup
        },
        {title: '调用目标字符串', dataIndex: 'invokeTarget', search: false, ellipsis: true},
        {title: '日志信息', dataIndex: 'jobMessage', search: false, ellipsis: true},
        {
            title: '执行状态',
            dataIndex: 'status',
            width: 100,
            valueType: 'select',
            valueEnum: Object.fromEntries(statusDict.options.map((i) => [i.value, {text: i.label}])),
            render: (_, r) => (
                <Tag color={r.status === '0' ? 'success' : 'error'}>{r.status === '0' ? '正常' : '失败'}</Tag>
            )
        },
        {title: '执行时间', dataIndex: 'createTime', search: false, width: 180},
        {
            title: '操作',
            valueType: 'option',
            width: 100,
            render: (_, r) => [
                <PermissionButton
                    key="v"
                    type="link"
                    icon={<EyeOutlined />}
                    permission="monitor:job:query"
                    onClick={() => setDetail(r)}
                >
                    详细
                </PermissionButton>
            ]
        }
    ];

    return (
        <PageContainer>
            <ProTable<JobLogRecord>
                rowKey="jobLogId"
                actionRef={ref}
                columns={columns}
                params={preset}
                rowSelection={{selectedRowKeys: selected, onChange: setSelected}}
                request={async ({current, pageSize, ...p}) => {
                    const r = await listJobLogs({...p, pageNum: current, pageSize});
                    return {data: r.rows, total: r.total, success: r.code === 200};
                }}
                toolBarRender={() => [
                    <Button
                        key="b"
                        type="primary"
                        icon={<CloseOutlined />}
                        onClick={() => history.push('/monitor/job')}
                    >
                        返回任务
                    </Button>,
                    <PermissionButton
                        key="d"
                        danger
                        icon={<DeleteOutlined />}
                        permission="monitor:job:remove"
                        disabled={!selected.length}
                        onClick={() =>
                            modal.confirm({
                                title: `是否确认删除调度日志编号为"${selected.join(',')}"的数据项？`,
                                onOk: async () => {
                                    await deleteJobLogs(selected);
                                    setSelected([]);
                                    message.success('删除成功');
                                    ref.current?.reload();
                                }
                            })
                        }
                    >
                        删除
                    </PermissionButton>,
                    <PermissionButton
                        key="c"
                        danger
                        icon={<ClearOutlined />}
                        permission="monitor:job:remove"
                        onClick={() =>
                            modal.confirm({
                                title: '是否确认清空所有调度日志数据项？',
                                onOk: async () => {
                                    await cleanJobLogs();
                                    message.success('清空成功');
                                    ref.current?.reload();
                                }
                            })
                        }
                    >
                        清空
                    </PermissionButton>,
                    <PermissionButton
                        key="x"
                        icon={<DownloadOutlined />}
                        permission="monitor:job:export"
                        onClick={() => void downloadFile('/schedule/job/log/export', {}, `log_${Date.now()}.xlsx`)}
                    >
                        导出
                    </PermissionButton>
                ]}
            />
            <JobDetail type="log" record={detail} onClose={() => setDetail(undefined)} />
        </PageContainer>
    );
}
