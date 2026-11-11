import {FieldTimeOutlined, InfoCircleOutlined, PlusOutlined} from '@ant-design/icons';
import {App, Button, Empty, Popconfirm, Space, Spin, Switch, Table, Tag, TimePicker, Tooltip, Typography} from 'antd';
import type {ColumnsType} from 'antd/es/table';
import dayjs, {Dayjs} from 'dayjs';
import {useCallback, useEffect, useState} from 'react';
import {PermissionButton} from '@/components/PermissionButton';
import {
    addReservationConfig,
    deleteReservationConfigs,
    listReservationConfigs,
    updateReservationEnabled
} from '@/services/sunpalaceartspace/projectReservationConfig';
import type {ReservationConfigRecord} from '@/services/sunpalaceartspace/projectReservationConfig';
import type {ProjectRecord} from '@/services/sunpalaceartspace/project';

/**
 * 「时段配置」弹窗：
 * 1. 查询项目每个时间段（每日生效）的配置；
 * 2. 通过开关配置每个时间段是否可预约，并可新增 / 删除时段。
 */

/** 单页拉取上限：时段配置属小表，一个项目的配置条数远小于该值 */
const PAGE_SIZE = 200;

const toDayjs = (value?: string | null) => {
    if (!value) return undefined;
    const parsed = dayjs(`2000-01-01 ${value}`);
    return parsed.isValid() ? parsed : undefined;
};

const toStr = (value?: Dayjs | string | null) => {
    if (!value) return undefined;
    if (dayjs.isDayjs(value)) return value.format('HH:mm:ss');
    const timePart = String(value).replace(/^\d{4}-\d{2}-\d{2}[T\s]+/, '');
    const parsed = dayjs(`2000-01-01 ${timePart}`);
    return parsed.isValid() ? parsed.format('HH:mm:ss') : timePart;
};

interface AddFormValues {
    startTime?: Dayjs;
    endTime?: Dayjs;
}

interface Props {
    open: boolean;
    project?: ProjectRecord;
    onClose: () => void;
}

