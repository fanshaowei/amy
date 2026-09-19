import {CopyOutlined, DeleteOutlined, EditOutlined, PlusOutlined} from '@ant-design/icons';
import {
    ModalForm,
    PageContainer,
    ProFormDigit,
    ProFormItem,
    ProFormRadio,
    ProFormText,
    ProFormTimePicker,
    ProTable
} from '@ant-design/pro-components';
import type {ActionType, ProColumns} from '@ant-design/pro-components';
import {App, Image, Tag, Typography} from 'antd';
import dayjs, {Dayjs} from 'dayjs';
import {useRef, useState} from 'react';
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
import {CoverUpload} from './CoverUpload';

/** 表单值类型：时间字段使用 dayjs 对象，提交时再转回 HH:mm:ss 字符串 */
interface ProjectFormValues extends Omit<ProjectRecord, 'cutOffTime' | 'reservationStartTime' | 'reservationEndTime'> {
    cutOffTime?: Dayjs;
    reservationStartTime?: Dayjs;
    reservationEndTime?: Dayjs;
}

const toDayjs = (value?: string | null) => {
    if (!value) return undefined;
    const parsed = dayjs(`2000-01-01 ${value}`);
    return parsed.isValid() ? parsed : undefined;
};
/**
 * ProForm 提交时已把 TimePicker 的值自动转换为 'HH:mm:ss' 字符串（valueType: 'time'），
 * 这里只在值仍是 dayjs 对象时做格式化，避免 dayjs 核心解析纯时间字符串得到 Invalid Date。
 */
const toStr = (value?: Dayjs | string | null) => {
    if (!value) return undefined;
    return dayjs.isDayjs(value) ? value.format('HH:mm:ss') : value;
};
const toFormValues = (record?: ProjectRecord): ProjectFormValues | undefined => record ? {
    ...record,
    cutOffTime: toDayjs(record.cutOffTime),
    reservationStartTime: toDayjs(record.reservationStartTime),
    reservationEndTime: toDayjs(record.reservationEndTime)
} : {projectName: '', coverArtUrl: '', status: '0', sort: 0, reservationNotes: ''};

