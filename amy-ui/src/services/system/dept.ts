import { request } from '@umijs/max';
import type { RuoYiResponse } from '@/types/api';
export interface DeptRecord { deptId?: number; parentId?: number; parentName?: string; deptName: string; orderNum: number; leader?: string; phone?: string; email?: string; status?: string; createTime?: string; children?: DeptRecord[] }
export const listDepts = (params?: Record<string, unknown>) => request<RuoYiResponse<DeptRecord[]>>('/system/dept/list', { params });
export const listDeptsExclude = (id: number) => request<RuoYiResponse<DeptRecord[]>>(`/system/dept/list/exclude/${id}`);
export const getDept = (id: number) => request<RuoYiResponse<DeptRecord>>(`/system/dept/${id}`);
export const addDept = (data: DeptRecord) => request('/system/dept', { method: 'POST', data });
export const updateDept = (data: DeptRecord) => request('/system/dept', { method: 'PUT', data });
export const updateDeptSort = (data: { deptIds: string; orderNums: string }) => request('/system/dept/updateSort', { method: 'PUT', data });
export const deleteDept = (id: number) => request(`/system/dept/${id}`, { method: 'DELETE' });
