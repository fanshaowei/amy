import {request} from '@umijs/max';
import type {RuoYiResponse, RuoYiTableResponse} from '@/types/api';

/** 预约会员（biz_spas_reservation_user） */
export interface ReservationUserRecord {
    reservationUserId?: number;
    /** 用户编号 */
    number?: string;
    /** 微信昵称 */
    nickname?: string;
    /** 姓名 */
    name?: string;
    /** 电话 */
    phone?: string;
    /** 用户头像 */
    headImgUrl?: string;
    /** openid */
    openId?: string;
    /** 身份证号 */
    idNum?: string;
    /** 状态（0=正常,1=停用,2=删除） */
    status?: number;
    /** 类型（1用户 2核销员） */
    type?: number;
    /** 是否实名认证 */
    isVerified?: boolean;
    /** 注册时间 */
    createTime?: string;
}

export const listUsers = (params: Record<string, unknown>) =>
    request<RuoYiTableResponse<ReservationUserRecord>>('/amyBizSunPalaceArtSpace/reservation/user/list', {params});
export const getUser = (reservationUserId: number) =>
    request<RuoYiResponse<ReservationUserRecord>>(`/amyBizSunPalaceArtSpace/reservation/user/${reservationUserId}`);
/** 切换身份（1用户 2核销员） */
export const changeUserType = (reservationUserId: number, type: number) =>
    request(`/amyBizSunPalaceArtSpace/reservation/user/changeType/${reservationUserId}/${type}`, {method: 'PUT'});
export const deleteUsers = (userIds: React.Key[]) =>
    request(`/amyBizSunPalaceArtSpace/reservation/user/${userIds.join(',')}`, {method: 'DELETE'});