export default function ProjectPage() {
    const {message, modal} = App.useApp();
    const tableRef = useRef<ActionType>();
    const [selected, setSelected] = useState<React.Key[]>([]);
    const [editing, setEditing] = useState<ProjectRecord>();
    const [open, setOpen] = useState(false);

    const openForm = async (row?: ProjectRecord) => {
        setEditing(row?.projectId ? (await getProject(row.projectId)).data : undefined);
        setOpen(true);
    };

    const copyPageAddress = (row: ProjectRecord) => {
        const address = `${window.location.origin}/#/sunpalaceartspace/project-preview?projectId=${row.projectId}`;
        navigator.clipboard.writeText(address).then(() => message.success('页面地址已复制'));
    };

    const columns: ProColumns<ProjectRecord>[] = [{
        title: '封面',
        dataIndex: 'coverArtUrl',
        search: false,
        width: 90,
        render: (_, row) => row.coverArtUrl
            ? <Image width={44} height={44} src={row.coverArtUrl} alt={`${row.projectName}封面`}
                     style={{objectFit: 'cover', borderRadius: 4}}/>
            : '-'
    }, {
        title: '标题',
        dataIndex: 'projectName',
        ellipsis: true
    }, {
        title: '可预约时段',
        dataIndex: 'reservationStartTime',
        search: false,
        render: (_, row) => row.reservationStartTime || row.reservationEndTime
            ? `${row.reservationStartTime || ''} - ${row.reservationEndTime || ''}`
            : '-'
    }, {
        title: '预约间隔',
        dataIndex: 'reservationIntervalSecond',
        search: false,
        render: (_, row) => row.reservationIntervalSecond != null ? `${row.reservationIntervalSecond} 分钟` : '-'
    }, {
        title: '时段预约人数',
        dataIndex: 'reservationCount',
        search: false
    }, {
        title: '停留秒数',
        dataIndex: 'reservationStaySecond',
        search: false,
        render: (_, row) => row.reservationStaySecond != null ? `${row.reservationStaySecond} 秒` : '-'
    }, {
        title: '当日截止时间',
        dataIndex: 'cutOffTime',
        search: false
    }, {
        title: '提前预约天数',
        dataIndex: 'advanceReservationDays',
        search: false
    }, {
        title: '页面地址',
        dataIndex: 'pageAddress',
        search: false,
        render: (_, row) => <Typography.Link onClick={() => copyPageAddress(row)}>
            <CopyOutlined/> 点击复制
        </Typography.Link>
    }, {
        title: '展示状态',
        dataIndex: 'status',
        search: false,
        render: (_, row) => <Tag color={row.status === '0' ? 'success' : 'default'}>
            {row.status === '0' ? '展示' : '隐藏'}
        </Tag>
    }, {
        title: '排序',
        dataIndex: 'sort',
        search: false
    }, {
        title: '发布时间',
        dataIndex: 'createTime',
        search: false,
        width: 160
    }, {
        title: '操作',
        valueType: 'option',
        fixed: 'right',
        width: 130,
        render: (_, row) => [
            <PermissionButton key="edit" type="link" size="small" permission="sunpalaceartspace:project:edit"
                              onClick={() => void openForm(row)}>编辑</PermissionButton>,
            <PermissionButton key="delete" type="link" size="small" danger
                              permission="sunpalaceartspace:project:remove"
                              onClick={() => modal.confirm({
                                  title: `确认删除项目「${row.projectName}」？`,
                                  content: '删除后该项目将不再展示，且不可恢复。',
                                  onOk: async () => {
                                      await deleteProjects([row.projectId!]);
                                      message.success('删除成功');
                                      tableRef.current?.reload();
                                  }
                              })}>删除</PermissionButton>
        ]
    }];

    return <PageContainer>
        <ProTable<ProjectRecord>
            rowKey="projectId"
            actionRef={tableRef}
            columns={columns}
            scroll={{x: 1500}}
            rowSelection={{selectedRowKeys: selected, onChange: setSelected}}
            request={async ({current, pageSize, ...params}) => {
                const result = await listProjects({...params, pageNum: current, pageSize});
                return {data: result.data.rows, total: result.data.total, success: result.code === 200 || result.code == 0};
            }}
            toolBarRender={() => [
                <PermissionButton key="add" type="primary" icon={<PlusOutlined/>}
                                  permission="sunpalaceartspace:project:add"
                                  onClick={() => void openForm()}>新增</PermissionButton>,
                <PermissionButton key="batchDelete" danger icon={<DeleteOutlined/>}
                                  permission="sunpalaceartspace:project:remove"
                                  disabled={!selected.length}
                                  onClick={() => modal.confirm({
                                      title: `确认删除选中的 ${selected.length} 个项目？`,
                                      onOk: async () => {
                                          await deleteProjects(selected);
                                          setSelected([]);
                                          message.success('删除成功');
                                          tableRef.current?.reload();
                                      }
                                  })}>删除</PermissionButton>
            ]}
        />
        <ModalForm<ProjectFormValues>
            title={editing?.projectId ? '编辑项目' : '添加项目'}
            open={open}
            initialValues={toFormValues(editing)}
            modalProps={{destroyOnHidden: true, width: 860, onCancel: () => setOpen(false)}}
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
                message.success(editing?.projectId ? '修改成功' : '新增成功');
                setOpen(false);
                tableRef.current?.reload();
                return true;
            }}
        >
            <ProFormText name="projectName" label="项目名称" placeholder="请输入项目名称" rules={[{required: true, message: '项目名称不能为空'}]}/>
            <ProFormItem name="coverArtUrl" label="封面图" rules={[{required: true, message: '封面图不能为空'}]}>
                <CoverUpload/>
            </ProFormItem>
            <ProFormTimePicker name="cutOffTime" label="截止时间" placeholder="请选择截止时间" fieldProps={{format: 'HH:mm:ss', allowClear: true}}
                               extra="每日可预约的截止时间，过了时间就只能预约第二天的"/>
            <ProFormDigit name="reservationStaySecond" label="预约停留秒数" placeholder="请输入停留秒数" min={0} precision={0}
                          fieldProps={{addonAfter: '秒'}} extra="用户在预约界面可停留的时长"/>
            <ProFormTimePicker name="reservationStartTime" label="预约时间" placeholder="请选择开始时间"
                               fieldProps={{format: 'HH:mm:ss', allowClear: true}} colProps={{span: 12}}/>
            <ProFormTimePicker name="reservationEndTime" label=" " placeholder="请选择结束时间" colon={false}
                               fieldProps={{format: 'HH:mm:ss', allowClear: true}} colProps={{span: 12}}/>
            <ProFormDigit name="reservationIntervalSecond" label="间隔时间" placeholder="预约间隔" min={0} precision={0}
                          fieldProps={{addonAfter: '分钟'}} extra="预约间隔，单位：分钟" colProps={{span: 12}}/>
            <ProFormDigit name="reservationCount" label="预约人数" placeholder="每个时间段可预约的人数" min={0} precision={0}
                          extra="每个时间段可预约的人数" colProps={{span: 12}}/>
            <ProFormDigit name="advanceReservationDays" label="预约天数" placeholder="可提前预约的天数" min={0} precision={0}
                          extra="可提前预约几天" colProps={{span: 12}}/>
            <ProFormDigit name="travelerCount" label="随行人数" placeholder="可随行的人数" min={0} precision={0}
                          colProps={{span: 12}}/>
            <ProFormRadio.Group name="status" label="展示状态" options={[{label: '展示', value: '0'}, {label: '隐藏', value: '1'}]}/>
            <ProFormDigit name="sort" label="排序" placeholder="数值越小越靠前" min={0} precision={0}/>
            <ProFormRichText name="reservationNotes" label="预约须知" placeholder="请输入预约须知"
                             rules={[{required: true, message: '预约须知不能为空'}]}/>
        </ModalForm>
    </PageContainer>;
}
