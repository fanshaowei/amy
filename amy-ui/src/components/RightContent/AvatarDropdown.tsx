import {LogoutOutlined, UserOutlined} from '@ant-design/icons';
import {App, Avatar} from 'antd';
import React from 'react';
import {history} from '@umijs/max';
import HeaderDropdown from '@/components/HeaderDropdown';
import type {CurrentUser} from '@/types/api';
import {clearSession} from '@/utils/auth';
import {logout} from '@/services/auth';
import styles from './index.less';

/**
 * 右上角用户入口，对齐 Ant Design Pro 官方 AvatarDropdown：
 * - 触发区自带 header 高度居中、左右内边距、hover 背景反馈；
 * - 下拉弹层 placement=bottomRight，贴齐右上角（默认 bottomLeft 会偏移溢出）；
 * - 窄屏（≤768px）隐藏用户名只留头像。
 */
const AvatarDropdown: React.FC<{ currentUser?: CurrentUser }> = ({currentUser}) => {
    const {message} = App.useApp();

    const onMenuClick = async ({key}: { key: string }) => {
        if (key === 'profile') {
            history.push('/system/user/profile');
            return;
        }
        if (key === 'logout') {
            try {
                await logout();
            } finally {
                clearSession();
                message.success('已退出登录');
                history.push('/login');
            }
        }
    };

    const items = [
        {key: 'profile', icon: <UserOutlined/>, label: '个人中心'},
        {type: 'divider' as const},
        {key: 'logout', icon: <LogoutOutlined/>, label: '退出登录'}
    ];

    return (
        <HeaderDropdown placement="bottomRight" menu={{items, onClick: onMenuClick}}>
            <span className={styles.action}>
                <Avatar
                    size="small"
                    src={currentUser?.avatar}
                    icon={<UserOutlined/>}
                    alt="avatar"
                />
                <span className={styles.name}>{currentUser?.nickName || currentUser?.userName}</span>
            </span>
        </HeaderDropdown>
    );
};

export default AvatarDropdown;
