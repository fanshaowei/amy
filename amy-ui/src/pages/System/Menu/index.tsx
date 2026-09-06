import {DeleteOutlined, PlusOutlined, SaveOutlined} from '@ant-design/icons';
import {
    ModalForm,
    PageContainer,
    ProFormDependency,
    ProFormDigit,
    ProFormItem,
    ProFormRadio,
    ProFormText,
    ProFormTreeSelect,
    ProTable
} from '@ant-design/pro-components';
import type {ActionType, ProColumns} from '@ant-design/pro-components';
import {App, Col, InputNumber, Tag} from 'antd';
import {useRef, useState} from 'react';
import {PermissionButton} from '@/components/PermissionButton';
import IconSelect, {renderIcon} from '@/components/IconSelect';
import {useDict} from '@/hooks/useDict';
import {addMenu, deleteMenu, getMenu, getMenuTree, listMenus, updateMenu, updateMenuSort} from '@/services/system/menu';
import type {MenuRecord} from '@/services/system/menu';
import type {TreeOption} from '@/services/system/user';
import {buildTree} from '@/utils/tree';

const menuTypeMap = {
    M: {text: '目录', color: 'blue'},
    C: {text: '菜单', color: 'green'},
    F: {text: '按钮', color: 'orange'}
} as const;

function renderMenuType(record: MenuRecord) {
    if (record.menuType === 'M') {
        return record.isFrame === '0'
            ? <Tag color="red">外链</Tag>
            : <Tag color={menuTypeMap.M.color}>{menuTypeMap.M.text}</Tag>;
    }
    if (record.menuType === 'C') {
        return record.isFrame === '0'
            ? <Tag color="red">外链</Tag>
            : <Tag color={menuTypeMap.C.color}>{menuTypeMap.C.text}</Tag>;
    }
    if (record.menuType === 'F') {
        return <Tag color={menuTypeMap.F.color}>{menuTypeMap.F.text}</Tag>;
    }
    return <Tag>{record.menuType || '-'}</Tag>;
}

