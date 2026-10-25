import {PageContainer, ProCard, ProForm, ProFormRadio, ProFormText, ProSkeleton} from '@ant-design/pro-components';
import type {ProFormInstance} from '@ant-design/pro-components';
import {App, Descriptions, Tabs, Typography} from 'antd';
import {useEffect, useRef, useState} from 'react';
import {useModel} from '@umijs/max';
import AvatarUpload from './AvatarUpload';
import {getUserProfile, updateUserPassword, updateUserProfile} from '@/services/system/user';
import type {UserRecord} from '@/services/system/user';
import type {CurrentUser} from '@/types/api';
import styles from './index.less';

interface PasswordForm {
    oldPassword: string;
    newPassword: string;
    confirmPassword: string;
}

const PAGE_HEADER = {
    title: false,
    breadcrumb: {items: [{title: '个人中心'}]}
};

/**
 * 个人中心：左「个人信息」+ 右「修改资料 / 修改密码」双栏布局，
 * 结构对齐 ruoyi-ui 的 profile/index，控件与表单按 Ant Design Pro 规范书写。
 */
export default function ProfilePage() {
    const {message} = App.useApp();
    const {initialState, setInitialState} = useModel('@@initialState');
    const [user, setUser] = useState<UserRecord>();
    const [roleGroup, setRoleGroup] = useState('');
    const [postGroup, setPostGroup] = useState('');
    const passwordFormRef = useRef<ProFormInstance<PasswordForm> | null>(null);

    const load = async () => {
        const response = await getUserProfile();
        setUser(response.data);
        setRoleGroup(response.roleGroup);
        setPostGroup(response.postGroup);
    };

    useEffect(() => {
        void load();
    }, []);

    /**
     * 把最新的用户信息同步到全局 initialState，
     * 否则右上角 AvatarDropdown 里的头像/昵称要等下次刷新页面才会变。
     */
    const syncInitialState = (patch: Partial<CurrentUser>) => {
        const currentUser = initialState?.currentUser;
        if (!currentUser) return;
        void setInitialState({...initialState, currentUser: {...currentUser, ...patch}});
    };

    // 头像上传走独立接口（/system/user/profile/avatar），这里只需要回填 URL
    const handleAvatarChange = (avatar: string) => {
        setUser((prev) => (prev ? {...prev, avatar} : prev));
        syncInitialState({avatar});
    };

    const handleProfileSubmit = async (values: Partial<UserRecord>) => {
        await updateUserProfile(values);
        message.success('资料修改成功');
        await load();
        syncInitialState({nickName: values.nickName});
        return true;
    };

    const handlePasswordSubmit = async (values: PasswordForm) => {
        await updateUserPassword(values.oldPassword, values.newPassword);
        message.success('密码修改成功，请牢记新密码');
        passwordFormRef.current?.resetFields();
        return true;
    };

    if (!user) {
        return <PageContainer header={PAGE_HEADER}><ProSkeleton type="descriptions"/></PageContainer>;
    }

    const infoItems = [
        {key: 'userName', label: '用户名称', children: user.userName},
        {key: 'phone', label: '手机号码', children: user.phonenumber || '-'},
        {key: 'email', label: '用户邮箱', children: user.email || '-'},
        {key: 'dept', label: '所属部门', children: `${user.dept?.deptName || '-'} / ${postGroup || '-'}`},
        {key: 'role', label: '所属角色', children: roleGroup || '-'},
        {key: 'createTime', label: '创建日期', children: user.createTime || '-'}
    ];

    const tabItems = [
        {
            key: 'info',
            label: '基本资料',
            children: (
                <ProForm<Partial<UserRecord>>
                    grid
                    rowProps={{gutter: [16, 0]}}
                    initialValues={user}
                    onFinish={handleProfileSubmit}
                    submitter={{searchConfig: {submitText: '保存'}}}
                >
                    <ProFormText
                        name="nickName"
                        label="用户昵称"
                        colProps={{md: 12, sm: 24}}
                        rules={[{required: true, message: '请输入用户昵称'}]}
                    />
                    <ProFormText
                        name="phonenumber"
                        label="手机号码"
                        colProps={{md: 12, sm: 24}}
                        rules={[
                            {required: true, message: '请输入手机号码'},
                            {pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码'}
                        ]}
                    />
                    <ProFormText
                        name="email"
                        label="邮箱"
                        colProps={{md: 12, sm: 24}}
                        rules={[
                            {required: true, message: '请输入邮箱'},
                            {type: 'email', message: '请输入正确的邮箱地址'}
                        ]}
                    />
                    <ProFormRadio.Group
                        name="sex"
                        label="性别"
                        options={[{label: '男', value: '0'}, {label: '女', value: '1'}]}
                    />
                </ProForm>
            )
        },
        {
            key: 'password',
            label: '修改密码',
            children: (
                <ProForm<PasswordForm>
                    formRef={passwordFormRef}
                    grid
                    rowProps={{gutter: [16, 0]}}
                    onFinish={handlePasswordSubmit}
                    submitter={{searchConfig: {submitText: '保存'}}}
                >
                    <ProFormText.Password
                        name="oldPassword"
                        label="旧密码"
                        colProps={{md: 12, sm: 24}}
                        rules={[{required: true, message: '请输入旧密码'}]}
                    />
                    <ProFormText.Password
                        name="newPassword"
                        label="新密码"
                        colProps={{md: 12, sm: 24}}
                        rules={[
                            {required: true, message: '请输入新密码'},
                            {min: 5, max: 20, message: '密码长度需在 5 到 20 个字符之间'}
                        ]}
                    />
                    <ProFormText.Password
                        name="confirmPassword"
                        label="确认密码"
                        colProps={{md: 12, sm: 24}}
                        dependencies={['newPassword']}
                        rules={[
                            {required: true, message: '请再次输入新密码'},
                            ({getFieldValue}) => ({
                                validator: (_, value) =>
                                    value === getFieldValue('newPassword')
                                        ? Promise.resolve()
                                        : Promise.reject(new Error('两次输入的密码不一致'))
                            })
                        ]}
                    />
                </ProForm>
            )
        }
    ];

    return (
        <PageContainer header={PAGE_HEADER}>
            <ProCard split="vertical">
                <ProCard title="个人信息" colSpan="30%" headerBordered>
                    <AvatarUpload value={user.avatar} onChange={handleAvatarChange}/>
                    <div className={styles.userMeta}>
                        <Typography.Text className={styles.userName}>
                            {user.nickName || user.userName}
                        </Typography.Text>
                        <Typography.Text type="secondary" className={styles.userAccount}>
                            {user.userName}
                        </Typography.Text>
                    </div>
                    <Descriptions
                        className={styles.profileDesc}
                        column={1}
                        size="small"
                        bordered
                        items={infoItems}
                    />
                </ProCard>
                <ProCard title="资料设置" headerBordered>
                    <Tabs items={tabItems}/>
                </ProCard>
            </ProCard>
        </PageContainer>
    );
}
