import {PlusOutlined, SaveOutlined} from '@ant-design/icons';
import {
    ModalForm,
    PageContainer,
    ProFormDigit,
    ProFormRadio,
    ProFormText,
    ProFormTreeSelect,
    ProTable
} from '@ant-design/pro-components';
import type {ActionType, ProColumns} from '@ant-design/pro-components';
import {App, InputNumber, Tag} from 'antd';
import {useRef, useState} from 'react';
import {PermissionButton} from '@/components/PermissionButton';
import {useDict} from '@/hooks/useDict';
import {
    addDept,
    deleteDept,
    getDept,
    listDepts,
    listDeptsExclude,
    updateDept,
    updateDeptSort
} from '@/services/system/dept';
import type {DeptRecord} from '@/services/system/dept';
import {buildTree} from '@/utils/tree';

export default function DeptPage() {
    const {message, modal} = App.useApp();
    const actionRef = useRef<ActionType>();
    const [editing, setEditing] = useState<DeptRecord>();
    const [open, setOpen] = useState(false);
    const [options, setOptions] = useState<DeptRecord[]>([]);
    const [orders, setOrders] = useState<Record<number, number>>({});
    const normalDict = useDict('sys_normal_disable');
    const openForm = async (record?: DeptRecord, parentId?: number) => {
        if (record?.deptId) {
            const [detail, tree] = await Promise.all([getDept(record.deptId), listDeptsExclude(record.deptId)]);
            setEditing(detail.data);
            setOptions(buildTree(tree.data || [], 'deptId', 'parentId'));
        } else {
            setEditing({parentId: parentId ?? 0, deptName: '', orderNum: 0, status: '0'});
            setOptions(buildTree((await listDepts()).data || [], 'deptId', 'parentId'));
        }
        setOpen(true);
    };
    const columns: ProColumns<DeptRecord>[] = [
        {title: '部门名称', dataIndex: 'deptName', width: 260},
        {
            title: '排序',
            dataIndex: 'orderNum',
            search: false,
            width: 120,
            render: (_, r) => <InputNumber min={0} value={orders[r.deptId!] ?? r.orderNum}
                                           onChange={(v) => setOrders((s) => ({...s, [r.deptId!]: v ?? 0}))}/>
        },
        {
            title: '状态',
            dataIndex: 'status',
            valueType: 'select',
            valueEnum: Object.fromEntries(normalDict.options.map((i) => [i.value, {text: i.label}])),
            render: (_, r) => <Tag
                color={r.status === '0' ? 'success' : 'error'}>{r.status === '0' ? '正常' : '停用'}</Tag>
        },
        {title: '创建时间', dataIndex: 'createTime', search: false},
        {
            title: '操作',
            valueType: 'option',
            render: (_, r) => [<PermissionButton key="e" type="link" permission="system:dept:edit"
                                                 onClick={() => void openForm(r)}>修改</PermissionButton>,
                <PermissionButton key="a" type="link" permission="system:dept:add"
                                  onClick={() => void openForm(undefined, r.deptId)}>新增</PermissionButton>, r.parentId !== 0 &&
                <PermissionButton key="d" type="link" danger permission="system:dept:remove"
                                  onClick={() => modal.confirm({
                                      title: `确认删除部门「${r.deptName}」？`,
                                      onOk: async () => {
                                          await deleteDept(r.deptId!);
                                          message.success('删除成功');
                                          actionRef.current?.reload();
                                      }
                                  })}>删除</PermissionButton>]
        }
    ];
    return <PageContainer><ProTable<DeptRecord> rowKey="deptId" actionRef={actionRef} columns={columns}
                                                pagination={false} defaultExpandAllRows request={async (p) => {
        const r = await listDepts(p);
        return {data: buildTree(r.data || [], 'deptId', 'parentId'), success: r.code === 200};
    }} toolBarRender={() => [<PermissionButton key="a" type="primary" icon={<PlusOutlined/>}
                                               permission="system:dept:add"
                                               onClick={() => void openForm()}>新增</PermissionButton>,
        <PermissionButton key="s" icon={<SaveOutlined/>} permission="system:dept:edit"
                          disabled={!Object.keys(orders).length} onClick={async () => {
            const ids = Object.keys(orders);
            await updateDeptSort({deptIds: ids.join(','), orderNums: ids.map((id) => orders[Number(id)]).join(',')});
            setOrders({});
            message.success('排序保存成功');
            actionRef.current?.reload();
        }}>保存排序</PermissionButton>]}/>
        <ModalForm<DeptRecord> title={editing?.deptId ? '修改部门' : '添加部门'} open={open} initialValues={editing}
                               grid modalProps={{destroyOnClose: true, onCancel: () => setOpen(false)}}
                               onFinish={async (v) => {
                                   const data = {...editing, ...v};
                                   editing?.deptId ? await updateDept(data) : await addDept(data);
                                   message.success(editing?.deptId ? '修改成功' : '新增成功');
                                   setOpen(false);
                                   actionRef.current?.reload();
                                   return true;
                               }}>
            <ProFormTreeSelect name="parentId" label="上级部门" colProps={{span: 24}} fieldProps={{
                treeData: options,
                fieldNames: {label: 'deptName', value: 'deptId'},
                treeDefaultExpandAll: true
            }} rules={[{required: true}]}/>
            <ProFormText name="deptName" label="部门名称" colProps={{span: 12}}
                         rules={[{required: true}]}/><ProFormDigit name="orderNum" label="显示排序"
                                                                   colProps={{span: 12}} min={0}
                                                                   rules={[{required: true}]}/>
            <ProFormText name="leader" label="负责人" colProps={{span: 12}}/><ProFormText name="phone" label="联系电话"
                                                                                          colProps={{span: 12}}
                                                                                          rules={[{
                                                                                              pattern: /^1[3-9]\d{9}$/,
                                                                                              message: '请输入正确的手机号码'
                                                                                          }]}/>
            <ProFormText name="email" label="邮箱" colProps={{span: 12}} rules={[{type: 'email'}]}/><ProFormRadio.Group
            name="status" label="部门状态" colProps={{span: 12}} options={normalDict.options}/>
        </ModalForm></PageContainer>;
}
