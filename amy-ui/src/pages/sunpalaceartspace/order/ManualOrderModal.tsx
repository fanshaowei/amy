import {
    CalendarOutlined,
    DeleteOutlined,
    IdcardOutlined,
    InfoCircleOutlined,
    LockOutlined,
    PlusOutlined,
    TeamOutlined
} from '@ant-design/icons';
import {
    ModalForm,
    ProCard,
    ProFormDatePicker,
    ProFormSelect,
    ProFormText,
    ProFormTimePicker
} from '@ant-design/pro-components';
import {App, Button, Col, Form, Input, Row, Space, Tag, Typography, theme} from 'antd';
import dayjs from 'dayjs';
import {useCallback, useEffect, useState} from 'react';
import type {ReactNode} from 'react';
import {addManualOrder, getManualOrderOptions} from '@/services/sunpalaceartspace/order';
import type {ManualOrderProjectOption} from '@/services/sunpalaceartspace/order';

/**
 * 后台「手动添加预约订单」。
 *
 * 与小程序下单的区别（后端口径）：直接落「待核销」，不发 MQ、不扣减 Redis 时段库存，
 * 也不绑定会员；名额需由操作员自行确认。
 *
 * 布局与「项目管理」表单保持一致：ProCard 分区 + Row/Col 栅格 + 主题 token 取色，
 * 不用 Space 横排表单控件（会导致宽度不均、label 与输入框错位）。
 */

/** 证件类型中文名，与后端 IdType 枚举及小程序端 cardTypeToIdType 一致 */
const ID_TYPE_OPTIONS = [
    {label: '身份证', value: '身份证'},
    {label: '护照', value: '护照'},
    {label: '港澳通行证', value: '港澳通行证'},
    {label: '台湾通行证', value: '台湾通行证'},
    {label: '军官证', value: '军官证'}
];

const PHONE_PATTERN = /^1[3-9]\d{9}$/;
const ID_CARD_PATTERN = /^[1-9]\d{5}(18|19|20)\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\d|3[01])\d{3}[\dXx]$/;

interface ManualOrderFormValues {
    projectId?: number;
    reservationDate?: string | dayjs.Dayjs;
    reservationTime?: string | dayjs.Dayjs;
    name?: string;
    phone?: string;
    idType?: string;
    idNum?: string;
    compUsers?: Array<{name?: string; idNum?: string; phone?: string}>;
}

interface Props {
    open: boolean;
    onClose: () => void;
    /** 添加成功：刷新列表与顶部统计 */
    onSuccess: () => void;
}

/** ProForm 日期控件可能给 dayjs 对象、日期串或 ISO 串，这里统一取 yyyy-MM-dd */
const toDateStr = (value: unknown): string => {
    if (!value) return '';
    if (dayjs.isDayjs(value)) return value.format('YYYY-MM-DD');
    return String(value).slice(0, 10);
};

/** ProForm 时间控件可能给 dayjs 对象、HH:mm:ss 或带日期的完整串，这里统一取 HH:mm:ss */
const toTimeStr = (value: unknown): string => {
    if (!value) return '';
    if (dayjs.isDayjs(value)) return value.format('HH:mm:ss');
    const text = String(value).trim();
    const full = /(\d{2}:\d{2}:\d{2})/.exec(text);
    if (full?.[1]) return full[1];
    const short = /(\d{2}:\d{2})/.exec(text);
    return short?.[1] ? `${short[1]}:00` : text;
};

/** 分区标题：主色徽标 + 标题 + 右侧灰色说明，颜色全部取自当前主题 */
function SectionHead({icon, title, hint}: { icon: ReactNode; title: string; hint?: string }) {
    const {token} = theme.useToken();
    return <Space size={8}>
        <span style={{
            display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
            width: 22, height: 22, borderRadius: 6, fontSize: 13,
            background: token.colorPrimaryBg, color: token.colorPrimary
        }}>{icon}</span>
        <span style={{fontWeight: 600, fontSize: 14}}>{title}</span>
        {hint ? <Typography.Text type="secondary" style={{fontSize: 12, fontWeight: 400}}>{hint}</Typography.Text> : null}
    </Space>;
}

