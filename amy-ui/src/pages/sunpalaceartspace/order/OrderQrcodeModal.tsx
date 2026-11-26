import {DownloadOutlined, InfoCircleOutlined, QrcodeOutlined} from '@ant-design/icons';
import {App, Button, Col, Divider, Modal, QRCode, Row, Space, Spin, Typography, theme} from 'antd';
import {useEffect, useRef, useState} from 'react';
import type {ReactNode} from 'react';
import {getOrderSourceName, getOrderStatusName, getOrderVerifyCode, refreshOrderVerifyCode} from '@/services/sunpalaceartspace/order';
import type {ReservationOrderRecord} from '@/services/sunpalaceartspace/order';
import styles from '../spas.less';

/**
 * 预约订单二维码弹窗。
 *
 * 二维码内容与小程序 QrcodeCard.vue 保持同一格式：JSON 携带「预约单号 + 核销码」。
 * 核销端 scan/index.vue#parseQrContent 会拆出两个字段，
 * 交给 POST /spas/mina/app/reservation/verify/{orderNum}/{verifyCode} 确认核销。
 * 若只放裸核销码，扫码端会按「旧格式 = 内容即订单号」去查详情，反而查不到订单。
 * 若后续改成「扫链接核销」，只需修改 buildQrcodeValue 一处。
 *
 * 核销码生命周期（仅在本弹窗打开期间生效）：
 * 打开 / 切换订单 -> refreshOrderVerifyCode 取一枚新码 -> 按 verifyCodeEffectiveDuration 倒计时
 * -> 归零再次取新码；关闭弹窗（或组件卸载）立即清理定时器与在途请求，不再倒计时、不再请求。
 */
const buildQrcodeValue = (orderNum: string, verifyCode: string): string =>
    JSON.stringify({orderNum, verifyCode});

/** 手动添加订单（OrderSource.MANUAL）：核销码固定为预约单号，长期有效，不参与倒计时 */
const MANUAL_ORDER_SOURCE = 2;
/** 后端未返回 / 返回非法时长时的兜底分钟数（与原「1 分钟有效」文案一致） */
const DEFAULT_DURATION_MINUTES = 1;
/** 倒计时轮询间隔：按绝对 deadline 算剩余时间，切后台被节流后回到前台也能立刻校正 */
const TICK_MS = 250;

const pad2 = (n: number) => String(n).padStart(2, '0');

/** 剩余秒数 -> mm:ss；超过 1 小时显示 hh:mm:ss（手动订单 720 分钟场景） */
export const formatCountdown = (seconds: number): string => {
    const total = Math.max(0, Math.floor(seconds));
    const h = Math.floor(total / 3600);
    const m = Math.floor((total % 3600) / 60);
    const s = total % 60;
    return h > 0 ? `${pad2(h)}:${pad2(m)}:${pad2(s)}` : `${pad2(m)}:${pad2(s)}`;
};

/** 解析后端返回的核销码有效时长（分钟），非法值兜底 1 分钟 */
export const resolveDurationMinutes = (value?: number): number => {
    const n = Number(value);
    return Number.isFinite(n) && n > 0 ? Math.floor(n) : DEFAULT_DURATION_MINUTES;
};

interface Props {
    open: boolean;
    order?: ReservationOrderRecord | null;
    onClose: () => void;
}

/** 右侧信息行：定宽灰色 label + 内容 */
function InfoRow({label, children}: { label: string; children: ReactNode }) {
    const {token} = theme.useToken();
    return <div style={{display: 'flex', gap: 12, alignItems: 'flex-start', marginBottom: 12, fontSize: 13}}>
        <span style={{flex: '0 0 64px', color: token.colorTextSecondary}}>{label}</span>
        <span style={{flex: 1, color: token.colorText, wordBreak: 'break-all'}}>{children || '-'}</span>
    </div>;
}

