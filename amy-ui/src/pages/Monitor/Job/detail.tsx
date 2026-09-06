import {Descriptions, Drawer, Tag, Typography} from 'antd';
import {useDict} from '@/hooks/useDict';
import type {JobLogRecord, JobRecord} from '@/services/monitor/job';

interface JobDetailProps {
    type: 'job' | 'log';
    record?: JobRecord | JobLogRecord;
    onClose: () => void;
}

const MISFIRE_MAP: Record<string, {text: string; color: string}> = {
    '0': {text: '默认策略', color: 'default'},
    '1': {text: '立即执行', color: 'warning'},
    '2': {text: '执行一次', color: 'processing'},
    '3': {text: '放弃执行', color: 'error'}
};

export default function JobDetail({type, record, onClose}: JobDetailProps) {
    const groupDict = useDict('sys_job_group');
    const r = record as (JobRecord & JobLogRecord) | undefined;

    const groupLabel = groupDict.options.find((i) => i.value === r?.jobGroup)?.label || r?.jobGroup || '-';

    return (
        <Drawer
            title={type === 'log' ? '调度日志详细' : '任务详细'}
            width={780}
            open={Boolean(record)}
            onClose={onClose}
            destroyOnClose
        >
            {type === 'job' ? (
                <Descriptions bordered column={2} size="middle">
                    <Descriptions.Item label="任务编号">{r?.jobId ?? '-'}</Descriptions.Item>
                    <Descriptions.Item label="任务名称">{r?.jobName || '-'}</Descriptions.Item>
                    <Descriptions.Item label="任务分组">{groupLabel}</Descriptions.Item>
                    <Descriptions.Item label="执行状态">
                        {r?.status === '0' ? <Tag color="success">正常</Tag> : <Tag color="default">暂停</Tag>}
                    </Descriptions.Item>
                    <Descriptions.Item label="cron 表达式" span={2}>
                        <Typography.Text copyable>{r?.cronExpression || '-'}</Typography.Text>
                    </Descriptions.Item>
                    <Descriptions.Item label="下次执行时间">{r?.nextValidTime || '-'}</Descriptions.Item>
                    <Descriptions.Item label="执行策略">
                        {r?.misfirePolicy ? (
                            <Tag color={MISFIRE_MAP[r.misfirePolicy]?.color}>{MISFIRE_MAP[r.misfirePolicy]?.text}</Tag>
                        ) : (
                            '-'
                        )}
                    </Descriptions.Item>
                    <Descriptions.Item label="并发执行">
                        {r?.concurrent === '0' ? <Tag color="success">允许</Tag> : <Tag color="error">禁止</Tag>}
                    </Descriptions.Item>
                    <Descriptions.Item label="创建人">{r?.createBy || '-'}</Descriptions.Item>
                    <Descriptions.Item label="创建时间">{r?.createTime || '-'}</Descriptions.Item>
                    <Descriptions.Item label="更新人">{r?.updateBy || '-'}</Descriptions.Item>
                    <Descriptions.Item label="更新时间">{r?.updateTime || '-'}</Descriptions.Item>
                    <Descriptions.Item label="调用方法" span={2}>
                        <pre style={{margin: 0, whiteSpace: 'pre-wrap', wordBreak: 'break-all'}}>
                            {r?.invokeTarget || '（无）'}
                        </pre>
                    </Descriptions.Item>
                    {r?.remark ? (
                        <Descriptions.Item label="备注" span={2}>
                            {r.remark}
                        </Descriptions.Item>
                    ) : null}
                </Descriptions>
            ) : (
                <>
                    <Descriptions bordered column={2} size="middle">
                        <Descriptions.Item label="日志编号">{r?.jobLogId ?? '-'}</Descriptions.Item>
                        <Descriptions.Item label="执行状态">
                            {r?.status === '0' ? <Tag color="success">正常</Tag> : <Tag color="error">失败</Tag>}
                        </Descriptions.Item>
                        <Descriptions.Item label="开始时间">{r?.startTime || '-'}</Descriptions.Item>
                        <Descriptions.Item label="结束时间">{r?.endTime || '-'}</Descriptions.Item>
                        <Descriptions.Item label="记录时间">{r?.createTime || '-'}</Descriptions.Item>
                        <Descriptions.Item label="执行耗时">
                            {r?.status === '0' && r?.startTime && r?.endTime
                                ? `${new Date(r.endTime).getTime() - new Date(r.startTime).getTime()} 毫秒`
                                : '-'}
                        </Descriptions.Item>
                        <Descriptions.Item label="任务名称">{r?.jobName || '-'}</Descriptions.Item>
                        <Descriptions.Item label="任务分组">{groupLabel}</Descriptions.Item>
                        <Descriptions.Item label="日志信息" span={2}>
                            {r?.jobMessage || '-'}
                        </Descriptions.Item>
                        <Descriptions.Item label="调用方法" span={2}>
                            <pre style={{margin: 0, whiteSpace: 'pre-wrap', wordBreak: 'break-all'}}>
                                {r?.invokeTarget || '（无）'}
                            </pre>
                        </Descriptions.Item>
                    </Descriptions>
                    {r?.status === '1' ? (
                        <Typography.Paragraph type="danger" style={{marginTop: 16}}>
                            <Typography.Text strong>异常信息：</Typography.Text>
                            <pre style={{whiteSpace: 'pre-wrap', wordBreak: 'break-all'}}>
                                {r.exceptionInfo || '（无）'}
                            </pre>
                        </Typography.Paragraph>
                    ) : null}
                </>
            )}
        </Drawer>
    );
}
