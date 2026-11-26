import {request} from '@umijs/max';
import type {RuoYiResponse, RuoYiTableResponse} from '@/types/api';
import {resolveRichText, toStoredHtml} from '@/utils/fileUrl';

export interface NoticeRecord {
    noticeId?: number;
    noticeTitle: string;
    noticeType?: string;
    noticeContent?: string;
    status?: string;
    createBy?: string;
    createTime?: string
}

export interface NoticeReadUser {
    userId: number;
    userName: string;
    nickName: string;
    deptName?: string;
    phonenumber?: string;
    readTime?: string
}

/** 读接口：富文本内 <img src> 的资源 key 统一解析成同源可访问地址 */
function withContent(record: NoticeRecord): NoticeRecord {
    if (!record || !record.noticeContent) return record;
    return {...record, noticeContent: resolveRichText(record.noticeContent)};
}

/** 写接口：富文本回显的是展示地址，提交前还原成资源 key */
function toSubmitData(data: NoticeRecord): NoticeRecord {
    return {
        ...data,
        ...(data.noticeContent
            ? {noticeContent: toStoredHtml(data.noticeContent)}
            : {}),
    };
}

export const listNotices = async (params: Record<string, unknown>) => {
    const res = await request<RuoYiTableResponse<NoticeRecord>>('/system/notice/list', {params});
    return res?.rows ? {...res, rows: res.rows.map(withContent)} : res;
};

export const getNotice = async (id: number) => {
    const res = await request<RuoYiResponse<NoticeRecord>>(`/system/notice/${id}`);
    return res?.data ? {...res, data: withContent(res.data)} : res;
};

export const addNotice = (data: NoticeRecord) => request('/system/notice', {method: 'POST', data: toSubmitData(data)});
export const updateNotice = (data: NoticeRecord) => request('/system/notice', {method: 'PUT', data: toSubmitData(data)});
export const deleteNotices = (ids: React.Key[]) => request(`/system/notice/${ids.join(',')}`, {method: 'DELETE'});
export const listNoticeReadUsers = (params: Record<string, unknown>) => request<RuoYiTableResponse<NoticeReadUser>>('/system/notice/readUsers/list', {params});
