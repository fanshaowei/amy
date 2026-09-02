import {request} from '@umijs/max';
import type {RuoYiTableResponse} from '@/types/api';

export interface OperlogRecord {
    operId: number;
    title?: string;
    businessType?: number;
    requestMethod?: string;
    operatorType?: number;
    operName?: string;
    deptName?: string;
    operUrl?: string;
    operIp?: string;
    operLocation?: string;
    method?: string;
    operParam?: string;
    jsonResult?: string;
    status?: number;
    errorMsg?: string;
    operTime?: string;
    costTime?: number
}

export const listOperlogs = (params: Record<string, unknown>) => request<RuoYiTableResponse<OperlogRecord>>('/system/operlog/list', {params});
export const deleteOperlogs = (ids: React.Key[]) => request(`/system/operlog/${ids.join(',')}`, {method: 'DELETE'});
export const cleanOperlogs = () => request('/system/operlog/clean', {method: 'DELETE'});