export function ReservationConfigModal({open, project, onClose}: Props) {
    const {message} = App.useApp();
    const [loading, setLoading] = useState(false);
    const [saving, setSaving] = useState(false);
    const [togglingIds, setTogglingIds] = useState<Set<number>>(new Set());
    const [rows, setRows] = useState<ReservationConfigRecord[]>([]);
    const [formValues, setFormValues] = useState<AddFormValues>({});
    const [formErrors, setFormErrors] = useState<{ startTime?: string; endTime?: string }>({});

    const projectId = project?.projectId;

    const loadConfigs = useCallback(async () => {
        if (!projectId) return;
        setLoading(true);
        try {
            const result = await listReservationConfigs({projectId, pageNum: 1, pageSize: PAGE_SIZE});
            if (result.code === 200 || result.code == 0) {
                setRows(result.data.rows ?? []);
            } else {
                message.error(result.msg || '查询时段配置失败');
            }
        } catch {
            message.error('查询时段配置失败');
        } finally {
            setLoading(false);
        }
    }, [projectId, message]);

    useEffect(() => {
        if (open && projectId) {
            setRows([]);
            setFormValues({});
            setFormErrors({});
            void loadConfigs();
        }
    }, [open, projectId, loadConfigs]);

    /** 切换某个时段是否可预约（乐观更新，失败回滚） */
    const toggleEnabled = async (row: ReservationConfigRecord, enabled: boolean) => {
        if (!row.configId) return;
        setTogglingIds(prev => new Set(prev).add(row.configId!));
        const prevRows = rows;
        setRows(list => list.map(item => item.configId === row.configId ? {...item, reservationEnabled: enabled} : item));
        try {
            const result = await updateReservationEnabled({configId: row.configId, reservationEnabled: enabled});
            if (result.code === 200 || result.code == 0) {
                message.success(enabled ? '该时段已开放预约' : '该时段已关闭预约');
            } else {
                setRows(prevRows);
                message.error(result.msg || '修改失败');
            }
        } catch {
            setRows(prevRows);
            message.error('修改失败');
        } finally {
            setTogglingIds(prev => {
                const next = new Set(prev);
                next.delete(row.configId!);
                return next;
            });
        }
    };

    const submitAdd = async () => {
        const startTime = toStr(formValues.startTime);
        const endTime = toStr(formValues.endTime);
        const errors: { startTime?: string; endTime?: string } = {};
        if (!startTime) errors.startTime = '请选择开始时间';
        if (!endTime) errors.endTime = '请选择结束时间';
        else if (startTime && !dayjs(`2000-01-01 ${endTime}`).isAfter(dayjs(`2000-01-01 ${startTime}`))) {
            errors.endTime = '结束时间必须晚于开始时间';
        }
        setFormErrors(errors);
        if (Object.keys(errors).length || !projectId) return;

        setSaving(true);
        try {
            const result = await addReservationConfig({projectId, startTime, endTime, reservationEnabled: true});
            if (result.code === 200 || result.code == 0) {
                message.success('时段配置添加成功');
                setFormValues({});
                await loadConfigs();
            } else {
                message.error(result.msg || '添加失败');
            }
        } catch {
            message.error('添加失败');
        } finally {
            setSaving(false);
        }
    };

    const confirmDelete = async (configId: number) => {
        try {
            const result = await deleteReservationConfigs([configId]);
            if (result.code === 200 || result.code == 0) {
                message.success('删除成功');
                await loadConfigs();
            } else {
                message.error(result.msg || '删除失败');
            }
        } catch {
            message.error('删除失败');
        }
    };

    const columns: ColumnsType<ReservationConfigRecord> = [{
        title: '时间段',
        width: 160,
        render: (_, row) => <Typography.Text code>{`${row.startTime || '--'} ~ ${row.endTime || '--'}`}</Typography.Text>
    }, {
        title: '状态',
        width: 110,
        render: (_, row) => row.reservationEnabled
            ? <Tag color="success" bordered={false} style={{margin: 0}}>可预约</Tag>
            : <Tag color="default" bordered={false} style={{margin: 0}}>不可预约</Tag>
    }, {
        title: '是否可预约',
        width: 110,
        align: 'center',
        render: (_, row) => <Tooltip title={row.reservationEnabled ? '关闭后该时段不可预约' : '开启后恢复该时段预约'}>
            <Switch
                checked={row.reservationEnabled}
                loading={row.configId != null && togglingIds.has(row.configId)}
                onChange={checked => void toggleEnabled(row, checked)}
            />
        </Tooltip>
    }, {
        title: '操作',
        width: 90,
        align: 'center',
        render: (_, row) => row.configId == null ? null : <Popconfirm
            title="确认删除该时段配置？"
            description="删除后该时段将恢复默认可预约规则。"
            onConfirm={() => void confirmDelete(row.configId!)}>
            <PermissionButton type="link" size="small" danger
                              permission="sunpalaceartspace:reservationConfig:remove">删除</PermissionButton>
        </Popconfirm>
    }];

    return <div>
        <div style={{marginBottom: 8, display: 'flex', alignItems: 'center', justifyContent: 'space-between'}}>
            <Space size={6}>
                <FieldTimeOutlined style={{color: '#fa8c16'}}/>
                <Typography.Text strong>{project?.projectName ?? '-'}</Typography.Text>
                <Typography.Text type="secondary" style={{fontSize: 12}}>
                    共 {rows.length} 个时段，配置对项目每天生效
                </Typography.Text>
            </Space>
            <Button icon={<PlusOutlined/>} onClick={loadConfigs}>刷新</Button>
        </div>

        <Spin spinning={loading}>
            <Table<ReservationConfigRecord>
                rowKey="configId"
                size="small"
                columns={columns}
                dataSource={rows}
                pagination={false}
                locale={{emptyText: <Empty description="暂无时段配置，请在下方添加" style={{padding: 24}}/>}}
            />
        </Spin>

        <div style={{
            marginTop: 16,
            padding: '12px 16px',
            border: '1px dashed #d9d9d9',
            borderRadius: 8,
            background: '#fafafa'
        }}>
            <Typography.Text strong style={{display: 'block', marginBottom: 8}}>
                <PlusOutlined style={{color: '#1677ff', marginRight: 6}}/>新增时段
            </Typography.Text>
            <div style={{display: 'flex', gap: 16, alignItems: 'flex-end', flexWrap: 'wrap'}}>
                <div style={{width: 160}}>
                    <Typography.Text type="secondary" style={{fontSize: 12}}>开始时间</Typography.Text>
                    <TimePicker
                        format="HH:mm:ss"
                        style={{width: '100%', marginTop: 2}}
                        placeholder="请选择"
                        status={formErrors.startTime ? 'error' : undefined}
                        value={formValues.startTime}
                        onChange={value => {
                            setFormValues(prev => ({...prev, startTime: value}));
                            setFormErrors(prev => ({...prev, startTime: undefined}));
                        }}
                    />
                    {formErrors.startTime &&
                        <Typography.Text type="danger" style={{fontSize: 12}}>{formErrors.startTime}</Typography.Text>}
                </div>
                <div style={{width: 160}}>
                    <Typography.Text type="secondary" style={{fontSize: 12}}>结束时间</Typography.Text>
                    <TimePicker
                        format="HH:mm:ss"
                        style={{width: '100%', marginTop: 2}}
                        placeholder="请选择"
                        status={formErrors.endTime ? 'error' : undefined}
                        value={formValues.endTime}
                        onChange={value => {
                            setFormValues(prev => ({...prev, endTime: value}));
                            setFormErrors(prev => ({...prev, endTime: undefined}));
                        }}
                    />
                    {formErrors.endTime &&
                        <Typography.Text type="danger" style={{fontSize: 12}}>{formErrors.endTime}</Typography.Text>}
                </div>
                <PermissionButton type="primary" loading={saving}
                                  permission="sunpalaceartspace:reservationConfig:add"
                                  onClick={() => void submitAdd()}>保存</PermissionButton>
            </div>
            <div style={{
                marginTop: 8,
                padding: '6px 10px',
                borderRadius: 6,
                background: '#fff7e6',
                border: '1px solid #ffe7ba',
                color: '#ad6800',
                fontSize: 12,
                display: 'flex',
                gap: 6,
                alignItems: 'flex-start'
            }}>
                <InfoCircleOutlined style={{marginTop: 3}}/>
                <span>新时段默认「可预约」，保存后可在上方列表中随时开关；同一项目不允许重复的开始时间。</span>
            </div>
        </div>
    </div>;
}

export default ReservationConfigModal;
