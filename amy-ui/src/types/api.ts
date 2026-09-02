export interface RuoYiResponse<T = unknown> {
    code: number;
    msg: string;
    data: T;
}

export interface RuoYiTableResponse<T> extends Omit<RuoYiResponse<T[]>, 'data'> {
    rows: T[];
    total: number;
}

export interface LoginParams {
    username: string;
    password: string;
    code?: string;
    uuid?: string;
}

export interface LoginResult {
    access_token: string;
    expires_in: number;
}

export interface CaptchaResult {
    captchaEnabled?: boolean;
    img?: string;
    uuid?: string;
}

export interface CurrentUser {
    userId: number;
    userName: string;
    nickName: string;
    avatar?: string;
    dept?: { deptName?: string };
}

export interface UserInfoResult {
    code: number;
    msg: string;
    user: CurrentUser;
    roles: string[];
    permissions: string[];
    pwdChrtype?: string;
    isDefaultModifyPwd?: boolean;
    isPasswordExpired?: boolean;
}

export interface BackendRouteMeta {
    title?: string;
    icon?: string;
    noCache?: boolean;
    link?: string;
}

export interface BackendRoute {
    name?: string;
    path: string;
    hidden?: boolean;
    redirect?: string;
    component?: string;
    alwaysShow?: boolean;
    meta?: BackendRouteMeta;
    children?: BackendRoute[];
}
