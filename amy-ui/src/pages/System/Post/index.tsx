import { DeleteOutlined, DownloadOutlined, EditOutlined, PlusOutlined } from '@ant-design/icons';
import { ModalForm, PageContainer, ProFormDigit, ProFormRadio, ProFormText, ProFormTextArea, ProTable } from '@ant-design/pro-components';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { App, Tag } from 'antd';
import { useRef, useState } from 'react';
import { PermissionButton } from '@/components/PermissionButton';
import { useDict } from '@/hooks/useDict';
import { addPost, deletePosts, getPost, listPosts, updatePost } from '@/services/system/post';
import type { PostRecord } from '@/services/system/post';
import { downloadFile } from '@/utils/download';

export default function PostPage() {
  const { message, modal } = App.useApp(); const actionRef = useRef<ActionType>(); const [selected, setSelected] = useState<React.Key[]>([]); const [editing, setEditing] = useState<PostRecord>(); const [open, setOpen] = useState(false); const normalDict = useDict('sys_normal_disable');
  const openForm = async (record?: PostRecord) => { setEditing(record?.postId ? (await getPost(record.postId)).data : { postCode: '', postName: '', postSort: 0, status: '0' }); setOpen(true); };
  const columns: ProColumns<PostRecord>[] = [
    { title: '岗位编号', dataIndex: 'postId', search: false }, { title: '岗位编码', dataIndex: 'postCode' }, { title: '岗位名称', dataIndex: 'postName' }, { title: '岗位排序', dataIndex: 'postSort', search: false },
    { title: '状态', dataIndex: 'status', valueType: 'select', valueEnum: Object.fromEntries(normalDict.options.map((i) => [i.value, { text: i.label }])), render: (_, r) => <Tag color={r.status === '0' ? 'success' : 'error'}>{r.status === '0' ? '正常' : '停用'}</Tag> },
    { title: '创建时间', dataIndex: 'createTime', search: false }, { title: '操作', valueType: 'option', render: (_, r) => [<PermissionButton key="e" type="link" permission="system:post:edit" onClick={() => void openForm(r)}>修改</PermissionButton>, <PermissionButton key="d" type="link" danger permission="system:post:remove" onClick={() => modal.confirm({ title: `确认删除岗位「${r.postName}」？`, onOk: async () => { await deletePosts([r.postId!]); message.success('删除成功'); actionRef.current?.reload(); } })}>删除</PermissionButton>] }
  ];
  return <PageContainer><ProTable<PostRecord> rowKey="postId" actionRef={actionRef} columns={columns} rowSelection={{ selectedRowKeys: selected, onChange: setSelected }} request={async ({ current, pageSize, ...p }) => { const r = await listPosts({ ...p, pageNum: current, pageSize }); return { data: r.rows, total: r.total, success: r.code === 200 }; }} toolBarRender={() => [
    <PermissionButton key="a" type="primary" icon={<PlusOutlined />} permission="system:post:add" onClick={() => void openForm()}>新增</PermissionButton>, <PermissionButton key="e" icon={<EditOutlined />} permission="system:post:edit" disabled={selected.length !== 1} onClick={() => void openForm({ postId: Number(selected[0]), postCode: '', postName: '', postSort: 0 })}>修改</PermissionButton>, <PermissionButton key="d" danger icon={<DeleteOutlined />} permission="system:post:remove" disabled={!selected.length} onClick={() => modal.confirm({ title: `确认删除岗位编号「${selected.join(',')}」？`, onOk: async () => { await deletePosts(selected); setSelected([]); message.success('删除成功'); actionRef.current?.reload(); } })}>删除</PermissionButton>, <PermissionButton key="x" icon={<DownloadOutlined />} permission="system:post:export" onClick={() => void downloadFile('/system/post/export', {}, `post_${Date.now()}.xlsx`)}>导出</PermissionButton>]} />
    <ModalForm<PostRecord> title={editing?.postId ? '修改岗位' : '添加岗位'} open={open} initialValues={editing} modalProps={{ destroyOnClose: true, onCancel: () => setOpen(false) }} onFinish={async (v) => { const data = { ...editing, ...v }; editing?.postId ? await updatePost(data) : await addPost(data); message.success(editing?.postId ? '修改成功' : '新增成功'); setOpen(false); actionRef.current?.reload(); return true; }}><ProFormText name="postName" label="岗位名称" rules={[{ required: true }]} /><ProFormText name="postCode" label="岗位编码" rules={[{ required: true }]} /><ProFormDigit name="postSort" label="岗位顺序" min={0} rules={[{ required: true }]} /><ProFormRadio.Group name="status" label="岗位状态" options={normalDict.options} /><ProFormTextArea name="remark" label="备注" /></ModalForm>
  </PageContainer>;
}
