import {
    AppstoreOutlined,
    ClockCircleOutlined,
    DeleteOutlined,
    EyeInvisibleOutlined,
    EyeOutlined,
    FileTextOutlined,
    InfoCircleOutlined,
    PlusOutlined
} from '@ant-design/icons';
import {
    ModalForm,
    PageContainer,
    ProCard,
    ProFormDependency,
    ProFormDigit,
    ProFormItem,
    ProFormRadio,
    ProFormText,
    ProFormTimePicker,
    ProTable
} from '@ant-design/pro-components';
import type {ActionType, ProColumns} from '@ant-design/pro-components';
import {App, Col, Empty, Row, Skeleton, Space, Typography} from 'antd';
import dayjs, {Dayjs} from 'dayjs';
import {useCallback, useEffect, useRef, useState} from 'react';
import PageHero from '@/components/PageHero';
import {PermissionButton} from '@/components/PermissionButton';
import {ProFormRichText} from '@/components/RichTextEditor';
import {
    addProject,
    deleteProjects,
    getProject,
    listProjects,
    updateProject
} from '@/services/sunpalaceartspace/project';
import type {ProjectRecord} from '@/services/sunpalaceartspace/project';
import {StatFilterBar} from '../StatFilterBar';
import type {StatFilterItem} from '../StatFilterBar';
import styles from '../spas.less';
import {CoverUpload} from './CoverUpload';
import ProjectCard from './ProjectCard';

/** 表单值类型：时间字段使用 dayjs 对象，提交时再转回 HH:mm:ss 字符串 */
interface ProjectFormValues extends Omit<ProjectRecord, 'cutOffTime' | 'reservationStartTime' | 'reservationEndTime'> {
    cutOffTime?: Dayjs;
    reservationStartTime?: Dayjs;
    reservationEndTime?: Dayjs;
}

/**
 * 统计时一次性拉取的项目数量。
 * 后端 ProjectsController.list 未支持 status 过滤（见 ProjectsMapper.xml），
 * 因此 展示中 / 已隐藏 两个数量在本地统计；项目属于配置类小表，直接取整页即可。
 */
const STAT_PAGE_SIZE = 200;

const toDayjs = (value?: string | null) => {
    if (!value) return undefined;
    const parsed = dayjs(`2000-01-01 ${value}`);
    return parsed.isValid() ? parsed : undefined;
};
/**
 * 时间字段统一转 'HH:mm:ss'。
 * ProForm 提交时通常已转成 'HH:mm:ss'，但依赖监听（ProFormDependency）里可能拿到带日期的字符串
 * （如 '2000-01-01 09:00:00'，日期部分是 TimePicker 内部基准日，无意义），这里一律剥掉日期前缀。
 */
const toStr = (value?: Dayjs | string | null) => {
    if (!value) return undefined;
    if (dayjs.isDayjs(value)) return value.format('HH:mm:ss');
    const raw = String(value);
    // 去掉 'YYYY-MM-DD ' / 'YYYY-MM-DDTHH:mm:ss' 这类日期前缀，只保留时间部分
    const timePart = raw.replace(/^\d{4}-\d{2}-\d{2}[T\s]+/, '');
    const parsed = dayjs(`2000-01-01 ${timePart}`);
    if (parsed.isValid()) return parsed.format('HH:mm:ss');
    const parsedRaw = dayjs(raw);
    return parsedRaw.isValid() ? parsedRaw.format('HH:mm:ss') : (timePart || undefined);
};

const toFormValues = (record?: ProjectRecord): ProjectFormValues | undefined => record ? {
    ...record,
    cutOffTime: toDayjs(record.cutOffTime),
    reservationStartTime: toDayjs(record.reservationStartTime),
    reservationEndTime: toDayjs(record.reservationEndTime)
} : {projectName: '', coverArtUrl: '', status: '0', sort: 0, reservationNotes: ''};