export function OrderQrcodeModal({open, order, onClose}: Props) {
    const {message} = App.useApp();
    const {token} = theme.useToken();
    const qrRef = useRef<HTMLDivElement>(null);

    /** 当前实际用于生成二维码的核销码：打开时 / 倒计时归零时由刷新接口写入 */
    const [activeCode, setActiveCode] = useState('');
    /** 剩余有效秒数（0 = 未启动倒计时） */
    const [remaining, setRemaining] = useState(0);
    const [refreshing, setRefreshing] = useState(false);
    const [error, setError] = useState('');

    const num = order?.reservationNum || '';
    const listCode = getOrderVerifyCode(order);
    /** 与列表二维码入口判断保持一致：后端仅在「待核销」订单返回 verifyCode */
    const canOpen = open && !!num && !!listCode;
    const isManual = order?.orderSource === MANUAL_ORDER_SOURCE;
    const durationMinutes = resolveDurationMinutes(order?.verifyCodeEffectiveDuration);
    const qrValue = num && activeCode ? buildQrcodeValue(num, activeCode) : '';

    useEffect(() => {
        // 关闭时定时器由 cleanup 清理；这里同时复位状态，避免下次打开瞬间闪出上一枚码 / 上一轮倒计时
        if (!canOpen) {
            setActiveCode('');
            setRemaining(0);
            setRefreshing(false);
            setError('');
            return;
        }

        let disposed = false;
        let timer: ReturnType<typeof setInterval> | undefined;
        let inFlight = false;

        function clearTimer() {
            if (timer !== undefined) {
                clearInterval(timer);
                timer = undefined;
            }
        }

        /** 基于绝对 deadline 倒计时；归零时停表 -> 重新取码 -> 开启下一轮 */
        function startCountdown() {
            clearTimer();
            const deadline = Date.now() + durationMinutes * 60 * 1000;
            setRemaining(durationMinutes * 60);
            timer = setInterval(() => {
                if (disposed) return;
                const left = Math.max(0, Math.ceil((deadline - Date.now()) / 1000));
                setRemaining(left);
                if (left <= 0) {
                    clearTimer();
                    void fetchCode();
                }
            }, TICK_MS);
        }

        async function fetchCode() {
            if (disposed || inFlight) return;
            inFlight = true;
            setRefreshing(true);
            // 取码期间先把旧二维码撤下，避免用户扫到已过期 / 即将被覆盖的码
            setActiveCode('');
            try {
                const code = await refreshOrderVerifyCode(num);
                if (disposed) return;
                setActiveCode(code);
                setError('');
                startCountdown();
            } catch {
                // 非 200 已由全局响应拦截器提示，这里只收敛 UI 状态，不重复弹窗
                if (disposed) return;
                clearTimer();
                setRemaining(0);
                setActiveCode('');
                setError('核销码获取失败，请关闭弹窗后重试');
            } finally {
                inFlight = false;
                if (!disposed) setRefreshing(false);
            }
        }

        if (isManual) {
            // 手动添加订单：核销码就是预约单号且长期有效，不请求接口、不启动倒计时
            setActiveCode(num);
            setRemaining(0);
            setRefreshing(false);
            setError('');
        } else {
            void fetchCode();
        }

        // 只在弹窗打开期间运行：关闭 / 切换订单 / 组件卸载都会清掉定时器与在途请求
        return () => {
            disposed = true;
            clearTimer();
        };
    }, [canOpen, num, isManual, durationMinutes]);

    /** antd QRCode 默认 type='canvas'，可直接取 canvas 导出 PNG */
    const handleDownload = () => {
        const canvas = qrRef.current?.querySelector('canvas');
        if (!canvas) {
            message.error('二维码尚未生成，请稍后重试');
            return;
        }
        const link = document.createElement('a');
        link.download = `预约订单_${num}.png`;
        link.href = canvas.toDataURL('image/png');
        link.click();
        message.success('二维码已下载');
    };

    return <Modal
        title={<Space size={8}>
            <span style={{
                display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
                width: 22, height: 22, borderRadius: 6, fontSize: 13,
                background: token.colorPrimaryBg, color: token.colorPrimary
            }}><QrcodeOutlined/></span>
            <span style={{fontWeight: 600}}>订单二维码</span>
        </Space>}
        open={canOpen}
        width={620}
        centered
        destroyOnHidden
        onCancel={onClose}
        footer={<Space>
            <Button onClick={onClose}>关闭</Button>
            <Button type="primary" icon={<DownloadOutlined/>} disabled={!qrValue} onClick={handleDownload}>下载二维码</Button>
        </Space>}
    >
        <Row gutter={24} align="middle" wrap={false} style={{padding: '4px 0 8px'}}>
            <Col flex="0 0 236px">
                <div ref={qrRef} style={{
                    display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 12,
                    padding: 18, borderRadius: 16, background: '#ffffff',
                    border: `1px solid ${token.colorBorderSecondary}`,
                    boxShadow: token.boxShadowTertiary
                }}>
                    <div style={{width: 196, height: 196, display: 'flex', alignItems: 'center', justifyContent: 'center'}}>
                        {qrValue
                            ? <QRCode
                                value={qrValue}
                                size={196}
                                errorLevel="M"
                                bordered={false}
                                color="#1c2333"
                                bgColor="#ffffff"
                            />
                            : <Spin size="large"/>}
                    </div>
                    {/* 沿用原「小程序扫码核销」文案所在位置，左右分栏结构保持不变 */}
                    {error
                        ? <div style={{fontSize: 12, textAlign: 'center', color: token.colorError}}>{error}</div>
                        : isManual
                            ? <Typography.Text type="secondary" style={{fontSize: 12}}>
                                核销码即预约单号，长期有效
                            </Typography.Text>
                            : <div style={{textAlign: 'center'}}>
                                <div style={{fontSize: 11, color: token.colorTextSecondary}}>
                                    {refreshing ? '正在获取新核销码…' : '核销码剩余有效期'}
                                </div>
                                <div style={{
                                    fontSize: 22, fontWeight: 700, lineHeight: 1.35,
                                    fontVariantNumeric: 'tabular-nums',
                                    color: !refreshing && remaining <= 10 ? token.colorError : token.colorPrimary
                                }}>
                                    {refreshing ? '--:--' : formatCountdown(remaining)}
                                </div>
                                <div style={{fontSize: 11, color: token.colorTextSecondary}}>
                                    到期自动获取新核销码
                                </div>
                            </div>}
                </div>
            </Col>
            <Col flex="auto" style={{minWidth: 0}}>
                <div style={{color: token.colorTextSecondary, fontSize: 12, marginBottom: 6}}>订单号</div>
                <Typography.Text
                    className={styles.mono}
                    copyable={{text: num, tooltips: ['复制订单号', '已复制']}}
                    style={{fontSize: 15, fontWeight: 600, wordBreak: 'break-all'}}
                >
                    {num}
                </Typography.Text>
                <Divider style={{margin: '16px 0'}}/>
                <InfoRow label="预约项目">{order?.projectName}</InfoRow>
                <InfoRow label="预约人">{order?.name}</InfoRow>
                <InfoRow label="预约时间">{order?.reservationTime}</InfoRow>
                <InfoRow label="当前状态">
                    {getOrderStatusName(order?.reservationStatus, order?.reservationStatusName)}
                </InfoRow>
                <InfoRow label="订单来源">{getOrderSourceName(order?.orderSource, order?.orderSourceName)}</InfoRow>
            </Col>
        </Row>
        <div style={{
            display: 'flex', alignItems: 'flex-start', gap: 8,
            padding: '10px 12px', borderRadius: 10, fontSize: 12,
            background: token.colorFillQuaternary, color: token.colorTextSecondary
        }}>
            <InfoCircleOutlined style={{marginTop: 2, color: token.colorPrimary}}/>
            <span>二维码含预约单号与核销码，{isManual
                ? '手动添加订单的核销码即预约单号，长期有效'
                : `核销码 ${durationMinutes} 分钟有效，倒计时结束会自动获取新核销码`}；下载的图片为白底 PNG，可直接打印。</span>
        </div>
    </Modal>;
}
