import {CaretRightOutlined, DeleteOutlined, DownloadOutlined, EditOutlined, PlusOutlined, ThunderboltOutlined} from '@ant-design/icons';
import {ModalForm, PageContainer, ProFormRadio, ProFormSelect, ProFormText, ProTable} from '@ant-design/pro-components';
import type {ActionType, ProColumns} from '@ant-design/pro-components';
import {App, Button, Dropdown, Switch} from 'antd';
import {useRef, useState} from 'react';
import {history, useAccess} from '@umijs/max';
import {PermissionButton} from '@/components/PermissionButton';
import {useDict} from '@/hooks/useDict';
import {
    addJob,
    changeJobStatus,
    deleteJobs,
    getJob,
    listJobs,
    runJob,
    updateJob
} from '@/services/monitor/job';
import type {JobRecord} from '@/services/monitor/job';
import {downloadFile} from '@/utils/download';
import JobDetail from './detail';

export default function JobPage() {
    const {message, modal} = App.useApp();
    const access = useAccess();
    const ref = useRef<ActionType>();
    const [selected, setSelected] = useState<React.Key[]>([]);
    const [editing, setEditing] = useState<JobRecord>();
    const [open, setOpen] = useState(false);
    const [detail, setDetail] = useState<JobRecord>();
    const groupDict = useDict('sys_job_group');
    const statusDict = useDict('sys_job_status');

    const openForm = async (r?: JobRecord) => {
        if (r?.jobId) {
            const res = await getJob(r.jobId);
            setEditing(res.data);
        } else {
            setEditing({jobGroup: 'DEFAULT', misfirePolicy: '1', concurrent: '1', status: '0'});
        }
        setOpen(true);
    };

    const openDetail = async (r: JobRecord) => {
        const res = await getJob(r.jobId!);
        setDetail(res.data);
    };

    const columns: ProColumns<JobRecord>[] = [
        {title: '任务编号', dataIndex: 'jobId', search: false, width: 90},
        {
            title: '任务名称',
            dataIndex: 'jobName',
            render: (_, r) => <a onClick={() => void openDetail(r)}>{r.jobName}</a>
        },
        {
            title: '任务组名',
            dataIndex: 'jobGroup',
            valueType: 'select',
            valueEnum: Object.fromEntries(groupDict.options.map((i) => [i.value, {text: i.label}])),
            render: (_, r) => groupDict.options.find((i) => i.value === r.jobGroup)?.label || r.jobGroup
        },
        {title: '调用目标字符串', dataIndex: 'invokeTarget', search: false, ellipsis: true},
        {title: 'cron执行表达式', dataIndex: 'cronExpression', search: false, ellipsis: true},
        {
            title: '状态',
            dataIndex: 'status',
            width: 100,
            valueType: 'select',
            valueEnum: Object.fromEntries(statusDict.options.map((i) => [i.value, {text: i.label}])),
            render: (_, r) => (
                <Switch
                    checked={r.status === '0'}
                    disabled={!access.hasPermission('monitor:job:changeStatus')}
                    onChange={(checked) => {
                        const next = checked ? '0' : '1';
                        const text = checked ? '启用' : '停用';
                        modal.confirm({
                            title: `确认要"${text}""${r.jobName}"任务吗？`,
                            onOk: async () => {
                                await changeJobStatus(r.jobId!, next);
                                message.success(`${text}成功`);
                                ref.current?.reload();
                            },
                            onCancel: () => ref.current?.reload()
                        });
                    }}
                />
            )
        },
        {
            title: '操作',
            valueType: 'option',
            width: 240,
            render: (_, r) => {
                const moreItems = [];
                if (access.hasPermission('monitor:job:changeStatus')) {
                    moreItems.push({
                        key: 'run',
                        label: '执行一次',
                        icon: <CaretRightOutlined />,
                        onClick: () =>
                            modal.confirm({
                                title: `确认要立即执行一次"${r.jobName}"任务吗？`,
                                onOk: async () => {
                                    await runJob(r.jobId!, r.jobGroup!);
                                    message.success('执行成功');
                                }
                            })
                    });
                }
                if (access.hasPermission('monitor:job:query')) {
                    moreItems.push({
                        key: 'log',
                        label: '调度日志',
                        icon: <ThunderboltOutlined />,
                        onClick: () => history.push(`/monitor/job-log?jobId=${r.jobId}`)
                    });
                }
                return [
                    <PermissionButton key="e" type="link" permission="monitor:job:edit" onClick={() => void openForm(r)}>
                        修改
                    </PermissionButton>,
                    <PermissionButton
                        key="d"
                        type="link"
                        danger
                        permission="monitor:job:remove"
                        onClick={() =>
                            modal.confirm({
                                title: `是否确认删除定时任务编号为"${r.jobId}"的数据项？`,
                                onOk: async () => {
                                    await deleteJobs([r.jobId!]);
                                    message.success('删除成功');
                                    ref.current?.reload();
                                }
                            })
                        }
                    >
                        删除
                    </PermissionButton>,
                    moreItems.length ? (
                        <Dropdown key="more" menu={{items: moreItems}}>
                            <Button type="link" size="small">
                                更多
                            </Button>
                        </Dropdown>
                    ) : null
                ];
            }
        }
    ];

    return (
        <PageContainer>
            <ProTable<JobRecord>
                rowKey="jobId"
                actionRef={ref}
                columns={columns}
                rowSelection={{selectedRowKeys: selected, onChange: setSelected}}
                request={async ({current, pageSize, ...p}) => {
                    const r = await listJobs({...p, pageNum: current, pageSize});
                    return {data: r.rows, total: r.total, success: r.code === 200};
                }}
                toolBarRender={() => [
                    <PermissionButton
                        key="a"
                        type="primary"
                        icon={<PlusOutlined />}
                        permission="monitor:job:add"
                        onClick={() => void openForm()}
                    >
                        新增
                    </PermissionButton>,
                    <PermissionButton
                        key="e"
                        icon={<EditOutlined />}
                        permission="monitor:job:edit"
                        disabled={selected.length !== 1}
                        onClick={() => void openForm({jobId: Number(selected[0])})}
                    >
                        修改
                    </PermissionButton>,
                    <PermissionButton
                        key="d"
                        danger
                        icon={<DeleteOutlined />}
                        permission="monitor:job:remove"
                        disabled={!selected.length}
                        onClick={() =>
                            modal.confirm({
                                title: `是否确认删除定时任务编号为"${selected.join(',')}"的数据项？`,
                                onOk: async () => {
                                    await deleteJobs(selected);
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
                        key="x"
                        icon={<DownloadOutlined />}
                        permission="monitor:job:export"
                        onClick={() => void downloadFile('/schedule/job/export', {}, `job_${Date.now()}.xlsx`)}
                    >
                        导出
                    </PermissionButton>,
                    <PermissionButton
                        key="l"
                        icon={<ThunderboltOutlined />}
                        permission="monitor:job:query"
                        onClick={() => history.push('/monitor/job-log')}
                    >
                        日志
                    </PermissionButton>
                ]}
            />
            <ModalForm<JobRecord>
                title={editing?.jobId ? '修改任务' : '添加任务'}
                open={open}
                initialValues={editing}
                modalProps={{destroyOnClose: true, onCancel: () => setOpen(false), width: 800}}
                onFinish={async (v) => {
                    const d = {...editing, ...v} as JobRecord;
                    if (editing?.jobId) {
                        await updateJob(d);
                        message.success('修改成功');
                    } else {
                        await addJob(d);
                        message.success('新增成功');
                    }
                    setOpen(false);
                    ref.current?.reload();
                    return true;
                }}
            >
                <ProFormText name="jobName" label="任务名称" rules={[{required: true, message: '任务名称不能为空'}]} />
                <ProFormSelect
                    name="jobGroup"
                    label="任务分组"
                    options={groupDict.options}
                    rules={[{required: true, message: '任务分组不能为空'}]}
                />
                <ProFormText
                    name="invokeTarget"
                    label="调用方法"
                    tooltip="Bean调用示例：ryTask.ryParams('ry')；Class类调用示例：com.ruoyi.quartz.task.RyTask.ryParams('ry')；参数支持字符串、布尔、长整型、浮点、整型"
                    rules={[{required: true, message: '调用目标字符串不能为空'}]}
                />
                <ProFormText
                    name="cronExpression"
                    label="cron表达式"
                    tooltip="标准 Quartz cron 表达式，例如 0 0 2 * * ? 表示每天凌晨 2 点执行"
                    rules={[{required: true, message: 'cron执行表达式不能为空'}]}
                />
                {editing?.jobId ? (
                    <ProFormRadio.Group name="status" label="状态" options={statusDict.options} />
                ) : null}
                <ProFormRadio.Group
                    name="misfirePolicy"
                    label="执行策略"
                    options={[
                        {label: '立即执行', value: '1'},
                        {label: '执行一次', value: '2'},
                        {label: '放弃执行', value: '3'}
                    ]}
                />
                <ProFormRadio.Group
                    name="concurrent"
                    label="是否并发"
                    options={[
                        {label: '允许', value: '0'},
                        {label: '禁止', value: '1'}
                    ]}
                />
            </ModalForm>
            <JobDetail type="job" record={detail} onClose={() => setDetail(undefined)} />
        </PageContainer>
    );
}
