import {request} from '@umijs/max';
import type {RuoYiResponse, SpasTableResponse} from '@/types/api';
import {resolveFileSrc} from '@/utils/fileUrl';

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

/** services 适配层：把头像资源 key 转成同源可访问地址 */
function withAvatar(record: ReservationUserRecord): ReservationUserRecord {
    if (!record || !record.avatarImgUrl) return record;
    return {...record, avatarImgUrl: resolveFileSrc(record.avatarImgUrl)};
}

export const listUsers = async (params: Record<string, unknown>) => {
    const res = await request<SpasTableResponse<ReservationUserRecord>>('/spas/reservation/user/list', {params});
    if (!res?.data) return res;
    return {...res, data: {...res.data, rows: (res.data.rows || []).map(withAvatar)}};
};

export const getUser = async (reservationUserId: number) => {
    const res = await request<RuoYiResponse<ReservationUserRecord>>(`/spas/reservation/user/${reservationUserId}`);
    return res?.data ? {...res, data: withAvatar(res.data)} : res;
};

/** 切换身份（1用户 2核销员） */
export const changeUserType = (reservationUserId: number, type: number) =>
    request(`/spas/reservation/user/changeType/${reservationUserId}/${type}`, {method: 'PUT'});
export const deleteUsers = (userIds: React.Key[]) =>
    request(`/spas/reservation/user/${userIds.join(',')}`, {method: 'DELETE'});
