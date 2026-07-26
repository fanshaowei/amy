import { request } from '@umijs/max';
import type { RuoYiResponse, RuoYiTableResponse } from '@/types/api';
import type { TreeOption, UserRecord } from './user';

export interface RoleRecord {
  roleId?: number;
  roleName: string;
  roleKey: string;
  roleSort: number;
  status?: string;
  dataScope?: string;
  menuIds?: number[];
  deptIds?: number[];
  menuCheckStrictly?: boolean;
  deptCheckStrictly?: boolean;
  remark?: string;
  createTime?: string;
}

interface CheckedTreeResponse extends RuoYiResponse<TreeOption[]> { checkedKeys: number[] }

export const listRoles = (params: Record<string, unknown>) => request<RuoYiTableResponse<RoleRecord>>('/system/role/list', { params });
export const getRole = (roleId: number) => request<RuoYiResponse<RoleRecord>>(`/system/role/${roleId}`);
export const addRole = (data: RoleRecord) => request('/system/role', { method: 'POST', data });
export const updateRole = (data: RoleRecord) => request('/system/role', { method: 'PUT', data });
export const deleteRoles = (roleIds: React.Key[]) => request(`/system/role/${roleIds.join(',')}`, { method: 'DELETE' });
export const changeRoleStatus = (roleId: number, status: string) => request('/system/role/changeStatus', { method: 'PUT', data: { roleId, status } });
export const updateDataScope = (data: RoleRecord) => request('/system/role/dataScope', { method: 'PUT', data });
export const getMenuTree = () => request<RuoYiResponse<TreeOption[]>>('/system/menu/treeselect');
export const getRoleMenuTree = (roleId: number) => request<CheckedTreeResponse>(`/system/menu/roleMenuTreeselect/${roleId}`);
export const getRoleDeptTree = (roleId: number) => request<CheckedTreeResponse>(`/system/role/deptTree/${roleId}`);
export const listAllocatedUsers = (params: Record<string, unknown>) => request<RuoYiTableResponse<UserRecord>>('/system/role/authUser/allocatedList', { params });
export const listUnallocatedUsers = (params: Record<string, unknown>) => request<RuoYiTableResponse<UserRecord>>('/system/role/authUser/unallocatedList', { params });
export const cancelUserRole = (data: { userId: number; roleId: number }) => request('/system/role/authUser/cancel', { method: 'PUT', data });
export const cancelUserRoles = (params: { roleId: number; userIds: string }) => request('/system/role/authUser/cancelAll', { method: 'PUT', params });
export const selectRoleUsers = (params: { roleId: number; userIds: string }) => request('/system/role/authUser/selectAll', { method: 'PUT', params });
