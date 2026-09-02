import {request} from '@umijs/max';
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

export const getUserInfo = () => request<UserInfoResult>('/system/user/getInfo');

export const getRouters = () => request<RuoYiResponse<BackendRoute[]>>('/system/menu/getRouters');
