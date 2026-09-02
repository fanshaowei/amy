import {DeleteOutlined, DownloadOutlined, EditOutlined, PlusOutlined, UploadOutlined} from '@ant-design/icons';
import {
    ModalForm,
    PageContainer,
    ProFormRadio,
    ProFormSelect,
    ProFormText,
    ProFormTextArea,
    ProFormTreeSelect,
    ProTable
} from '@ant-design/pro-components';
import type {ActionType, ProColumns} from '@ant-design/pro-components';
import {App, Card, Col, Descriptions, Drawer, Input, Row, Switch, Tree, Upload} from 'antd';
import type {DataNode} from 'antd/es/tree';
import {useEffect, useRef, useState} from 'react';
import {PermissionButton} from '@/components/PermissionButton';
import {useDict} from '@/hooks/useDict';
import {
    addUser,
    changeUserStatus,
    deleteUsers,
    getConfigValue,
    getDeptTree,
    getUser,
    getUserAuthRoles,
    listUsers,
    resetUserPassword,
    updateUser
    , updateUserAuthRoles
} from '@/services/system/user';
import type {AuthRoleOption, PostOption, RoleOption, TreeOption, UserRecord} from '@/services/system/user';
import {downloadFile} from '@/utils/download';

function filterEnabled(nodes: TreeOption[]): TreeOption[] {
    return nodes.filter((node) => !node.disabled).map((node) => ({
        ...node,
        children: node.children ? filterEnabled(node.children) : undefined
    }));
}

function toTreeData(nodes: TreeOption[]): DataNode[] {
    return nodes.map((node) => ({
        key: node.id,
        title: node.label,
        disabled: node.disabled,
        children: node.children ? toTreeData(node.children) : undefined
    }));
}

