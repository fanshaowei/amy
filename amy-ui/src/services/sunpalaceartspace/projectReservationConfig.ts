import {request} from '@umijs/max';
import type {RuoYiResponse, SpasTableResponse} from '@/types/api';

/** 项目预约时段配置（biz_spas_project_reservation_config），配置对项目每一天生效 */
export interface ReservationConfigRecord {
    configId?: number;
    projectId?: number;
    /** 预约开始时间（HH:mm:ss） */
    startTime?: string;
    /** 预约结束时间（HH:mm:ss，不含，左闭右开） */
    endTime?: string;
    /** 是否可预约（true=可预约 false=不可预约） */
    reservationEnabled?: boolean;
    projectName?: string;
    createBy?: string;
    createTime?: string;
}

/** 按项目分组的时段配置返回 */
export interface ReservationConfigGroup {
    projectId: number;
    projectName?: string;
    configs: ReservationConfigRecord[];
}

/** 分页查询时段配置（可按 projectId 过滤） */
export const listReservationConfigs = (params: Record<string, unknown>) =>
    request<SpasTableResponse<ReservationConfigRecord>>('/spas/reservationConfig/list', {params});

/** 按项目ID批量查询并按项目分组 */
export const groupReservationConfigsByProject = (projectIds: number[]) =>
    request<RuoYiResponse<ReservationConfigGroup[]>>('/spas/reservationConfig/groupby/project', {
        params: {projectIds: projectIds.join(',')}
    });

/** 新增时段配置 */
export const addReservationConfig = (data: ReservationConfigRecord) =>
    request<RuoYiResponse<boolean>>('/spas/reservationConfig', {method: 'POST', data});

/** 修改时段是否可预约 */
export const updateReservationEnabled = (data: { configId: number; reservationEnabled: boolean }) =>
    request<RuoYiResponse<boolean>>('/spas/reservationConfig/update/reservation/enabled', {method: 'PUT', data});

/** 删除时段配置（软删除） */
export const deleteReservationConfigs = (configIds: React.Key[]) =>
    request<RuoYiResponse<boolean>>(`/spas/reservationConfig/${configIds.join(',')}`, {method: 'DELETE'});
