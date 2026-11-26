import {request} from '@umijs/max';
import {resolveFileSrc} from '@/utils/fileUrl';
import type {
    BackendRoute,
    CaptchaResult,
    LoginParams,
    LoginResult,
    RuoYiResponse,
    UserInfoResult
} from '@/types/api';

export const login = (data: LoginParams) =>
    request<RuoYiResponse<LoginResult>>('/auth/login', {
        method: 'POST',
        data,
        headers: {isToken: 'false', repeatSubmit: 'false'}
    });

export const logout = () => request<RuoYiResponse>('/auth/logout', {method: 'DELETE'});

export const getCaptcha = () =>
    request<CaptchaResult>('/code', {
        method: 'GET',
        timeout: 20000,
        headers: {isToken: 'false'}
    });

/**
 * 当前登录用户信息。sys_user.avatar 存的是资源 key（<file.prefix>/...），
 * 后端出站原样返回；这里统一解析成同源可访问地址，右上角 AvatarDropdown 的
 * <Avatar src> 才能直接显示（否则浏览器会去请求站点根路径 /<file.prefix>/... 而拿到 index.html）。
 */
export const getUserInfo = async (): Promise<UserInfoResult> => {
    const result = await request<UserInfoResult>('/system/user/getInfo');
    const user = result.user;
    return user?.avatar
        ? {...result, user: {...user, avatar: resolveFileSrc(user.avatar)}}
        : result;
};

export const getRouters = () => request<RuoYiResponse<BackendRoute[]>>('/system/menu/getRouters');
