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
            header={{title: false}}
            content="配置系统整体主题色、链接色、圆角、组件尺寸、导航模式与侧边菜单类型。保存后立即全局生效，并持久化到参数表 sys_config（config_key = sys.style.settings）。"
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
                                圆角 {preview.borderRadius}px · 组件尺寸 {preview.componentSize} · 导航 {preview.layout} · 侧栏 {preview.navTheme}
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
                        onValuesChange={(changed, all) => {
                            // 改主题色时链接色自动跟随，省去重复设置；
                            // 之后仍可在下方「链接色」里单独微调（手动改链接色不会触发这里）。
                            if (changed.colorPrimary) {
                                form.setFieldValue('colorLink', changed.colorPrimary);
                                setPreview({ ...(all as SystemStyle), colorLink: changed.colorPrimary });
                                return;
                            }
                            setPreview(all as SystemStyle);
                        }}
                    >
                        <ProFormItem
                            label="主题色"
                            name="colorPrimary"
                            tooltip="系统主色，作用于按钮、选中态、聚焦边框、进度条等。修改后链接色会自动跟随它"
                        >
                            <ColorField />
                        </ProFormItem>
                        <ProFormItem
                            label="链接色"
                            name="colorLink"
                            tooltip="默认随主题色自动同步，也可在此单独微调"
                        >
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
                        <ProFormRadio.Group
                            label="导航模式"
                            name="layout"
                            tooltip="侧边菜单 / 顶部菜单 / 混合菜单"
                            options={[
                                { label: '侧边菜单', value: 'side' },
                                { label: '顶部菜单', value: 'top' },
                                { label: '混合菜单', value: 'mix' },
                            ]}
                        />
                        <ProFormRadio.Group
                            label="侧边菜单类型"
                            name="navTheme"
                            tooltip="暗色侧栏 / 亮色侧栏"
                            options={[
                                { label: '暗色侧栏', value: 'dark' },
                                { label: '亮色侧栏', value: 'light' },
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
                                            // 预设色是程序化赋值，不会触发 onValuesChange，
                                            // 因此这里手动把链接色一并同步为主色。
                                            form.setFieldsValue({ colorPrimary: c, colorLink: c });
                                            setPreview((p) => ({ ...p, colorPrimary: c, colorLink: c }));
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
