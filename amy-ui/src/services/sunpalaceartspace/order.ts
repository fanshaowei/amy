import {request} from '@umijs/max';
import type {RuoYiResponse, RuoYiTableResponse} from '@/types/api';

/** 预约订单（biz_spas_reservation_order） */
export interface ReservationOrderRecord {
    reservationOrderId?: number;
    /** 预约单号 */
    reservationNum?: string;
    projectId?: number;
    /** 预约项目名称 */
    projectName?: string;
    reservationUserId?: number;
    /** 会员编号 */
    reservationUserNumber?: string;
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
    /** 状态码：0待核销 1已完成 2已过期 3已取消 */
    reservationStatus?: number;
    /** 状态中文名 */
    reservationStatusName?: string;
    verifyBy?: string;
    verifyTime?: string;
    createTime?: string;
    /** 随行人信息，每个元素为 UserIdentity JSON 字符串 */
    compUsers?: string[];
}

/** 随行人身份信息（UserIdentity JSON 反序列化） */
export interface CompUser {
    name?: string;
    /** 证件类型（0身份证 1护照 2港澳台证件 3军官证） */
    idType?: number;
    idNum?: string;
    gender?: number;
    phone?: string;
}

export const listOrders = (params: Record<string, unknown>) =>
    request<RuoYiTableResponse<ReservationOrderRecord>>('/amyBizSunPalaceArtSpace/reservation/order/list', {params});
export const getOrder = (reservationOrderId: number) =>
    request<RuoYiResponse<ReservationOrderRecord>>(`/amyBizSunPalaceArtSpace/reservation/order/${reservationOrderId}`);
export const deleteOrders = (orderIds: React.Key[]) =>
    request(`/amyBizSunPalaceArtSpace/reservation/order/${orderIds.join(',')}`, {method: 'DELETE'});
