import { request } from '@umijs/max';
import type { RuoYiResponse, RuoYiTableResponse } from '@/types/api';

export interface TreeOption {
  id: number;
  label: string;
  disabled?: boolean;
  children?: TreeOption[];
}

export interface UserRecord {
  userId?: number;
  deptId?: number;
  userName: string;
  nickName: string;
  email?: string;
  phonenumber?: string;
  sex?: string;
  status?: string;
  avatar?: string;
  password?: string;
  postIds?: number[];
  roleIds?: number[];
  remark?: string;
  createTime?: string;
  dept?: { deptName?: string };
}

export interface PostOption { postId: number; postName: string; status: string }
export interface RoleOption { roleId: number; roleName: string; status: string }

export interface AuthRoleOption extends RoleOption {
  roleKey: string;
  createTime?: string;
  flag?: boolean;
}

export interface UserProfileResponse extends RuoYiResponse<UserRecord> {
  roleGroup: string;
  postGroup: string;
}

export interface UserAuthRoleResponse {
  user: UserRecord;
  roles: AuthRoleOption[];
}

export interface UserDetailResponse extends RuoYiResponse<UserRecord> {
  posts: PostOption[];
  roles: RoleOption[];
  postIds: number[];
  roleIds: number[];
}

function serializeUserQuery(params: Record<string, unknown>) {
  const searchParams = new URLSearchParams();

  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '') {
      return;
    }

    if (key === 'params' && typeof value === 'object' && !Array.isArray(value)) {
      Object.entries(value as Record<string, unknown>).forEach(([nestedKey, nestedValue]) => {
        if (nestedValue !== undefined && nestedValue !== null && nestedValue !== '') {
          searchParams.append(`params[${nestedKey}]`, String(nestedValue));
        }
      });
      return;
    }

    searchParams.append(key, String(value));
  });

  return searchParams.toString();
}

export const listUsers = (params: Record<string, unknown>) => {
  const query = serializeUserQuery(params);
  return request<RuoYiTableResponse<UserRecord>>(`/system/user/list${query ? `?${query}` : ''}`);
};
export const getUser = (userId?: number) =>
  request<UserDetailResponse>(`/system/user/${userId ?? ''}`);
export const addUser = (data: UserRecord) => request('/system/user', { method: 'POST', data });
export const updateUser = (data: UserRecord) => request('/system/user', { method: 'PUT', data });
export const deleteUsers = (userIds: React.Key[]) => request(`/system/user/${userIds.join(',')}`, { method: 'DELETE' });
export const changeUserStatus = (userId: number, status: string) =>
  request('/system/user/changeStatus', { method: 'PUT', data: { userId, status } });
export const resetUserPassword = (userId: number, password: string) =>
  request('/system/user/resetPwd', { method: 'PUT', data: { userId, password } });
export const getDeptTree = () => request<RuoYiResponse<TreeOption[]>>('/system/user/deptTree');
export const getConfigValue = (key: string) => request<RuoYiResponse>(`/system/config/configKey/${key}`);
export const getUserProfile = () => request<UserProfileResponse>('/system/user/profile');
export const updateUserProfile = (data: Partial<UserRecord>) => request('/system/user/profile', { method: 'PUT', data });
export const updateUserPassword = (oldPassword: string, newPassword: string) => request('/system/user/profile/updatePwd', { method: 'PUT', data: { oldPassword, newPassword } });
export const getUserAuthRoles = (userId: number) => request<UserAuthRoleResponse>(`/system/user/authRole/${userId}`);
export const updateUserAuthRoles = (userId: number, roleIds: React.Key[]) => request('/system/user/authRole', { method: 'PUT', params: { userId, roleIds: roleIds.join(',') } });
