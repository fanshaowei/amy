import { request } from '@umijs/max';
import type { RuoYiResponse, RuoYiTableResponse } from '@/types/api';
export interface DictTypeRecord { dictId?: number; dictName: string; dictType: string; status?: string; remark?: string; createTime?: string }
export interface DictDataRecord { dictCode?: number; dictSort: number; dictLabel: string; dictValue: string; dictType: string; cssClass?: string; listClass?: string; isDefault?: string; status?: string; remark?: string; createTime?: string }
export const listDictTypes = (params: Record<string, unknown>) => request<RuoYiTableResponse<DictTypeRecord>>('/system/dict/type/list', { params });
export const getDictType = (id: number) => request<RuoYiResponse<DictTypeRecord>>(`/system/dict/type/${id}`);
export const addDictType = (data: DictTypeRecord) => request('/system/dict/type', { method: 'POST', data });
export const updateDictType = (data: DictTypeRecord) => request('/system/dict/type', { method: 'PUT', data });
export const deleteDictTypes = (ids: React.Key[]) => request(`/system/dict/type/${ids.join(',')}`, { method: 'DELETE' });
export const refreshDictCache = () => request('/system/dict/type/refreshCache', { method: 'DELETE' });
export const listDictData = (params: Record<string, unknown>) => request<RuoYiTableResponse<DictDataRecord>>('/system/dict/data/list', { params });
export const getDictData = (id: number) => request<RuoYiResponse<DictDataRecord>>(`/system/dict/data/${id}`);
export const addDictData = (data: DictDataRecord) => request('/system/dict/data', { method: 'POST', data });
export const updateDictData = (data: DictDataRecord) => request('/system/dict/data', { method: 'PUT', data });
export const deleteDictData = (ids: React.Key[]) => request(`/system/dict/data/${ids.join(',')}`, { method: 'DELETE' });
