import { request } from '@umijs/max'; import type { RuoYiTableResponse } from '@/types/api';
export interface LoginInfoRecord { infoId: number; userName?: string; ipaddr?: string; loginLocation?: string; browser?: string; os?: string; status?: string; msg?: string; accessTime?: string }
export const listLoginInfo = (params: Record<string, unknown>) => request<RuoYiTableResponse<LoginInfoRecord>>('/system/logininfor/list', { params });
export const deleteLoginInfo = (ids: React.Key[]) => request(`/system/logininfor/${ids.join(',')}`, { method: 'DELETE' });
export const cleanLoginInfo = () => request('/system/logininfor/clean', { method: 'DELETE' });
export const unlockLogin = (userName: string) => request(`/system/logininfor/unlock/${encodeURIComponent(userName)}`);
