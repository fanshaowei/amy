import {PageContainer, ProCard, ProForm, ProFormRadio, ProFormText} from '@ant-design/pro-components';
import {App, Descriptions, Spin, Tabs} from 'antd';
import {useEffect, useState} from 'react';
import {getUserProfile, updateUserPassword, updateUserProfile} from '@/services/system/user';
import type {UserRecord} from '@/services/system/user';

interface PasswordForm {
    oldPassword: string;
    newPassword: string;
    confirmPassword: string
}

export default function ProfilePage() {
    const {message} = App.useApp();
    const [user, setUser] = useState<UserRecord>();
    const [roleGroup, setRoleGroup] = useState('');
    const [postGroup, setPostGroup] = useState('');
    const load = async () => {
        const response = await getUserProfile();
        setUser(response.data);
        setRoleGroup(response.roleGroup);
        setPostGroup(response.postGroup);
    };
    useEffect(() => {
        void load();
    }, []);
    if (!user) return <PageContainer><Spin/></PageContainer>;
    return <PageContainer><ProCard split="vertical"><ProCard title="个人信息" colSpan="30%"><Descriptions column={1}
                                                                                                          items={[{
                                                                                                              key: 'name',
                                                                                                              label: '用户名称',
                                                                                                              children: user.userName
                                                                                                          }, {
                                                                                                              key: 'phone',
                                                                                                              label: '手机号码',
                                                                                                              children: user.phonenumber || '-'
                                                                                                          }, {
                                                                                                              key: 'email',
                                                                                                              label: '用户邮箱',
                                                                                                              children: user.email || '-'
                                                                                                          }, {
                                                                                                              key: 'dept',
                                                                                                              label: '所属部门',
                                                                                                              children: `${user.dept?.deptName || '-'} / ${postGroup || '-'}`
                                                                                                          }, {
                                                                                                              key: 'role',
                                                                                                              label: '所属角色',
                                                                                                              children: roleGroup || '-'
                                                                                                          }, {
                                                                                                              key: 'time',
                                                                                                              label: '创建日期',
                                                                                                              children: user.createTime || '-'
                                                                                                          }]}/></ProCard><ProCard
        title="基本资料"><Tabs items={[
        {
            key: 'info',
            label: '基本资料',
            children: <ProForm<Partial<UserRecord>> initialValues={user} onFinish={async (values) => {
                await updateUserProfile(values);
                message.success('修改成功');
                await load();
                return true;
            }}><ProFormText name="nickName" label="用户昵称" rules={[{required: true}]}/><ProFormText name="phonenumber"
                                                                                                      label="手机号码"
                                                                                                      rules={[{required: true}, {pattern: /^1[3-9]\d{9}$/}]}/><ProFormText
                name="email" label="邮箱" rules={[{required: true}, {type: 'email'}]}/><ProFormRadio.Group name="sex"
                                                                                                           label="性别"
                                                                                                           options={[{
                                                                                                               label: '男',
                                                                                                               value: '0'
                                                                                                           }, {
                                                                                                               label: '女',
                                                                                                               value: '1'
                                                                                                           }]}/></ProForm>
        },
        {
            key: 'password', label: '修改密码', children: <ProForm<PasswordForm> onFinish={async (values) => {
                await updateUserPassword(values.oldPassword, values.newPassword);
                message.success('修改成功');
                return true;
            }}><ProFormText.Password name="oldPassword" label="旧密码" rules={[{required: true}]}/><ProFormText.Password
                name="newPassword" label="新密码" rules={[{required: true}, {min: 5, max: 20}]}/><ProFormText.Password
                name="confirmPassword" label="确认密码" dependencies={['newPassword']}
                rules={[{required: true}, ({getFieldValue}) => ({validator: (_, value) => value === getFieldValue('newPassword') ? Promise.resolve() : Promise.reject(new Error('两次输入的密码不一致'))})]}/></ProForm>
        }
    ]}/></ProCard></ProCard></PageContainer>;
}
