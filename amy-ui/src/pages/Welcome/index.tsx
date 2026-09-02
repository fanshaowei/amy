import {PageContainer, ProCard} from '@ant-design/pro-components';
import {Typography} from 'antd';

export default function WelcomePage() {
    return (
        <PageContainer>
            <ProCard>
                <Typography.Title level={2}>欢迎使用若依管理系统</Typography.Title>
                <Typography.Paragraph>amy-ui 已使用 Ant Design Pro 完成登录与应用基础框架重写。</Typography.Paragraph>
            </ProCard>
        </PageContainer>
    );
}
