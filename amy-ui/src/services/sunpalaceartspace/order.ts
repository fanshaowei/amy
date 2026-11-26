import {request} from '@umijs/max';
import type {RuoYiResponse, SpasTableResponse} from '@/types/api';

/** 预约订单（biz_spas_reservation_order） */
export interface ReservationOrderRecord {
    reservationOrderId?: number;
    /** 预约单号 */
    reservationNum?: string;
    projectId?: number;
    /** 预约项目名称 */
    projectName?: string;
    reservationUserId?: number;
    /** 会员姓名 */
    reservationUserName?: string;
    /** 预约人姓名 */
    name?: string;
    /** 联系电话 */
    phone?: string;
    /** 证件类型 */
    idType?: string;
    /** 证件号码 */
    idNum?: string;
    /** 预约人数（含本人） */
    guestsNum?: number;
    /** 预约时间（yyyy-MM-dd HH:mm:ss） */
    reservationTime?: string;
    /** 状态码：-1预约失败 0待核销 1已完成 2已过期 3已取消 4预约中（与后端 ReservationOrderStatus 一致） */
    reservationStatus?: number;
    /** 状态中文名 */
    reservationStatusName?: string;
    /** 订单来源码：1小程序预约 2后台手动添加 */
    orderSource?: number;
    /** 订单来源中文名（后端 OrderSource 枚举装配） */
    orderSourceName?: string;
    verifyBy?: string;
    verifyTime?: string;
    createTime?: string;
    /**
     * 核销码：后端 selectReservationOrderList 仅在「待核销」状态查询时生成并返回，
     * 已完成 / 已过期 / 已取消等订单为 null。前端据此决定是否展示订单二维码。
     * 小程序订单为 Redis 中的一次性码（1 分钟有效），手动添加订单则为订单号本身。
     */
    verifyCode?: string | null;
    /**
     * 核销码有效持续时长（分钟），来自后端 spas.verifyCodeEffectiveDuration。
     * 列表仅对「待核销」订单返回：小程序订单为配置值，手动添加订单固定 720。
     * 二维码弹窗据此倒计时，倒计时归零时重新取码。
     */
    verifyCodeEffectiveDuration?: number;
    /** 随行人信息，每个元素为 UserIdentity JSON 字符串 */
    compUsers?: string[];
}

/** 取核销码：null / 非字符串 / 纯空白统一归一为空串，供列表与弹窗共用同一判空口径 */
export const getOrderVerifyCode = (order?: Pick<ReservationOrderRecord, 'verifyCode'> | null): string =>
    typeof order?.verifyCode === 'string' ? order.verifyCode.trim() : '';

/** 订单来源码 -> 中文名（与后端 OrderSource 枚举保持一致） */
export const ORDER_SOURCE_TEXT: Record<number, string> = {
    1: '小程序预约',
    // 与后端 OrderSource.MANUAL 的 name 保持一致
    2: '手动添加'
};

/**
 * 取订单来源展示文案：按来源码取前端统一文案（与后端 OrderSource 枚举保持一致），
 * 后端新增了前端还不认识的来源码时才回退到 VO 的中文名，最后兜底「未知来源」。
 * 这样即使后端仍是旧编译产物、返回的 name 还是旧名，页面文案也能稳定统一。
 */
export const getOrderSourceName = (code?: number, name?: string): string =>
    (code != null ? ORDER_SOURCE_TEXT[code] : undefined) || name || '未知来源';

/** 订单状态码 -> 中文名（与后端 ReservationOrderStatus 枚举逐项对应） */
export const ORDER_STATUS_TEXT: Record<number, string> = {
    [-1]: '预约失败',
    0: '待核销',
    1: '已完成',
    2: '已过期',
    3: '已取消',
    4: '预约中'
};

/**
 * 取订单状态展示文案：优先用后端装配的中文名（VO#fillReservationStatusName），
 * 没装配时回退本地映射，最后兜底「未知状态」。新增状态只要后端枚举更新，
 * 即使前端未同步也能显示正确文案，不会再出现「非成功状态一律未知」。
 */
