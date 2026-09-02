import { DeleteOutlined, DownloadOutlined, EditOutlined, PlusOutlined, UserAddOutlined } from '@ant-design/icons';
import { ModalForm, PageContainer, ProFormDigit, ProFormRadio, ProFormSelect, ProFormText, ProFormTextArea, ProFormTreeSelect, ProTable } from '@ant-design/pro-components';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { App, Drawer, Space, Switch } from 'antd';
import { useRef, useState } from 'react';
import { PermissionButton } from '@/components/PermissionButton';
import { useDict } from '@/hooks/useDict';
import {
  addRole, cancelUserRole, cancelUserRoles, changeRoleStatus, deleteRoles, getMenuTree, getRole,
  getRoleDeptTree, getRoleMenuTree, listAllocatedUsers, listRoles, listUnallocatedUsers,
  selectRoleUsers, updateDataScope, updateRole
} from '@/services/system/role';
import type { RoleRecord } from '@/services/system/role';
import type { TreeOption, UserRecord } from '@/services/system/user';
import { downloadFile } from '@/utils/download';

export default function RolePage() {
  const { message, modal } = App.useApp();
  const actionRef = useRef<ActionType>();
  const authActionRef = useRef<ActionType>();
  const selectActionRef = useRef<ActionType>();
  const [selectedKeys, setSelectedKeys] = useState<React.Key[]>([]);
  const [editing, setEditing] = useState<RoleRecord>();
  const [formOpen, setFormOpen] = useState(false);
  const [menuTree, setMenuTree] = useState<TreeOption[]>([]);
  const [dataScopeOpen, setDataScopeOpen] = useState(false);
  const [deptTree, setDeptTree] = useState<TreeOption[]>([]);
  const [authRole, setAuthRole] = useState<RoleRecord>();
  const [authOpen, setAuthOpen] = useState(false);
  const [selectOpen, setSelectOpen] = useState(false);
  const [authUserIds, setAuthUserIds] = useState<React.Key[]>([]);
  const [candidateIds, setCandidateIds] = useState<React.Key[]>([]);
  const normalDict = useDict('sys_normal_disable');

  const openRoleForm = async (record?: RoleRecord) => {
    if (record?.roleId) {
      const [roleResult, treeResult] = await Promise.all([getRole(record.roleId), getRoleMenuTree(record.roleId)]);
      setEditing({ ...roleResult.data, menuIds: treeResult.checkedKeys || [] });
      setMenuTree(treeResult.data || []);
    } else {
      setEditing({ roleName: '', roleKey: '', roleSort: 0, status: '0', menuCheckStrictly: true, menuIds: [] });
      setMenuTree((await getMenuTree()).data || []);
    }
    setFormOpen(true);
  };

  const openDataScope = async (record: RoleRecord) => {
    const [roleResult, treeResult] = await Promise.all([getRole(record.roleId!), getRoleDeptTree(record.roleId!)]);
    setEditing({ ...roleResult.data, deptIds: treeResult.checkedKeys || [] });
    setDeptTree(treeResult.data || []);
    setDataScopeOpen(true);
  };

  const columns: ProColumns<RoleRecord>[] = [
    { title: '角色编号', dataIndex: 'roleId', search: false, width: 100 },
    { title: '角色名称', dataIndex: 'roleName' },
    { title: '权限字符', dataIndex: 'roleKey' },
    { title: '显示顺序', dataIndex: 'roleSort', search: false, width: 100 },
    { title: '状态', dataIndex: 'status', valueType: 'select', valueEnum: Object.fromEntries(normalDict.options.map((item) => [item.value, { text: item.label }])), render: (_, record) => <Switch checked={record.status === '0'} disabled={record.roleId === 1} onChange={async (checked) => { await changeRoleStatus(record.roleId!, checked ? '0' : '1'); message.success('状态修改成功'); actionRef.current?.reload(); }} /> },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      valueType: 'dateRange',
      width: 170,
      search: {
        transform: (value) => ({ params: { beginTime: value?.[0], endTime: value?.[1] } })
      },
      render: (_, record) => record.createTime || '-'
    },
    { title: '操作', valueType: 'option', width: 260, render: (_, record) => record.roleId === 1 ? null : [
      <PermissionButton key="edit" type="link" size="small" permission="system:role:edit" onClick={() => void openRoleForm(record)}>修改</PermissionButton>,
      <PermissionButton key="scope" type="link" size="small" permission="system:role:edit" onClick={() => void openDataScope(record)}>数据权限</PermissionButton>,
      <PermissionButton key="users" type="link" size="small" permission="system:role:edit" onClick={() => { setAuthRole(record); setAuthOpen(true); }}>分配用户</PermissionButton>,
      <PermissionButton key="remove" type="link" danger size="small" permission="system:role:remove" onClick={() => modal.confirm({ title: `确认删除角色「${record.roleName}」？`, onOk: async () => { await deleteRoles([record.roleId!]); message.success('删除成功'); actionRef.current?.reload(); } })}>删除</PermissionButton>
    ] }
  ];

  const userColumns: ProColumns<UserRecord>[] = [
    { title: '用户名称', dataIndex: 'userName' },
    { title: '用户昵称', dataIndex: 'nickName', search: false },
    { title: '邮箱', dataIndex: 'email', search: false },
    { title: '手机', dataIndex: 'phonenumber' },
    { title: '状态', dataIndex: 'status', search: false, render: (_, record) => record.status === '0' ? '正常' : '停用' },
    { title: '创建时间', dataIndex: 'createTime', search: false },
    { title: '操作', valueType: 'option', render: (_, record) => [<PermissionButton key="cancel" type="link" danger permission="system:role:remove" onClick={async () => { await cancelUserRole({ roleId: authRole!.roleId!, userId: record.userId! }); message.success('取消授权成功'); authActionRef.current?.reload(); }}>取消授权</PermissionButton>] }
  ];

  return <PageContainer>
    <ProTable<RoleRecord>
      rowKey="roleId" actionRef={actionRef} columns={columns}
      rowSelection={{ selectedRowKeys: selectedKeys, onChange: setSelectedKeys }}
      pagination={{ defaultPageSize: 10 }}
      request={async ({ current, pageSize, ...params }) => { const response = await listRoles({ ...params, pageNum: current, pageSize }); return { data: response.rows, total: response.total, success: response.code === 200 }; }}
      toolBarRender={() => [
        <PermissionButton key="add" type="primary" icon={<PlusOutlined />} permission="system:role:add" onClick={() => void openRoleForm()}>新增</PermissionButton>,
        <PermissionButton key="edit" icon={<EditOutlined />} permission="system:role:edit" disabled={selectedKeys.length !== 1} onClick={() => void openRoleForm({ roleId: Number(selectedKeys[0]), roleName: '', roleKey: '', roleSort: 0 })}>修改</PermissionButton>,
        <PermissionButton key="delete" danger icon={<DeleteOutlined />} permission="system:role:remove" disabled={!selectedKeys.length} onClick={() => modal.confirm({ title: `确认删除角色编号「${selectedKeys.join(',')}」？`, onOk: async () => { await deleteRoles(selectedKeys); setSelectedKeys([]); message.success('删除成功'); actionRef.current?.reload(); } })}>删除</PermissionButton>,
        <PermissionButton key="export" icon={<DownloadOutlined />} permission="system:role:export" onClick={() => void downloadFile('/system/role/export', {}, `role_${Date.now()}.xlsx`)}>导出</PermissionButton>
      ]}
    />

    <ModalForm<RoleRecord> title={editing?.roleId ? '修改角色' : '添加角色'} open={formOpen} initialValues={editing} modalProps={{ destroyOnClose: true, onCancel: () => setFormOpen(false) }} onFinish={async (values) => { const data = { ...editing, ...values }; if (editing?.roleId) await updateRole(data); else await addRole(data); message.success(editing?.roleId ? '修改成功' : '新增成功'); setFormOpen(false); actionRef.current?.reload(); return true; }}>
      <ProFormText name="roleName" label="角色名称" rules={[{ required: true }, { min: 2, max: 30 }]} />
      <ProFormText name="roleKey" label="权限字符" rules={[{ required: true }]} />
      <ProFormDigit name="roleSort" label="角色顺序" min={0} rules={[{ required: true }]} />
      <ProFormRadio.Group name="status" label="状态" options={normalDict.options} />
      <ProFormTreeSelect name="menuIds" label="菜单权限" fieldProps={{ treeData: menuTree, treeCheckable: true, treeDefaultExpandAll: true, showCheckedStrategy: 'SHOW_ALL', fieldNames: { label: 'label', value: 'id' } }} />
      <ProFormRadio.Group name="menuCheckStrictly" label="父子联动" options={[{ label: '联动', value: true }, { label: '独立', value: false }]} />
      <ProFormTextArea name="remark" label="备注" />
    </ModalForm>

    <ModalForm<RoleRecord> title="分配数据权限" open={dataScopeOpen} initialValues={editing} modalProps={{ destroyOnClose: true, onCancel: () => setDataScopeOpen(false) }} onFinish={async (values) => { await updateDataScope({ ...editing, ...values }); message.success('修改成功'); setDataScopeOpen(false); actionRef.current?.reload(); return true; }}>
      <ProFormText name="roleName" label="角色名称" disabled />
      <ProFormText name="roleKey" label="权限字符" disabled />
      <ProFormSelect name="dataScope" label="权限范围" options={[{ label: '全部数据权限', value: '1' }, { label: '自定数据权限', value: '2' }, { label: '本部门数据权限', value: '3' }, { label: '本部门及以下数据权限', value: '4' }, { label: '仅本人数据权限', value: '5' }]} />
      <ProFormTreeSelect name="deptIds" label="数据权限" fieldProps={{ treeData: deptTree, treeCheckable: true, treeDefaultExpandAll: true, showCheckedStrategy: 'SHOW_ALL', fieldNames: { label: 'label', value: 'id' } }} />
      <ProFormRadio.Group name="deptCheckStrictly" label="父子联动" options={[{ label: '联动', value: true }, { label: '独立', value: false }]} />
    </ModalForm>

    <Drawer title={`分配用户 - ${authRole?.roleName || ''}`} width="88%" open={authOpen} onClose={() => setAuthOpen(false)} destroyOnClose>
      <ProTable<UserRecord> rowKey="userId" actionRef={authActionRef} columns={userColumns} rowSelection={{ selectedRowKeys: authUserIds, onChange: setAuthUserIds }} request={async ({ current, pageSize, ...params }) => { const response = await listAllocatedUsers({ ...params, roleId: authRole?.roleId, pageNum: current, pageSize }); return { data: response.rows, total: response.total, success: response.code === 200 }; }} toolBarRender={() => [
        <PermissionButton key="add" type="primary" icon={<UserAddOutlined />} permission="system:role:add" onClick={() => setSelectOpen(true)}>添加用户</PermissionButton>,
        <PermissionButton key="cancel" danger permission="system:role:remove" disabled={!authUserIds.length} onClick={async () => { await cancelUserRoles({ roleId: authRole!.roleId!, userIds: authUserIds.join(',') }); setAuthUserIds([]); message.success('取消授权成功'); authActionRef.current?.reload(); }}>批量取消授权</PermissionButton>
      ]} />
    </Drawer>

    <Drawer title="选择用户" width="75%" open={selectOpen} onClose={() => setSelectOpen(false)} extra={<Space><PermissionButton type="primary" permission="system:role:add" disabled={!candidateIds.length} onClick={async () => { await selectRoleUsers({ roleId: authRole!.roleId!, userIds: candidateIds.join(',') }); setCandidateIds([]); setSelectOpen(false); message.success('授权成功'); authActionRef.current?.reload(); }}>确认授权</PermissionButton></Space>}>
      <ProTable<UserRecord> rowKey="userId" actionRef={selectActionRef} columns={userColumns.filter((column) => column.dataIndex !== 'createTime' && column.valueType !== 'option')} rowSelection={{ selectedRowKeys: candidateIds, onChange: setCandidateIds }} request={async ({ current, pageSize, ...params }) => { const response = await listUnallocatedUsers({ ...params, roleId: authRole?.roleId, pageNum: current, pageSize }); return { data: response.rows, total: response.total, success: response.code === 200 }; }} />
    </Drawer>
  </PageContainer>;
}
