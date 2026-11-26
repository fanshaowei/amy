import {request} from '@umijs/max';
import type {RuoYiResponse, SpasTableResponse} from '@/types/api';
import {resolveFileSrc, resolveRichText, toStoredHtml, toStoredKey} from '@/utils/fileUrl';

/** 项目管理（biz_spas_projects） */
export interface ProjectRecord {
    projectId?: number;
    projectName: string;
    /** 封面图 URL（200*200） */
    coverArtUrl: string;
    /** 每日可预约截止时间（HH:mm:ss） */
    cutOffTime?: string;
    /** 预约停留秒数 */
    reservationStaySecond?: number;
    /** 预约开始时间（HH:mm:ss） */
    reservationStartTime?: string;
    /** 预约结束时间（HH:mm:ss） */
    reservationEndTime?: string;
    /** 预约间隔（后端单位为秒，前端表单以分钟展示，提交前需 *60） */
    reservationIntervalSecond?: number;
    /** 每个时间段可预约的人数 */
    reservationCount?: number;
    /** 可提前预约天数 */
    advanceReservationDays?: number;
    /** 随行人数 */
    travelerCount?: number;
    /** 每人每个月最多预约数量 */
    maxMonthlyReservationCount?: number;
    /** 每月取消几次不能预约 */
    maxMonthlyCancellationCount?: number;
    /** 多少天内只能预约1次 */
    reservationFrequencyDays?: number;
    /** 预约须知（富文本 HTML） */
    reservationNotes?: string;
    sort?: number;
    /** 状态（0=停用/隐藏 1=正常/展示） */
    status?: string;
    createBy?: string;
    createTime?: string
}

/** 文件服务上传结果 */
export interface UploadResult {
    name: string;
    url: string
}

/**
 * 读接口：services 适配层统一把后端返回的<b>资源 key</b>转成同源可访问地址。
 * 页面/组件拿到的始终是可直接用于展示的地址，无需各自拼前缀。
 * 只处理确实存在的字段，避免把 undefined 变成空串影响表单。
 */
function withFileUrls(record: ProjectRecord): ProjectRecord {
    if (!record) return record;
    return {
        ...record,
        coverArtUrl: resolveFileSrc(record.coverArtUrl),
        ...(record.reservationNotes
            ? {reservationNotes: resolveRichText(record.reservationNotes)}
            : {}),
    };
}

/**
 * 写接口：表单回显的是展示地址，提交前统一还原成资源 key，
 * 保证库里只存 amy-file 按 file.prefix 生成的 key，不含域名与网关前缀。
 */
function toSubmitData(data: ProjectRecord): ProjectRecord {
    return {
        ...data,
        coverArtUrl: toStoredKey(data.coverArtUrl),
        ...(data.reservationNotes
            ? {reservationNotes: toStoredHtml(data.reservationNotes)}
            : {}),
    };
}

export const listProjects = async (params: Record<string, unknown>) => {
    const res = await request<SpasTableResponse<ProjectRecord>>('/spas/project/list', {params});
    if (!res?.data) return res;
    return {...res, data: {...res.data, rows: (res.data.rows || []).map(withFileUrls)}};
};

export const getProject = async (projectId: number) => {
    const res = await request<RuoYiResponse<ProjectRecord>>(`/spas/project/${projectId}`);
    return res?.data ? {...res, data: withFileUrls(res.data)} : res;
};

export const addProject = (data: ProjectRecord) => request('/spas/project', {method: 'POST', data: toSubmitData(data)});
export const updateProject = (data: ProjectRecord) => request('/spas/project', {method: 'PUT', data: toSubmitData(data)});
export const deleteProjects = (projectIds: React.Key[]) => request(`/spas/project/${projectIds.join(',')}`, {method: 'DELETE'});
export const refreshProject = (projectIds: React.Key[]) => request('/spas/inner/reservationStock/releaseBatch1', {method: 'POST'});

/** 上传图片，走网关文件服务 /file/upload；返回的 url 是 key，在此统一解析成可预览地址 */
export const uploadImage = async (file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    const res = await request<RuoYiResponse<UploadResult>>('/file/upload', {method: 'POST', data: formData});
    return res?.data ? {...res, data: {...res.data, url: resolveFileSrc(res.data.url)}} : res;
};
