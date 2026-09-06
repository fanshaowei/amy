import {request} from '@umijs/max';
import type {RuoYiTableResponse} from '@/types/api';

/** 当前在线会话 */
export interface UserOnlineRecord {
    /** 会话编号 */
    tokenId?: string;
    /** 用户名称 */
    userName?: string;
    /** 登录 IP 地址 */
    ipaddr?: string;
    /** 登录地点 */
    loginLocation?: string;
    /** 浏览器类型 */
    browser?: string;
    /** 操作系统 */
    os?: string;
    /** 登录时间（毫秒时间戳） */
    loginTime?: number;
}

/** 查询在线用户列表 */
export const listOnline = (params: {ipaddr?: string; userName?: string}) =>
    request<RuoYiTableResponse<UserOnlineRecord>>('/system/online/list', {params});

/** 强退单个用户（DELETE /system/online/{tokenId}） */
export const forceLogout = (tokenId: string) =>
    request<unknown>(`/system/online/${encodeURIComponent(tokenId)}`, {method: 'DELETE'});