/** 顶部业务口径提示条：跟随主题主色，不再硬编码蓝色 */
function NoticeBar({children}: { children: ReactNode }) {
    const {token} = theme.useToken();
    return <div style={{
        marginBottom: 16, padding: '10px 14px', borderRadius: 8,
        background: token.colorPrimaryBg,
        border: `1px solid ${token.colorPrimaryBorder}`,
        color: token.colorText,
        fontSize: 12, lineHeight: 1.7,
        display: 'flex', gap: 8, alignItems: 'flex-start'
    }}>
        <InfoCircleOutlined style={{marginTop: 3, color: token.colorPrimary, fontSize: 14}}/>
        <span>{children}</span>
    </div>;
}

/** 分区底部的浅色说明条 */
function SectionTip({children}: { children: ReactNode }) {
    const {token} = theme.useToken();
    return <div style={{
        marginTop: 4, padding: '7px 12px', borderRadius: 6,
        background: token.colorFillQuaternary,
        color: token.colorTextSecondary,
        fontSize: 12, lineHeight: 1.7
    }}>{children}</div>;
}

/** 随行人列表的列宽：表头与数据行共用，保证严格对齐 */
const FOLLOWER_COLS = {index: 2, name: 5, idNum: 9, phone: 6, action: 2} as const;

