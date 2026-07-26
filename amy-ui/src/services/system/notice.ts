import { request } from '@umijs/max'; import type { RuoYiResponse, RuoYiTableResponse } from '@/types/api';
export interface NoticeRecord { noticeId?: number; noticeTitle: string; noticeType?: string; noticeContent?: string; status?: string; createBy?: string; createTime?: string }
export interface NoticeReadUser { userId: number; userName: string; nickName: string; deptName?: string; phonenumber?: string; readTime?: string }
export const listNotices = (params: Record<string, unknown>) => request<RuoYiTableResponse<NoticeRecord>>('/system/notice/list', { params });
export const getNotice = (id: number) => request<RuoYiResponse<NoticeRecord>>(`/system/notice/${id}`);
export const addNotice = (data: NoticeRecord) => request('/system/notice', { method: 'POST', data });
export const updateNotice = (data: NoticeRecord) => request('/system/notice', { method: 'PUT', data });
export const deleteNotices = (ids: React.Key[]) => request(`/system/notice/${ids.join(',')}`, { method: 'DELETE' });
export const listNoticeReadUsers = (params: Record<string, unknown>) => request<RuoYiTableResponse<NoticeReadUser>>('/system/notice/readUsers/list', { params });
