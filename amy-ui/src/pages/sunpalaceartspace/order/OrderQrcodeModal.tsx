import {DownloadOutlined, InfoCircleOutlined, QrcodeOutlined} from '@ant-design/icons';
import {App, Button, Col, Divider, Modal, QRCode, Row, Space, Typography, theme} from 'antd';
import {useRef} from 'react';
import type {ReactNode} from 'react';
import {getOrderSourceName} from '@/services/sunpalaceartspace/order';
import type {ReservationOrderRecord} from '@/services/sunpalaceartspace/order';
import styles from '../spas.less';

/**
 * 预约订单二维码弹窗。
 *
 * 二维码内容就是订单号本身（23 位数字），不含链接、不含 token。
 * 口径与小程序「扫码核销」页一致：uni.scanCode 的 res.result 直接当订单号，
 * 交给 GET /spas/mina/app/reservation/{orderNum} 查询详情后再确认核销。
 * 若后续改成「扫链接核销」，只需修改 buildQrcodeValue 一处。
 */
const buildQrcodeValue = (reservationNum: string) => reservationNum;

const STATUS_TEXT: Record<number, string> = {
    0: '待核销',
    1: '已完成',
    2: '已过期',
    3: '已取消'
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

    const num = order?.reservationNum || '';

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
        open={open && !!num}
        width={620}
        centered
        destroyOnHidden
        onCancel={onClose}
        footer={<Space>
            <Button onClick={onClose}>关闭</Button>
            <Button type="primary" icon={<DownloadOutlined/>} onClick={handleDownload}>下载二维码</Button>
        </Space>}
    >
        <Row gutter={24} align="middle" style={{padding: '4px 0 8px'}}>
            <Col flex="0 0 236px">
                <div ref={qrRef} style={{
                    display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 12,
                    padding: 18, borderRadius: 16, background: '#ffffff',
                    border: `1px solid ${token.colorBorderSecondary}`,
                    boxShadow: token.boxShadowTertiary
                }}>
                    <QRCode
                        value={buildQrcodeValue(num)}
                        size={196}
                        errorLevel="M"
                        bordered={false}
                        color="#1c2333"
                        bgColor="#ffffff"
                    />
                    <Typography.Text type="secondary" style={{fontSize: 12}}>
                        小程序「扫码核销」可直接使用
                    </Typography.Text>
                </div>
            </Col>
            <Col flex="auto">
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
                    {order?.reservationStatusName || STATUS_TEXT[order?.reservationStatus ?? -1] || '未知状态'}
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
            <span>二维码内容仅为订单号本身，不含任何链接或会员信息；下载的图片为白底 PNG，可直接打印。</span>
        </div>
    </Modal>;
}