/** 卡片标题前的彩色图标徽标，用于区分各个分区 */
const SectionBadge = ({icon, color, bg}: { icon: React.ReactNode; color: string; bg: string }) => <span style={{
    display: 'inline-flex',
    alignItems: 'center',
    justifyContent: 'center',
    width: 22,
    height: 22,
    borderRadius: 6,
    background: bg,
    color,
    fontSize: 13
}}>{icon}</span>;

/** 卡片标题：图标徽标 + 加粗文字 */
const sectionTitle = (icon: React.ReactNode, text: string, color: string, bg: string) => <Space size={8}>
    <SectionBadge icon={icon} color={color} bg={bg}/>
    <span style={{fontWeight: 600}}>{text}</span>
</Space>;

/** 卡片右侧的灰色说明文字 */
const sectionHint = (text: string) => <Typography.Text type="secondary" style={{fontSize: 12}}>{text}</Typography.Text>;

/** 卡片底部的浅色提示条 */
const TipBox = ({
    icon,
    text,
    bg = '#f5f8ff',
    border = '#e6ebf5',
    color = '#5a6b8c'
}: { icon: React.ReactNode; text: React.ReactNode; bg?: string; border?: string; color?: string }) => <div style={{
    marginTop: 8,
    padding: '8px 12px',
    borderRadius: 6,
    background: bg,
    border: `1px solid ${border}`,
    color,
    fontSize: 12,
    lineHeight: 1.8,
    display: 'flex',
    gap: 6,
    alignItems: 'flex-start'
}}>
    <span style={{marginTop: 4}}>{icon}</span>
    <span>{text}</span>
</div>;

const pad = (value: number) => String(value).padStart(2, '0');

