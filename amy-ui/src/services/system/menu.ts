import {request} from '@umijs/max';
import type {RuoYiResponse} from '@/types/api';
import type {TreeOption} from './user';

export interface MenuRecord {
    menuId?: number;
    parentId?: number;
    menuName: string;
    orderNum: number;
    path?: string;
    component?: string;
    query?: string;
    routeName?: string;
    isFrame?: string;
    isCache?: string;
    menuType: 'M' | 'C' | 'F';
    visible?: string;
    status?: string;
    perms?: string;
    icon?: string;
    createTime?: string;
    children?: MenuRecord[];
}

export const listMenus = (params: Record<string, unknown>) => request<RuoYiResponse<MenuRecord[]>>('/system/menu/list', {params});
export const getMenu = (menuId: number) => request<RuoYiResponse<MenuRecord>>(`/system/menu/${menuId}`);
export const getMenuTree = () => request<RuoYiResponse<TreeOption[]>>('/system/menu/treeselect');
export const addMenu = (data: MenuRecord) => request('/system/menu', {method: 'POST', data});
export const updateMenu = (data: MenuRecord) => request('/system/menu', {method: 'PUT', data});
export const updateMenuSort = (data: {
    menuIds: string;
    orderNums: string
}) => request('/system/menu/updateSort', {method: 'PUT', data});
export const deleteMenu = (menuId: number) => request(`/system/menu/${menuId}`, {method: 'DELETE'});
