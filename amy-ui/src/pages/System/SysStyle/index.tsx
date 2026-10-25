import {
    PageContainer,
    ProCard,
    ProForm,
    ProFormItem,
    ProFormRadio,
    ProFormSlider,
} from '@ant-design/pro-components';
import { App, Button, ColorPicker, ConfigProvider, Form, Input, Progress, Space, Tag } from 'antd';
import { useEffect, useMemo, useState } from 'react';
import {
    DEFAULT_SYSTEM_STYLE,
    useSystemTheme,
    type SystemStyle,
} from '@/theme/SystemStyle';
import styles from './index.less';

const PRESET_COLORS = [
    '#4f7d6d', '#1677ff', '#52c41a', '#fa541c',
    '#722ed1', '#eb2f96', '#13c2c2', '#faad14',
];

/** 把 antd ColorPicker 的 Color 对象收敛成 hex 字符串存入表单 */
const ColorField = ({ value, onChange }: { value?: string; onChange?: (v: string) => void }) => (
    <ColorPicker value={value} onChange={(c) => onChange?.(c.toHexString())} showText />
);

export default function SysStylePage() {
    const { message } = App.useApp();
    const { style, saveStyle } = useSystemTheme();
    const [form] = Form.useForm<SystemStyle>();
    const [preview, setPreview] = useState<SystemStyle>(style);

    // 全局样式变化（如刚登录后从服务端拉取）时，回填表单与预览
    useEffect(() => {
        form.setFieldsValue(style);
        setPreview(style);
    }, [style, form]);

    const handleFinish = async () => {
        const values = await form.validateFields();
        await saveStyle(values);
        message.success('系统样式已保存并全局生效');
        return true;
    };

    const handleReset = () => {
        form.setFieldsValue(DEFAULT_SYSTEM_STYLE);
        setPreview(DEFAULT_SYSTEM_STYLE);
    };

    const previewTheme = useMemo(
        () => ({
            token: {
                colorPrimary: preview.colorPrimary,
                colorLink: preview.colorLink,
                borderRadius: preview.borderRadius,
            },
        }),
        [preview]
    );

    return (
        <PageContainer
            title="系统样式设置"
            content="配置系统整体主题色、链接色、圆角与组件尺寸。保存后立即全局生效，并持久化到参数表 sys_config（config_key = sys.style.settings）。"
        >
            <ProCard split="vertical" wrap gutter={16}>
                <ProCard colSpan={{ xs: 24, md: 9 }} title="效果预览" headerBordered>
                    <ConfigProvider theme={previewTheme} componentSize={preview.componentSize}>
                        <Space direction="vertical" size="middle" style={{ width: '100%' }}>
                            <Space wrap>
                                <Button type="primary">主要按钮</Button>
                                <Button>默认按钮</Button>
                                <Button danger>危险按钮</Button>
                            </Space>
                            <Input placeholder="输入框示例" />
                            <Progress percent={60} />
                            <Space wrap>
                                <Tag color="processing">进行中</Tag>
                                <Tag color="success">成功</Tag>
                                <Tag>普通</Tag>
                            </Space>
                            <div className={styles.swatchRow}>
                                <span className={styles.swatch} style={{ background: preview.colorPrimary }} />
                                <span>主题色：{preview.colorPrimary}</span>
                            </div>
                            <div className={styles.metaRow}>
                                圆角 {preview.borderRadius}px · 组件尺寸 {preview.componentSize}
                            </div>
                        </Space>
                    </ConfigProvider>
                </ProCard>

                <ProCard colSpan={{ xs: 24, md: 15 }} title="样式配置" headerBordered>
                    <ProForm<SystemStyle>
                        form={form}
                        layout="vertical"
                        submitter={{
                            render: ({ submit }) => (
                                <Space>
                                    <Button type="primary" onClick={submit}>
                                        保存配置
                                    </Button>
                                    <Button onClick={handleReset}>恢复默认</Button>
                                </Space>
                            ),
                        }}
                        onFinish={handleFinish}
                        onValuesChange={(_, all) => setPreview(all as SystemStyle)}
                    >
                        <ProFormItem
                            label="主题色"
                            name="colorPrimary"
                            tooltip="系统主色，作用于按钮、选中态、聚焦边框、进度条等"
                        >
                            <ColorField />
                        </ProFormItem>
                        <ProFormItem label="链接色" name="colorLink" tooltip="文字链接的颜色">
                            <ColorField />
                        </ProFormItem>
                        <ProFormSlider
                            label="全局圆角"
                            name="borderRadius"
                            min={0}
                            max={16}
                            step={1}
                            marks={{ 0: '0', 8: '8', 16: '16' }}
                        />
                        <ProFormRadio.Group
                            label="组件尺寸"
                            name="componentSize"
                            options={[
                                { label: '紧凑', value: 'small' },
                                { label: '默认', value: 'middle' },
                                { label: '宽松', value: 'large' },
                            ]}
                        />
                        <ProFormItem label="预设主题色" tooltip="点击快速套用，可在上方继续微调">
                            <Space wrap>
                                {PRESET_COLORS.map((c) => (
                                    <button
                                        key={c}
                                        type="button"
                                        className={styles.preset}
                                        style={{ background: c }}
                                        aria-label={c}
                                        onClick={() => {
                                            form.setFieldValue('colorPrimary', c);
                                            setPreview((p) => ({ ...p, colorPrimary: c }));
                                        }}
                                    />
                                ))}
                            </Space>
                        </ProFormItem>
                    </ProForm>
                </ProCard>
            </ProCard>
        </PageContainer>
    );
}
