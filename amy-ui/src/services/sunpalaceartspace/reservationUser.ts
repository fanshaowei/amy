import {request} from '@umijs/max';
import type {RuoYiResponse, SpasTableResponse} from '@/types/api';

/** 预约会员（biz_spas_reservation_user） */
export interface ReservationUserRecord {
    reservationUserId?: number;
    /** 微信昵称 */
    nickname?: string;
    /** 姓名 */
    name?: string;
    /** 电话 */
    phone?: string;
    /** 用户头像 */
    avatarImgUrl?: string;
    /** openid */
    openId?: string;
    /** 身份证号 */
    idNum?: string;
    /** 状态（0=停用,1=正常,-1=删除） */
    status?: number;
    /** 类型（1用户 2核销员） */
    type?: number;
    /** 是否实名认证 */
    isVerified?: boolean;
    /** 注册时间 */
    createTime?: string;
}

export const listUsers = (params: Record<string, unknown>) =>
    request<SpasTableResponse<ReservationUserRecord>>('/spas/reservation/user/list', {params});
export const getUser = (reservationUserId: number) =>
    request<RuoYiResponse<ReservationUserRecord>>(`/spas/reservation/user/${reservationUserId}`);
/** 切换身份（1用户 2核销员） */
export const changeUserType = (reservationUserId: number, type: number) =>
    request(`/spas/reservation/user/changeType/${reservationUserId}/${type}`, {method: 'PUT'});
export const deleteUsers = (userIds: React.Key[]) =>
    request(`/spas/reservation/user/${userIds.join(',')}`, {method: 'DELETE'});