export default function ProjectPage() {
    const {message, modal} = App.useApp();
    const tableRef = useRef<ActionType>();
    const [selected, setSelected] = useState<React.Key[]>([]);
    const [editing, setEditing] = useState<ProjectRecord>();
    const [open, setOpen] = useState(false);
    const [stats, setStats] = useState<{ total?: number; shown?: number; hidden?: number }>({});
    const [updatedAt, setUpdatedAt] = useState('');

    /** 项目数量统计（展示中 / 已隐藏 在本地统计，见 STAT_PAGE_SIZE 注释） */
    const loadStats = useCallback(async () => {
        try {
            const result = await listProjects({pageNum: 1, pageSize: STAT_PAGE_SIZE});
            const rows = result.data?.rows ?? [];
            const shown = rows.filter(row => row.status === '0').length;
            setStats({total: rows.length, shown, hidden: rows.length - shown});
            setUpdatedAt(`${pad(new Date().getHours())}:${pad(new Date().getMinutes())}`);
        } catch {
            // 统计失败不影响列表本身，保留上一次的数值
        }
    }, []);

    useEffect(() => {
        void loadStats();
    }, [loadStats]);

    const refreshAll = useCallback(() => {
        tableRef.current?.reload();
        void loadStats();
    }, [loadStats]);

    const openForm = async (row?: ProjectRecord) => {
        setEditing(row?.projectId ? (await getProject(row.projectId)).data : undefined);
        setOpen(true);
    };

    const confirmDelete = async (ids: React.Key[]) => {
        await deleteProjects(ids);
        message.success('删除成功');
        setSelected([]);
        refreshAll();
    };

    const afterEdit = () => {
        message.success(editing?.projectId ? '修改成功' : '新增成功');
        setOpen(false);
        refreshAll();
    };

    const copyPageAddress = (row: ProjectRecord) => {
        const address = `${window.location.origin}/#/sunpalaceartspace/project-preview?projectId=${row.projectId}`;
        navigator.clipboard.writeText(address).then(() => message.success('页面地址已复制'));
    };

    const columns: ProColumns<ProjectRecord>[] = [{
        title: '标题',
        dataIndex: 'projectName',
        ellipsis: true
    }];

    const statItems: StatFilterItem[] = [{
        key: 'total',
        label: '项目总数',
        value: stats.total,
        icon: <AppstoreOutlined/>,
        color: '#4f6ef7',
        background: '#eef1fe',
        clickable: false
    }, {
        key: 'shown',
        label: '展示中',
        value: stats.shown,
        icon: <EyeOutlined/>,
        color: '#10b981',
        background: '#e7f8f1',
        clickable: false
    }, {
        key: 'hidden',
        label: '已隐藏',
        value: stats.hidden,
        icon: <EyeInvisibleOutlined/>,
        color: '#f59e0b',
        background: '#fef4e2',
        clickable: false
    }];

    return <PageContainer pageHeaderRender={false}
                          token={{paddingInlinePageContainerContent: 0, paddingBlockPageContainerContent: 0}}>
        <div className={styles.page}>
            <PageHero
                crumb={<>艺术空间&nbsp;›&nbsp;<b>项目管理</b></>}
                title="项目管理"
                description="配置小程序端可预约的艺术空间项目、时段规则与预约须知"
                updatedAt={updatedAt}
                onRefresh={refreshAll}
            />
            <div className={styles.content}>
                <StatFilterBar items={statItems}/>
                <ProTable<ProjectRecord>
                    rowKey="projectId"
                    actionRef={tableRef}
                    columns={columns}
                    tableAlertRender={false}
                    cardProps={{bodyStyle: {padding: '14px 16px'}}}
                    rowSelection={{selectedRowKeys: selected, onChange: setSelected}}
                    request={async ({current, pageSize, ...params}) => {
                        const result = await listProjects({...params, pageNum: current, pageSize});
                        return {
                            data: result.data.rows,
                            total: result.data.total,
                            success: result.code === 200 || result.code == 0
                        };
                    }}
                    tableViewRender={({dataSource, loading, rowSelection}) => {
                        const spinning = loading === true || (typeof loading === 'object' && loading?.spinning);
                        if (spinning) {
                            return <Row gutter={[16, 16]}>{Array.from({length: 8}).map((_, i) => (
                                <Col key={i} xs={24} sm={12} md={8} lg={8} xl={6}>
                                    <div className={styles.projectCard}>
                                        <Skeleton.Image active style={{width: '100%', height: 168}}/>
                                        <div className={styles.projectBody}>
                                            <Skeleton active paragraph={{rows: 3}}/>
                                        </div>
                                    </div>
                                </Col>
                            ))}</Row>;
                        }
                        if (!dataSource?.length) {
                            return <Empty description="暂无项目，点击右上角「新增」创建" style={{padding: 48}}/>;
                        }
                        const selectedKeys = new Set((rowSelection?.selectedRowKeys ?? []) as React.Key[]);
                        const toggle = (row: ProjectRecord, checked: boolean) => {
                            const key = row.projectId!;
                            const next = new Set(selectedKeys);
                            if (checked) next.add(key); else next.delete(key);
                            const keys = Array.from(next);
                            const rows = dataSource.filter(r => keys.includes(r.projectId!));
                            rowSelection?.onChange?.(keys, rows, {type: 'multiple' as any});
                        };
                        return <Row className={styles.cardGrid} gutter={[16, 16]}>
                            {dataSource.map(row => <Col key={row.projectId} xs={24} sm={12} md={8} lg={8} xl={6}>
                                <ProjectCard
                                    project={row}
                                    selected={selectedKeys.has(row.projectId!)}
                                    onSelectChange={checked => toggle(row, checked)}
                                    onEdit={() => void openForm(row)}
                                    onCopy={() => copyPageAddress(row)}
                                    onDelete={() => modal.confirm({
                                        title: `确认删除项目「${row.projectName}」？`,
                                        content: '删除后该项目将不再展示，且不可恢复。',
                                        onOk: async () => await confirmDelete([row.projectId!])
                                    })}
                                />
                            </Col>)}
                        </Row>;
                    }}
                    toolBarRender={() => [
                        <PermissionButton key="add" type="primary" icon={<PlusOutlined/>}
                                          permission="sunpalaceartspace:project:add"
                                          onClick={() => void openForm()}>新增项目</PermissionButton>,
                        <PermissionButton key="batchDelete" danger icon={<DeleteOutlined/>}
                                          permission="sunpalaceartspace:project:remove"
                                          disabled={!selected.length}
                                          onClick={() => modal.confirm({
                                              title: `确认删除选中的 ${selected.length} 个项目？`,
                                              onOk: async () => await confirmDelete(selected)
                                          })}>批量删除</PermissionButton>
                    ]}
                />
            </div>
        </div>
        <ModalForm<ProjectFormValues>
            title={editing?.projectId ? '编辑项目' : '添加项目'}
            open={open}
            initialValues={toFormValues(editing)}
            layout="vertical"
            labelAlign="left"
            modalProps={{destroyOnHidden: true, width: 900, onCancel: () => setOpen(false)}}
            onFinish={async (values) => {
                const data: ProjectRecord = {
                    ...editing,
                    ...values,
                    cutOffTime: toStr(values.cutOffTime),
                    reservationStartTime: toStr(values.reservationStartTime),
                    reservationEndTime: toStr(values.reservationEndTime)
                };
                if (editing?.projectId) {
                    await updateProject(data);
                } else {
                    await addProject(data);
                }
                afterEdit();
                return true;
            }}
        >
            <ProCard
                bordered
                size="small"
                headerBordered
                style={{marginBottom: 16}}
                title={sectionTitle(<AppstoreOutlined/>, '基本信息', '#1677ff', '#e6f4ff')}
                extra={sectionHint('用于小程序端项目列表展示')}
            >
                <Row gutter={16}>
                    <Col span={8}>
                        <ProFormItem name="coverArtUrl" label="封面图"
                                     rules={[{required: true, message: '封面图不能为空'}]}>
                            <CoverUpload/>
                        </ProFormItem>
                    </Col>
                    <Col span={16}>
                        <Row gutter={16}>
                            <Col span={14}>
                                <ProFormText name="projectName" label="项目名称" placeholder="请输入项目名称"
                                             fieldProps={{style: {width: '100%'}}}
                                             rules={[{required: true, message: '项目名称不能为空'}]}/>
                            </Col>
                            <Col span={10}>
                                <ProFormDigit name="sort" label="排序" placeholder="数值越小越靠前" min={0} precision={0}
                                              fieldProps={{style: {width: '100%'}}}
                                              tooltip="列表展示顺序，数值越小越靠前"/>
                            </Col>
                            <Col span={24}>
                                <ProFormRadio.Group name="status" label="展示状态"
                                                    options={[{label: '展示', value: '0'}, {label: '隐藏', value: '1'}]}/>
                            </Col>
                        </Row>
                    </Col>
                </Row>
                <TipBox icon={<InfoCircleOutlined/>} bg="#fafafa" border="#f0f0f0" color="#8c8c8c"
                        text="封面建议 200*200；排序值越小越靠前；设为「隐藏」后项目不会出现在小程序列表中。"/>
            </ProCard>

            <ProCard
                bordered
                size="small"
                headerBordered
                style={{marginBottom: 16}}
                title={sectionTitle(<ClockCircleOutlined/>, '预约规则', '#fa8c16', '#fff7e6')}
                extra={sectionHint('决定用户可预约的时间与人数')}
            >
                <Row gutter={16}>
                    <Col span={8}>
                        <ProFormTimePicker name="reservationStartTime" label="开始时间" placeholder="请选择开始时间"
                                           fieldProps={{format: 'HH:mm:ss', allowClear: true, style: {width: '100%'}}}/>
                    </Col>
                    <Col span={8}>
                        <ProFormTimePicker name="reservationEndTime" label="结束时间" placeholder="请选择结束时间"
                                           fieldProps={{format: 'HH:mm:ss', allowClear: true, style: {width: '100%'}}}/>
                    </Col>
                    <Col span={8}>
                        <ProFormTimePicker name="cutOffTime" label="预约截止" placeholder="请选择截止时间"
                                           fieldProps={{format: 'HH:mm:ss', allowClear: true, style: {width: '100%'}}}
                                           tooltip="每日可预约的截止时间，过了时间只能预约第二天"/>
                    </Col>
                    <Col span={8}>
                        <ProFormDigit name="reservationIntervalSecond" label="预约间隔" placeholder="请输入间隔时间"
                                      min={0} precision={0} fieldProps={{suffix: '分钟', style: {width: '100%'}}}
                                      tooltip="相邻两个预约时段之间的间隔"/>
                    </Col>
                    <Col span={8}>
                        <ProFormDigit name="reservationStaySecond" label="停留时长" placeholder="可停留的时长"
                                      min={0} precision={0} fieldProps={{suffix: '秒', style: {width: '100%'}}}
                                      tooltip="用户在预约界面可停留的时长"/>
                    </Col>
                    <Col span={8}>
                        <ProFormDigit name="advanceReservationDays" label="提前天数" placeholder="可提前预约的天数"
                                      min={0} precision={0} fieldProps={{suffix: '天', style: {width: '100%'}}}
                                      tooltip="最多可提前几天预约"/>
                    </Col>
                    <Col span={12}>
                        <ProFormDigit name="reservationCount" label="时段人数" placeholder="每个时段可预约人数"
                                      min={0} precision={0} fieldProps={{suffix: '人', style: {width: '100%'}}}
                                      tooltip="每个时间段可预约的人数"/>
                    </Col>
                    <Col span={12}>
                        <ProFormDigit name="travelerCount" label="随行人数" placeholder="可随行的人数"
                                      min={0} precision={0} fieldProps={{suffix: '人', style: {width: '100%'}}}
                                      tooltip="单个预约最多可携带的随行人数"/>
                    </Col>
                </Row>
                <ProFormDependency name={[
                    'reservationStartTime',
                    'reservationEndTime',
                    'cutOffTime',
                    'reservationIntervalSecond',
                    'reservationStaySecond',
                    'reservationCount',
                    'travelerCount',
                    'advanceReservationDays'
                ]}>
                    {(values) => {
                        const start = toStr(values.reservationStartTime);
                        const end = toStr(values.reservationEndTime);
                        const cutOff = toStr(values.cutOffTime);
                        const parts: string[] = [];
                        if (start || end) parts.push(`可预约时段 ${start || '未设置'} - ${end || '未设置'}`);
                        if (values.reservationIntervalSecond != null) parts.push(`每场间隔 ${values.reservationIntervalSecond} 分钟`);
                        if (values.reservationCount != null) parts.push(`每场 ${values.reservationCount} 人`);
                        if (values.travelerCount != null) parts.push(`最多随行 ${values.travelerCount} 人`);
                        if (values.advanceReservationDays != null) parts.push(`最多提前 ${values.advanceReservationDays} 天预约`);
                        if (cutOff) parts.push(`每日 ${cutOff} 后停止预约`);
                        if (values.reservationStaySecond != null) parts.push(`可停留 ${values.reservationStaySecond} 秒`);
                        return <TipBox icon={<InfoCircleOutlined/>} text={parts.length
                            ? `规则预览：${parts.join('；')}`
                            : '规则预览：填写上方规则后，这里会实时显示预约规则摘要'}/>;
                    }}
                </ProFormDependency>
            </ProCard>

            <ProCard
                bordered
                size="small"
                headerBordered
                title={sectionTitle(<FileTextOutlined/>, '预约须知', '#52c41a', '#f6ffed')}
                extra={sectionHint('展示在小程序预约须知页面')}
            >
                <ProFormRichText name="reservationNotes" label="须知内容" placeholder="请输入预约须知"
                                 rules={[{required: true, message: '预约须知不能为空'}]}/>
            </ProCard>
        </ModalForm>
    </PageContainer>;
}