export default function UserPage() {
    const {message, modal} = App.useApp();
    const actionRef = useRef<ActionType>();
    const [selectedKeys, setSelectedKeys] = useState<React.Key[]>([]);
    const [deptId, setDeptId] = useState<number>();
    const [deptTree, setDeptTree] = useState<TreeOption[]>([]);
    const [editing, setEditing] = useState<UserRecord>();
    const [formOpen, setFormOpen] = useState(false);
    const [detailOpen, setDetailOpen] = useState(false);
    const [detail, setDetail] = useState<UserRecord>();
    const [posts, setPosts] = useState<PostOption[]>([]);
    const [roles, setRoles] = useState<RoleOption[]>([]);
    const [initPassword, setInitPassword] = useState('123456');
    const [authUser, setAuthUser] = useState<UserRecord>();
    const [authRoles, setAuthRoles] = useState<AuthRoleOption[]>([]);
    const [authRoleIds, setAuthRoleIds] = useState<React.Key[]>([]);
    const [authRoleOpen, setAuthRoleOpen] = useState(false);
    const normalDict = useDict('sys_normal_disable');
    const sexDict = useDict('sys_user_sex');

    const refreshTree = async () => setDeptTree((await getDeptTree()).data || []);
    useEffect(() => {
        void refreshTree();
        void getConfigValue('sys.user.initPassword').then((result) => setInitPassword(result.msg || '123456'));
    }, []);

    const openForm = async (record?: UserRecord) => {
        const response = await getUser(record?.userId);
        setPosts(response.posts || []);
        setRoles(response.roles || []);
        setEditing(record ? {
            ...response.data,
            postIds: response.postIds || [],
            roleIds: response.roleIds || [],
            password: ''
        } : {userName: '', nickName: '', password: initPassword, status: '0'});
        setFormOpen(true);
    };

    const columns: ProColumns<UserRecord>[] = [
        {title: '用户编号', dataIndex: 'userId', search: false, width: 90},
        {
            title: '用户名称', dataIndex: 'userName', copyable: true, render: (_, record) => <a onClick={async () => {
                const response = await getUser(record.userId);
                setDetail({...response.data, postIds: response.postIds, roleIds: response.roleIds});
                setPosts(response.posts);
                setRoles(response.roles);
                setDetailOpen(true);
            }}>{record.userName}</a>
        },
        {title: '用户昵称', dataIndex: 'nickName', search: false},
        {title: '部门', dataIndex: ['dept', 'deptName'], search: false},
        {title: '手机号码', dataIndex: 'phonenumber'},
        {
            title: '状态',
            dataIndex: 'status',
            valueType: 'select',
            valueEnum: Object.fromEntries(normalDict.options.map((item) => [item.value, {text: item.label}])),
            render: (_, record) => <Switch checked={record.status === '0'} disabled={record.userId === 1}
                                           onChange={async (checked) => {
                                               const next = checked ? '0' : '1';
                                               try {
                                                   await changeUserStatus(record.userId!, next);
                                                   message.success(`${checked ? '启用' : '停用'}成功`);
                                                   actionRef.current?.reload();
                                               } catch {
                                                   actionRef.current?.reload();
                                               }
                                           }}/>
        },
        {
            title: '创建时间',
            dataIndex: 'createTime',
            valueType: 'dateRange',
            width: 170,
            search: {
                transform: (value) => ({params: {beginTime: value?.[0], endTime: value?.[1]}})
            },
            render: (_, record) => record.createTime || '-'
        },
        {
            title: '操作',
            valueType: 'option',
            width: 230,
            fixed: 'right',
            render: (_, record) => record.userId === 1 ? null : [
                <PermissionButton key="edit" type="link" size="small" permission="system:user:edit"
                                  onClick={() => void openForm(record)}>修改</PermissionButton>,
                <PermissionButton key="pwd" type="link" size="small" permission="system:user:resetPwd" onClick={() => {
                    let password = initPassword;
                    modal.confirm({
                        title: `重置「${record.userName}」密码`,
                        content: <Input.Password defaultValue={initPassword} onChange={(event) => {
                            password = event.target.value;
                        }}/>,
                        onOk: async () => {
                            if (password.length < 5 || password.length > 20) throw new Error('密码长度必须介于 5 和 20 之间');
                            await resetUserPassword(record.userId!, password);
                            message.success(`修改成功，新密码是：${password}`);
                        }
                    });
                }}>重置密码</PermissionButton>,
                <PermissionButton key="roles" type="link" size="small" permission="system:user:edit"
                                  onClick={async () => {
                                      const response = await getUserAuthRoles(record.userId!);
                                      setAuthUser(response.user);
                                      setAuthRoles(response.roles || []);
                                      setAuthRoleIds((response.roles || []).filter((role) => role.flag).map((role) => role.roleId));
                                      setAuthRoleOpen(true);
                                  }}>分配角色</PermissionButton>,
                <PermissionButton key="delete" type="link" danger size="small" permission="system:user:remove"
                                  onClick={() => modal.confirm({
                                      title: `确认删除用户「${record.userName}」？`,
                                      onOk: async () => {
                                          await deleteUsers([record.userId!]);
                                          message.success('删除成功');
                                          actionRef.current?.reload();
                                      }
                                  })}>删除</PermissionButton>
            ]
        }
    ];

    return (
        <PageContainer>
            <Row gutter={16} wrap={false}>
                <Col flex="260px">
                    <Card title="组织机构" extra={<a onClick={() => void refreshTree()}>刷新</a>}
                          styles={{body: {padding: 12}}}>
                        <Tree treeData={toTreeData(deptTree)} defaultExpandAll selectedKeys={deptId ? [deptId] : []}
                              onSelect={(keys) => {
                                  setDeptId(keys[0] as number | undefined);
                                  actionRef.current?.reload();
                              }}/>
                    </Card>
                </Col>
                <Col flex="auto">
                    <ProTable<UserRecord>
                        rowKey="userId"
                        actionRef={actionRef}
                        columns={columns}
                        pagination={{defaultPageSize: 10}}
                        rowSelection={{selectedRowKeys: selectedKeys, onChange: setSelectedKeys}}
                        scroll={{x: 1100}}
                        request={async (params) => {
                            const {current, pageSize, ...queryParams} = params;
                            const response = await listUsers({
                                ...queryParams,
                                deptId,
                                pageNum: current,
                                pageSize
                            });
                            return {data: response.rows, total: response.total, success: response.code === 200};
                        }}
                        toolBarRender={() => [
                            <PermissionButton key="add" type="primary" icon={<PlusOutlined/>}
                                              permission="system:user:add"
                                              onClick={() => void openForm()}>新增</PermissionButton>,
                            <PermissionButton key="edit" icon={<EditOutlined/>} permission="system:user:edit"
                                              disabled={selectedKeys.length !== 1} onClick={() => void openForm({
                                userId: Number(selectedKeys[0]),
                                userName: '',
                                nickName: ''
                            })}>修改</PermissionButton>,
                            <PermissionButton key="delete" danger icon={<DeleteOutlined/>}
                                              permission="system:user:remove" disabled={!selectedKeys.length}
                                              onClick={() => modal.confirm({
                                                  title: `确认删除用户编号「${selectedKeys.join(',')}」？`,
                                                  onOk: async () => {
                                                      await deleteUsers(selectedKeys);
                                                      setSelectedKeys([]);
                                                      message.success('删除成功');
                                                      actionRef.current?.reload();
                                                  }
                                              })}>删除</PermissionButton>,
                            <Upload key="import" showUploadList={false}
                                    action={`${API_BASE_URL}/system/user/importData`}
                                    headers={{Authorization: `Bearer ${document.cookie.match(/Admin-Token=([^;]+)/)?.[1] || ''}`}}
                                    onChange={(info) => {
                                        if (info.file.status === 'done') {
                                            message.success('导入成功');
                                            actionRef.current?.reload();
                                        }
                                    }}><PermissionButton icon={<UploadOutlined/>}
                                                         permission="system:user:import">导入</PermissionButton></Upload>,
                            <PermissionButton key="export" icon={<DownloadOutlined/>} permission="system:user:export"
                                              onClick={() => void downloadFile('/system/user/export', {deptId}, `user_${Date.now()}.xlsx`)}>导出</PermissionButton>
                        ]}
                    />
                </Col>
            </Row>

            <ModalForm<UserRecord>
                title={editing?.userId ? '修改用户' : '添加用户'}
                open={formOpen}
                initialValues={editing}
                modalProps={{destroyOnClose: true, onCancel: () => setFormOpen(false)}}
                grid
                onFinish={async (values) => {
                    const data = {...editing, ...values};
                    if (editing?.userId) await updateUser(data); else await addUser(data);
                    message.success(editing?.userId ? '修改成功' : '新增成功');
                    setFormOpen(false);
                    actionRef.current?.reload();
                    return true;
                }}
            >
                <ProFormText name="nickName" label="用户昵称" colProps={{span: 12}}
                             rules={[{required: true, message: '用户昵称不能为空'}]}/>
                <ProFormTreeSelect name="deptId" label="归属部门" colProps={{span: 12}} fieldProps={{
                    treeData: filterEnabled(deptTree),
                    fieldNames: {label: 'label', value: 'id'}
                }}/>
                <ProFormText name="phonenumber" label="手机号码" colProps={{span: 12}}
                             rules={[{pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码'}]}/>
                <ProFormText name="email" label="邮箱" colProps={{span: 12}}
                             rules={[{type: 'email', message: '请输入正确的邮箱地址'}]}/>
                {!editing?.userId && <ProFormText name="userName" label="用户名称" colProps={{span: 12}}
                                                  rules={[{required: true}, {min: 2, max: 20}]}/>}
                {!editing?.userId && <ProFormText.Password name="password" label="用户密码" colProps={{span: 12}}
                                                           rules={[{required: true}, {min: 5, max: 20}]}/>}
                <ProFormSelect name="sex" label="用户性别" colProps={{span: 12}} options={sexDict.options}/>
                <ProFormRadio.Group name="status" label="状态" colProps={{span: 12}} options={normalDict.options}/>
                <ProFormSelect name="postIds" label="岗位" colProps={{span: 12}} mode="multiple"
                               options={posts.map((post) => ({
                                   label: post.postName,
                                   value: post.postId,
                                   disabled: post.status === '1'
                               }))}/>
                <ProFormSelect name="roleIds" label="角色" colProps={{span: 12}} mode="multiple"
                               options={roles.map((role) => ({
                                   label: role.roleName,
                                   value: role.roleId,
                                   disabled: role.status === '1'
                               }))}/>
                <ProFormTextArea name="remark" label="备注" colProps={{span: 24}}/>
            </ModalForm>

            <Drawer title="用户详情" width={720} open={detailOpen} onClose={() => setDetailOpen(false)}>
                <Descriptions bordered column={2} items={[
                    {key: 'nickName', label: '用户名称', children: detail?.nickName || '-'},
                    {key: 'dept', label: '归属部门', children: detail?.dept?.deptName || '-'},
                    {key: 'phone', label: '手机号码', children: detail?.phonenumber || '-'},
                    {key: 'email', label: '邮箱', children: detail?.email || '-'},
                    {key: 'account', label: '登录账号', children: detail?.userName || '-'},
                    {key: 'status', label: '用户状态', children: detail?.status === '0' ? '正常' : '停用'},
                    {
                        key: 'post',
                        label: '岗位',
                        children: posts.filter((item) => detail?.postIds?.includes(item.postId)).map((item) => item.postName).join('、') || '无岗位'
                    },
                    {
                        key: 'role',
                        label: '角色',
                        children: roles.filter((item) => detail?.roleIds?.includes(item.roleId)).map((item) => item.roleName).join('、') || '无角色'
                    },
                    {key: 'remark', label: '备注', span: 2, children: detail?.remark || '-'}
                ]}/>
            </Drawer>
            <Drawer title={`分配角色 - ${authUser?.nickName || ''}`} width={760} open={authRoleOpen}
                    onClose={() => setAuthRoleOpen(false)}
                    extra={<PermissionButton type="primary" permission="system:user:edit" onClick={async () => {
                        await updateUserAuthRoles(authUser!.userId!, authRoleIds);
                        message.success('授权成功');
                        setAuthRoleOpen(false);
                    }}>提交</PermissionButton>}>
                <Descriptions column={2} items={[{key: 'nick', label: '用户昵称', children: authUser?.nickName}, {
                    key: 'name',
                    label: '登录账号',
                    children: authUser?.userName
                }]}/>
                <ProTable<AuthRoleOption> rowKey="roleId" search={false} options={false} pagination={{pageSize: 10}}
                                          dataSource={authRoles} columns={[{title: '角色编号', dataIndex: 'roleId'}, {
                    title: '角色名称',
                    dataIndex: 'roleName'
                }, {title: '权限字符', dataIndex: 'roleKey'}, {title: '创建时间', dataIndex: 'createTime'}]}
                                          rowSelection={{
                                              selectedRowKeys: authRoleIds,
                                              getCheckboxProps: (role) => ({disabled: role.status !== '0'}),
                                              onChange: setAuthRoleIds
                                          }}/>
            </Drawer>
        </PageContainer>
    );
}
