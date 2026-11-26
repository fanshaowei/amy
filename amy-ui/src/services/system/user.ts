import {request} from '@umijs/max';
import {resolveFileSrc} from '@/utils/fileUrl';
import type {RuoYiResponse, RuoYiTableResponse} from '@/types/api';

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

export interface PostOption {
    postId: number;
    postName: string;
    status: string
}

export interface RoleOption {
    roleId: number;
    roleName: string;
    status: string
}

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
export const addUser = (data: UserRecord) => request('/system/user', {method: 'POST', data});
export const updateUser = (data: UserRecord) => request('/system/user', {method: 'PUT', data});
export const deleteUsers = (userIds: React.Key[]) => request(`/system/user/${userIds.join(',')}`, {method: 'DELETE'});
export const changeUserStatus = (userId: number, status: string) =>
    request('/system/user/changeStatus', {method: 'PUT', data: {userId, status}});
export const resetUserPassword = (userId: number, password: string) =>
    request('/system/user/resetPwd', {method: 'PUT', data: {userId, password}});
export const getDeptTree = () => request<RuoYiResponse<TreeOption[]>>('/system/user/deptTree');
export const getConfigValue = (key: string) => request<RuoYiResponse>(`/system/config/configKey/${key}`);
/**
 * sys_user.avatar 存的是资源 key，个人中心 <Avatar> 需要可直接访问的地址。
 * 只在这里解析：用户管理的 listUsers / getUser 会把 record 整表回显后提交给
 * updateUser，若在那里解析展示地址，/api/file/... 前缀就会被写回数据库。
 */
export const getUserProfile = async () => {
    const response = await request<UserProfileResponse>('/system/user/profile');
    return response.data?.avatar
        ? {...response, data: {...response.data, avatar: resolveFileSrc(response.data.avatar)}}
        : response;
};
/**
 * 头像走独立的 /profile/avatar 接口。表单 initialValues 会带上已解析的展示地址，
 * 提交时必须剔除，避免把 /api/file/... 这类访问前缀写回 sys_user.avatar。
 */
export const updateUserProfile = (data: Partial<UserRecord>) => {
    const payload = {...data};
    delete payload.avatar;
    return request('/system/user/profile', {method: 'PUT', data: payload});
};
export const updateUserPassword = (oldPassword: string, newPassword: string) => request('/system/user/profile/updatePwd', {
    method: 'PUT',
    data: {oldPassword, newPassword}
});
/**
 * 头像上传结果。
 * 后端 SysProfileController#avatar 直接返回 AjaxResult 的 put 字段：{code, msg, imgUrl}，
 * 不套一层 data，所以不能用通用的 RuoYiResponse<T>。
 */
export interface AvatarUploadResult {
    code: number;
    msg: string;
    imgUrl: string;
}

/**
 * 上传用户头像。
 * 后端签名是 @RequestParam("avatarfile") MultipartFile，字段名必须固定为 avatarfile；
 * 上传成功后会同时 sys_user.avatar 和登录缓存（LoginUser）。
 */
export const uploadUserAvatar = async (file: File) => {
    const formData = new FormData();
    formData.append('avatarfile', file);
    const result = await request<AvatarUploadResult>('/system/user/profile/avatar', {method: 'POST', data: formData});
    // imgUrl 是资源 key，解析后回填才能在 <Avatar> 里立即显示
    return {...result, imgUrl: resolveFileSrc(result.imgUrl)};
};

export const getUserAuthRoles = (userId: number) =>
    request<UserAuthRoleResponse>(`/system/user/authRole/${userId}`);
export const updateUserAuthRoles = (userId: number, roleIds: React.Key[]) => request('/system/user/authRole', {
    method: 'PUT',
    params: {userId, roleIds: roleIds.join(',')}
});