export function ManualOrderModal({open, onClose, onSuccess}: Props) {
    const {message} = App.useApp();
    const {token} = theme.useToken();
    const [form] = Form.useForm<ManualOrderFormValues>();
    const [projects, setProjects] = useState<ManualOrderProjectOption[]>([]);
    const [loading, setLoading] = useState(false);

    const loadOptions = useCallback(async () => {
        setLoading(true);
        try {
            const result = await getManualOrderOptions();
            if (result.code === 200 || result.code === 0) {
                setProjects(result.data?.projects ?? []);
            } else {
                message.error(result.msg || '获取项目列表失败');
            }
        } catch {
            message.error('获取项目列表失败');
        } finally {
            setLoading(false);
        }
    }, [message]);

    useEffect(() => {
        if (!open) return;
        form.resetFields();
        void loadOptions();
    }, [open, form, loadOptions]);

    const projectId = Form.useWatch('projectId', form);
    const selected = projects.find(item => item.projectId === Number(projectId));
    /**
     * 只有真正匹配到项目（而不是单纯有 projectId 值）才解锁其余字段：
     * 项目列表加载失败 / 项目被停用移出选项时，填了也提交不出有效订单。
     */
    const projectChosen = !!selected;
    /** 除「预约项目」外的所有控件共用此开关 */
    const locked = !projectChosen;
    const timeSlots = selected?.timeSlots ?? [];
    /** 项目未配置 travelerCount 时不限制，避免后台补录被 0 卡住 */
    const maxFollowers = selected?.travelerCount ?? undefined;
    const openRange = selected?.reservationStartTime && selected?.reservationEndTime
        ? `${selected.reservationStartTime.slice(0, 5)} ~ ${selected.reservationEndTime.slice(0, 5)}`
        : '';

    return <ModalForm<ManualOrderFormValues>
        title={<Space size={8}>
            <span style={{
                display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
                width: 24, height: 24, borderRadius: 7, fontSize: 14,
                background: token.colorPrimaryBg, color: token.colorPrimary
            }}><IdcardOutlined/></span>
            <span style={{fontWeight: 600}}>手动添加预约订单</span>
            <Typography.Text type="secondary" style={{fontSize: 12, fontWeight: 400}}>线下 / 电话补录</Typography.Text>
        </Space>}
        open={open}
        form={form}
        layout="vertical"
        labelAlign="left"
        width={880}
        initialValues={{idType: '身份证'}}
        submitter={{
            searchConfig: {submitText: '确认添加', resetText: '重置'},
            submitButtonProps: {disabled: locked}
        }}
        modalProps={{
            destroyOnHidden: true,
            onCancel: onClose,
            styles: {body: {maxHeight: '70vh', overflowY: 'auto', paddingTop: 16}}
        }}
        onOpenChange={visible => {
            if (!visible) onClose();
        }}
        onFinish={async values => {
            // 兜底：即使绕过按钮禁用（如字段残留值），也不允许无项目提交
            if (!selected) {
                message.warning('请先选择预约项目');
                return false;
            }
            const date = toDateStr(values.reservationDate);
            const time = toTimeStr(values.reservationTime);
            if (!date || !time) {
                message.error('请选择预约日期和时段');
                return false;
            }
            try {
                const result = await addManualOrder({
                    projectId: Number(values.projectId),
                    name: String(values.name || '').trim(),
                    phone: String(values.phone || '').trim(),
                    idType: String(values.idType || ''),
                    idNum: String(values.idNum || '').trim(),
                    reservationTime: `${date} ${time}`,
                    compUsers: (values.compUsers ?? []).map(item => ({
                        name: String(item.name || '').trim(),
                        idNum: String(item.idNum || '').trim(),
                        phone: String(item.phone || '').trim()
                    }))
                });
                if (result.code === 200 || result.code === 0) {
                    message.success(`添加成功，订单号 ${result.data?.reservationNum ?? ''}`);
                    onSuccess();
                    return true;
                }
                message.error(result.msg || '添加失败');
                return false;
            } catch {
                // 非 2xx 由全局 errorHandler 统一提示，这里只保证弹窗不关闭
                return false;
            }
        }}
    >
        <NoticeBar>
            手动添加的订单直接为「待核销」，<b>不会扣减时段库存</b>，也不绑定会员；请自行确认该时段名额后再提交。
        </NoticeBar>

        {locked
            ? <div style={{marginBottom: 16}}>
                <SectionTip>
                    <Space size={6}><LockOutlined/>请先选择「预约项目」，选定后才能填写预约日期、时段、预约人及随行人员信息。</Space>
                </SectionTip>
            </div>
            : null}

        {/* ===== 分区一：预约信息 ===== */}
        <ProCard bordered size="small" headerBordered
                 style={{marginBottom: 16}}
                 bodyStyle={{padding: '16px 16px 4px'}}
                 title={<SectionHead icon={<CalendarOutlined/>} title="预约信息"
                                    hint={locked ? '第一步：请先选择预约项目' : '选择项目后自动带出可预约时段'}/>}>
            <Row gutter={16}>
                <Col span={24}>
                    <ProFormSelect
                        name="projectId" label="预约项目" placeholder="请选择预约项目"
                        options={projects.map(item => ({label: item.projectName, value: item.projectId}))}
                        fieldProps={{
                            style: {width: '100%'}, loading, showSearch: true,
                            optionFilterProp: 'label',
                            allowClear: true,
                            onChange: value => {
                                // 换项目后原时段不再适用
                                form.setFieldValue('reservationTime', undefined);
                                // 主动清空项目时连带清掉其余已填值，避免「禁用但值还在」被提交
                                if (value == null) {
                                    form.setFieldsValue({
                                        reservationDate: undefined,
                                        name: undefined,
                                        phone: undefined,
                                        idType: '身份证',
                                        idNum: undefined,
                                        compUsers: []
                                    });
                                }
                            }
                        }}
                        rules={[{required: true, message: '请选择预约项目'}]}
                    />
                </Col>
                <Col span={12}>
                    <ProFormDatePicker
                        name="reservationDate" label="预约日期" placeholder="请选择日期"
                        fieldProps={{style: {width: '100%'}, disabled: locked}}
                        rules={[{required: true, message: '请选择预约日期'}]}
                    />
                </Col>
                <Col span={12}>
                    {timeSlots.length
                        ? <ProFormSelect
                            name="reservationTime" label="预约时段" placeholder="请选择时段"
                            options={timeSlots.map(slot => ({label: slot.slice(0, 5), value: slot}))}
                            fieldProps={{style: {width: '100%'}, disabled: locked}}
                            rules={[{required: true, message: '请选择预约时段'}]}
                        />
                        : <ProFormTimePicker
                            name="reservationTime" label="预约时间" placeholder="请选择时间"
                            fieldProps={{format: 'HH:mm:ss', style: {width: '100%'}, disabled: locked}}
                            rules={[{required: true, message: '请选择预约时间'}]}
                        />}
                </Col>
            </Row>
            <SectionTip>
                {!loading && !projects.length
                    ? <>暂无可预约项目，请先在「项目管理」中添加并启用项目。</>
                    : selected
                        ? <>「{selected.projectName}」{openRange ? `开放 ${openRange}，` : ''}
                            {timeSlots.length
                                ? <>共 <b>{timeSlots.length}</b> 个可预约时段。</>
                                : <>未配置可预约时段，可手动填写时间。</>}</>
                        : <>选择项目后，这里会显示该项目的开放时段与可选时段数量。</>}
            </SectionTip>
        </ProCard>

        {/* ===== 分区二：预约人信息 ===== */}
        <ProCard bordered size="small" headerBordered
                 style={{marginBottom: 16}}
                 bodyStyle={{padding: '16px 16px 4px'}}
                 title={<SectionHead icon={<IdcardOutlined/>} title="预约人信息"
                                    hint={locked ? '请先选择预约项目' : '用于到馆核销时核验身份'}/>}
                 extra={<Tag bordered={false} color="default" style={{margin: 0, fontSize: 11}}>本人</Tag>}>
            <Row gutter={16}>
                <Col span={12}>
                    <ProFormText name="name" label="姓名" placeholder="请输入预约人姓名"
                                 fieldProps={{style: {width: '100%'}, maxLength: 50, disabled: locked}}
                                 rules={[{required: true, message: '请输入姓名'}]}/>
                </Col>
                <Col span={12}>
                    <ProFormText name="phone" label="联系电话" placeholder="请输入 11 位手机号"
                                 fieldProps={{style: {width: '100%'}, maxLength: 11, disabled: locked}}
                                 rules={[
                                     {required: true, message: '请输入联系电话'},
                                     {pattern: PHONE_PATTERN, message: '请输入正确的手机号'}
                                 ]}/>
                </Col>
                <Col span={12}>
                    <ProFormSelect name="idType" label="证件类型" options={ID_TYPE_OPTIONS}
                                   fieldProps={{style: {width: '100%'}, disabled: locked}}
                                   rules={[{required: true, message: '请选择证件类型'}]}/>
                </Col>
                <Col span={12}>
                    <ProFormText name="idNum" label="证件号码" placeholder="请输入证件号码"
                                 fieldProps={{style: {width: '100%'}, maxLength: 50, disabled: locked}}
                                 rules={[
                                     {required: true, message: '请输入证件号码'},
                                     {
                                         validator: (_, value) => {
                                             if (!value) return Promise.resolve();
                                             if (form.getFieldValue('idType') === '身份证'
                                                 && !ID_CARD_PATTERN.test(String(value))) {
                                                 return Promise.reject(new Error('身份证号格式不正确（需 18 位）'));
                                             }
                                             return Promise.resolve();
                                         }
                                     }
                                 ]}/>
                </Col>
            </Row>
            <SectionTip>证件号码会按所选证件类型做格式校验，身份证需为 18 位且含正确校验位。</SectionTip>
        </ProCard>

        {/* ===== 分区三：随行人员 ===== */}
        <ProCard bordered size="small" headerBordered
                 bodyStyle={{padding: '16px 16px 16px'}}
                 title={<SectionHead icon={<TeamOutlined/>} title="随行人员"
                                    hint={locked ? '请先选择预约项目' : '选填，不含预约人本人'}/>}
                 extra={maxFollowers !== undefined
                     ? <Typography.Text type="secondary" style={{fontSize: 12}}>最多 {maxFollowers} 人</Typography.Text>
                     : undefined}>
            <Form.List name="compUsers">
                {(fields, {add, remove}) => <>
                    {/* 表头与数据行用同一套 Col span，保证列对齐 */}
                    <Row gutter={12} style={{padding: '0 12px 6px'}}>
                        <Col span={FOLLOWER_COLS.index}/>
                        <Col span={FOLLOWER_COLS.name}><Typography.Text type="secondary" style={{fontSize: 12}}>姓名</Typography.Text></Col>
                        <Col span={FOLLOWER_COLS.idNum}><Typography.Text type="secondary" style={{fontSize: 12}}>证件号码</Typography.Text></Col>
                        <Col span={FOLLOWER_COLS.phone}><Typography.Text type="secondary" style={{fontSize: 12}}>联系电话</Typography.Text></Col>
                        <Col span={FOLLOWER_COLS.action}/>
                    </Row>

                    {fields.length === 0
                        ? <div style={{
                            padding: '14px 12px', marginBottom: 12, borderRadius: 8,
                            background: token.colorFillQuaternary,
                            color: token.colorTextSecondary, fontSize: 12, textAlign: 'center'
                        }}>{locked ? '请先选择预约项目，选定后即可添加随行人员' : '暂无随行人员，如需添加请点击下方按钮'}</div>
                        : null}

                    {fields.map((field, index) => <Row key={field.key} gutter={12} align="top"
                                                       style={{
                                                           marginBottom: 8, padding: '10px 12px',
                                                           borderRadius: 8,
                                                           background: token.colorFillQuaternary,
                                                           border: `1px solid ${token.colorBorderSecondary}`
                                                       }}>
                        <Col span={FOLLOWER_COLS.index}>
                            <span style={{
                                display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
                                width: 22, height: 22, marginTop: 4, borderRadius: '50%',
                                background: token.colorPrimaryBg, color: token.colorPrimary,
                                fontSize: 11, fontWeight: 600
                            }}>{index + 1}</span>
                        </Col>
                        <Col span={FOLLOWER_COLS.name}>
                            <Form.Item {...field} name={[field.name, 'name']}
                                       rules={[{required: true, message: '请输入姓名'}]}
                                       style={{marginBottom: 0}}>
                                <Input placeholder="姓名" maxLength={50} disabled={locked}/>
                            </Form.Item>
                        </Col>
                        <Col span={FOLLOWER_COLS.idNum}>
                            <Form.Item {...field} name={[field.name, 'idNum']}
                                       rules={[{required: true, message: '请输入证件号码'}]}
                                       style={{marginBottom: 0}}>
                                <Input placeholder="证件号码" maxLength={50} disabled={locked}/>
                            </Form.Item>
                        </Col>
                        <Col span={FOLLOWER_COLS.phone}>
                            <Form.Item {...field} name={[field.name, 'phone']}
                                       rules={[
                                           {required: true, message: '请输入联系电话'},
                                           {pattern: PHONE_PATTERN, message: '手机号格式不正确'}
                                       ]}
                                       style={{marginBottom: 0}}>
                                <Input placeholder="联系电话" maxLength={11} disabled={locked}/>
                            </Form.Item>
                        </Col>
                        <Col span={FOLLOWER_COLS.action} style={{textAlign: 'right'}}>
                            <Button type="text" danger icon={<DeleteOutlined/>} title="移除该随行人"
                                    style={{marginTop: 2}} disabled={locked}
                                    onClick={() => remove(field.name)}/>
                        </Col>
                    </Row>)}

                    <Button block type="dashed" icon={locked ? <LockOutlined/> : <PlusOutlined/>}
                            disabled={locked || (maxFollowers !== undefined && fields.length >= maxFollowers)}
                            onClick={() => add()}>
                        {locked
                            ? '请先选择预约项目'
                            : `添加随行人员${fields.length ? `（已添加 ${fields.length} 人）` : ''}`}
                    </Button>
                    {maxFollowers === 0
                        ? <SectionTip>该项目未开放随行人员，仅可为预约人本人添加订单。</SectionTip>
                        : null}
                </>}
            </Form.List>
        </ProCard>
    </ModalForm>;
}

export default ManualOrderModal;