export const getOrderStatusName = (code?: number, name?: string): string =>
    name || (code != null ? ORDER_STATUS_TEXT[code] : undefined) || '未知状态';

/** 随行人身份信息（UserIdentity JSON 反序列化） */
export interface CompUser {
    name?: string;
    /** 证件类型（0身份证 1护照 2港澳台证件 3军官证） */
    idType?: number;
    idNum?: string;
    gender?: number;
    phone?: string;
}

/** 手动添加表单：可选项目及其时段 */
export interface ManualOrderProjectOption {
    projectId: number;
    projectName: string;
    /** 允许的最大随行人数（不含预约人本人） */
    travelerCount?: number;
    reservationStartTime?: string;
    reservationEndTime?: string;
    /** 已启用的可预约时段（HH:mm:ss）；为空表示项目未配置时段，可手动输入 */
    timeSlots: string[];
}

export interface ManualOrderOptions {
    projects: ManualOrderProjectOption[];
}

/** 随行人提交项（对应后端 FollowerReq：三个字段都必填） */
export interface ManualOrderFollower {
    name: string;
    idNum: string;
    phone: string;
}

/** 后台手动添加预约订单入参（对应 ReservationOrderManualReq） */
export interface ManualOrderPayload {
    projectId: number;
    name: string;
    phone: string;
    /** 证件类型中文名，与小程序端口径一致 */
    idType: string;
    idNum: string;
    /** yyyy-MM-dd HH:mm:ss */
    reservationTime: string;
    compUsers?: ManualOrderFollower[];
}

export const listOrders = (params: Record<string, unknown>) =>
    request<SpasTableResponse<ReservationOrderRecord>>('/spas/reservation/order/list', {params});
export const getOrder = (reservationOrderId: number) =>
    request<RuoYiResponse<ReservationOrderRecord>>(`/spas/reservation/order/${reservationOrderId}`);

/**
 * 管理后台获取（并刷新）核销码：GET /spas/reservation/order/verify-code/{reservationNum}
 *
 * 对应 ReservationOrderController#getOrderVerifyCode。后端每次调用都会生成一枚新码
 * 并重置 Redis 有效期（手动添加订单返回预约单号本身），因此「打开二维码弹窗」
 * 与「倒计时归零」都调它即可；倒计时时长取列表返回的 verifyCodeEffectiveDuration。
 * 注意：列表装配用的 getReservationVerifyCode 会复用未过期旧码，不能拿来做刷新。
 */
export const refreshOrderVerifyCode = async (reservationNum: string): Promise<string> => {
    // 带时间戳参数：GET 有写副作用（每次换发新码），避免任何一层缓存返回上一枚码
    const res = await request<RuoYiResponse<string>>(
        `/spas/reservation/order/verify-code/${encodeURIComponent(reservationNum)}`,
        {params: {_: Date.now()}}
    );
    // request 响应拦截器已对 code !== 200 统一提示并 reject，这里只需取出字符串
    const code = typeof res?.data === 'string' ? res.data.trim() : '';
    if (!code) {
        throw new Error('未获取到核销码');
    }
    return code;
};
export const deleteOrders = (orderIds: React.Key[]) =>
    request(`/spas/reservation/order/${orderIds.join(',')}`, {method: 'DELETE'});

/** 取消预约订单：后端仅允许「待核销」状态取消，并记录取消时间（不回补时段库存） */
export const cancelOrder = (reservationOrderId: number) =>
    request<RuoYiResponse<boolean>>(`/spas/reservation/order/cancel/${reservationOrderId}`, {method: 'PUT'});

/** 手动添加表单初始化数据（权限 sunpalaceartspace:reservationOrder:add） */
export const getManualOrderOptions = () =>
    request<RuoYiResponse<ManualOrderOptions>>('/spas/project/manual/generate/order/options');

/**
 * 手动添加预约订单：后端直接落「待核销」，不发 MQ、不扣减时段库存，
 * 也不绑定会员（reservation_user_id 为空）。
 */
export const addManualOrder = (data: ManualOrderPayload) =>
    request<RuoYiResponse<ReservationOrderRecord>>('/spas/reservation/order/manual/generate', {
        method: 'POST',
        data
    });
