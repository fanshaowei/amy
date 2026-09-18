import {request} from '@umijs/max';
import type {RuoYiResponse, RuoYiTableResponse} from '@/types/api';

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
    /** 预约间隔（分钟） */
    reservationIntervalSecond?: number;
    /** 每个时间段可预约的人数 */
    reservationCount?: number;
    /** 可提前预约天数 */
    advanceReservationDays?: number;
    /** 随行人数 */
    travelerCount?: number;
    /** 预约须知（富文本 HTML） */
    reservationNotes?: string;
    sort?: number;
    /** 状态（0展示 1隐藏） */
    status?: string;
    createBy?: string;
    createTime?: string
}

/** 文件服务上传结果 */
export interface UploadResult {
    name: string;
    url: string
}

export const listProjects = (params: Record<string, unknown>) => request<RuoYiTableResponse<ProjectRecord>>('/amyBizSunPalaceArtSpace/project/list', {params});
export const getProject = (projectId: number) => request<RuoYiResponse<ProjectRecord>>(`/amyBizSunPalaceArtSpace/project/${projectId}`);
export const addProject = (data: ProjectRecord) => request('/amyBizSunPalaceArtSpace/project', {method: 'POST', data});
export const updateProject = (data: ProjectRecord) => request('/amyBizSunPalaceArtSpace/project', {method: 'PUT', data});
export const deleteProjects = (projectIds: React.Key[]) => request(`/amyBizSunPalaceArtSpace/project/${projectIds.join(',')}`, {method: 'DELETE'});

/** 上传图片，走网关文件服务 /file/upload */
export const uploadImage = (file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    return request<RuoYiResponse<UploadResult>>('/file/upload', {method: 'POST', data: formData});
};