export default function MenuPage() {
    const {message, modal} = App.useApp();
    const actionRef = useRef<ActionType>();
    const [editing, setEditing] = useState<MenuRecord>();
    const [open, setOpen] = useState(false);
    const [treeOptions, setTreeOptions] = useState<TreeOption[]>([]);
    const [orders, setOrders] = useState<Record<number, number>>({});
    const visibleDict = useDict('sys_show_hide');
    const normalDict = useDict('sys_normal_disable');

    const openForm = async (record?: MenuRecord, parentId?: number) => {
        const treeResult = await getMenuTree();
        setTreeOptions([{id: 0, label: '主类目', children: treeResult.data || []}]);
        if (record?.menuId) setEditing((await getMenu(record.menuId)).data);
        else setEditing({
            parentId: parentId ?? 0,
            menuName: '',
            menuType: 'M',
            orderNum: 0,
            isFrame: '1',
            isCache: '0',
            visible: '0',
            status: '0',
            icon: '#'
        });
        setOpen(true);
    };

    const columns: ProColumns<MenuRecord>[] = [
        {title: '菜单名称', dataIndex: 'menuName', width: 220, render: (_, record) => {
            const IconComp = renderIcon(record.icon);
            return <span style={{display: 'inline-flex', alignItems: 'center', gap: 6}}>
                {IconComp ? <IconComp/> : null}
                <span>{record.menuName}</span>
            </span>;
        }},
        {
            title: '类型',
            dataIndex: 'menuType',
            search: false,
            width: 90,
            render: (_, record) => renderMenuType(record)
        },
        {
            title: '排序',
            dataIndex: 'orderNum',
            search: false,
            width: 120,
            render: (_, record) => <InputNumber min={0} value={orders[record.menuId!] ?? record.orderNum}
                                                onChange={(value) => setOrders((state) => ({
                                                    ...state,
                                                    [record.menuId!]: value ?? 0
                                                }))}/>
        },
        {title: '权限标识', dataIndex: 'perms', search: false},
        {title: '组件路径', dataIndex: 'component', search: false},
        {
            title: '状态',
            dataIndex: 'status',
            valueType: 'select',
            valueEnum: Object.fromEntries(normalDict.options.map((item) => [item.value, {text: item.label}])),
            width: 90,
            render: (_, record) => record.status === '0' ? <Tag color="success">正常</Tag> :
                <Tag color="error">停用</Tag>
        },
        {
            title: '操作', valueType: 'option', width: 210, render: (_, record) => [
                <PermissionButton key="edit" type="link" size="small" permission="system:menu:edit"
                                  onClick={() => void openForm(record)}>修改</PermissionButton>,
                <PermissionButton key="add" type="link" size="small" permission="system:menu:add"
                                  onClick={() => void openForm(undefined, record.menuId)}>新增</PermissionButton>,
                <PermissionButton key="delete" type="link" danger size="small" permission="system:menu:remove"
                                  onClick={() => modal.confirm({
                                      title: `确认删除菜单「${record.menuName}」？`,
                                      onOk: async () => {
                                          try {
                                              await deleteMenu(record.menuId!);
                                              message.success('删除成功');
                                              actionRef.current?.reload();
                                          } catch {
                                              // 全局响应拦截器已 toast 错误，这里只 swallow，避免 unhandled rejection
                                          }
                                      }
                                  })}>删除</PermissionButton>
            ]
        }
    ];

    return <PageContainer>
        <ProTable<MenuRecord>
            rowKey="menuId" actionRef={actionRef} columns={columns} pagination={false} defaultExpandAllRows
            request={async (params) => {
                const response = await listMenus(params);
                const data = buildTree(response.data || [], 'menuId', 'parentId');
                return {data, success: response.code === 200};
            }}
            toolBarRender={() => [
                <PermissionButton key="add" type="primary" icon={<PlusOutlined/>} permission="system:menu:add"
                                  onClick={() => void openForm()}>新增</PermissionButton>,
                <PermissionButton key="sort" icon={<SaveOutlined/>} permission="system:menu:edit"
                                  disabled={!Object.keys(orders).length} onClick={async () => {
                    const menuIds = Object.keys(orders);
                    await updateMenuSort({
                        menuIds: menuIds.join(','),
                        orderNums: menuIds.map((id) => orders[Number(id)]).join(',')
                    });
                    setOrders({});
                    message.success('排序保存成功');
                    actionRef.current?.reload();
                }}>保存排序</PermissionButton>
            ]}
        />
        <ModalForm<MenuRecord> title={editing?.menuId ? '修改菜单' : '添加菜单'} open={open} initialValues={editing}
                               modalProps={{destroyOnClose: true, width: 760, onCancel: () => setOpen(false)}} grid
                               onFinish={async (values) => {
                                   const data = {...editing, ...values};
                                   if (editing?.menuId) await updateMenu(data); else await addMenu(data);
                                   message.success(editing?.menuId ? '修改成功' : '新增成功');
                                   setOpen(false);
                                   actionRef.current?.reload();
                                   return true;
                               }}>
            <ProFormTreeSelect name="parentId" label="上级菜单" colProps={{span: 24}} rules={[{required: true}]}
                               fieldProps={{
                                   treeData: treeOptions,
                                   fieldNames: {label: 'label', value: 'id'},
                                   treeDefaultExpandAll: true
                               }}/>
            <ProFormRadio.Group name="menuType" label="菜单类型" colProps={{span: 24}}
                                options={[{label: '目录', value: 'M'}, {label: '菜单', value: 'C'}, {
                                    label: '按钮',
                                    value: 'F'
                                }]}/>
            <ProFormDigit name="orderNum" label="显示排序" colProps={{span: 12}} min={0} rules={[{required: true}]}/>
            <ProFormText name="menuName" label="菜单名称" colProps={{span: 12}} rules={[{required: true}]}/>
            <ProFormDependency name={['menuType']}>
                {({menuType}) => (<>
                    {menuType !== 'F' && (<>
                        <Col span={12}>
                            <ProFormItem name="icon" label="菜单图标">
                                <IconSelect/>
                            </ProFormItem>
                        </Col>
                        <ProFormRadio.Group name="isFrame" label="是否外链" colProps={{span: 12}}
                                            options={[{label: '是', value: '0'}, {label: '否', value: '1'}]}/>
                        <ProFormText name="path" label="路由地址" colProps={{span: 12}}/>
                    </>)}
                    {menuType === 'C' && (<>
                        <ProFormText name="routeName" label="路由名称" colProps={{span: 12}}/>
                        <ProFormText name="component" label="组件路径" colProps={{span: 12}}/>
                        <ProFormText name="query" label="路由参数" colProps={{span: 12}}/>
                        <ProFormRadio.Group name="isCache" label="是否缓存" colProps={{span: 12}}
                                            options={[{label: '缓存', value: '0'}, {label: '不缓存', value: '1'}]}/>
                    </>)}
                    {menuType !== 'M' && (<ProFormText name="perms" label="权限字符" colProps={{span: 12}}/>)}
                    {menuType !== 'F' && (<ProFormRadio.Group name="visible" label="显示状态" colProps={{span: 12}}
                                                              options={visibleDict.options}/>)}
                </>)}
            </ProFormDependency>
            <ProFormRadio.Group name="status" label="菜单状态" colProps={{span: 12}} options={normalDict.options}/>
        </ModalForm>
    </PageContainer>;
}
