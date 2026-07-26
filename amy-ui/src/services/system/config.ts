import { request } from '@umijs/max'; import type { RuoYiResponse, RuoYiTableResponse } from '@/types/api';
export interface ConfigRecord { configId?: number; configName: string; configKey: string; configValue: string; configType?: string; remark?: string; createTime?: string }
export const listConfigs = (params: Record<string, unknown>) => request<RuoYiTableResponse<ConfigRecord>>('/system/config/list', { params });
export const getConfig = (id: number) => request<RuoYiResponse<ConfigRecord>>(`/system/config/${id}`);
export const addConfig = (data: ConfigRecord) => request('/system/config', { method: 'POST', data });
export const updateConfig = (data: ConfigRecord) => request('/system/config', { method: 'PUT', data });
export const deleteConfigs = (ids: React.Key[]) => request(`/system/config/${ids.join(',')}`, { method: 'DELETE' });
export const refreshConfigCache = () => request('/system/config/refreshCache', { method: 'DELETE' });
