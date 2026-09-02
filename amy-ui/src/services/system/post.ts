import {request} from '@umijs/max';
import type {RuoYiResponse, RuoYiTableResponse} from '@/types/api';

export interface PostRecord {
    postId?: number;
    postCode: string;
    postName: string;
    postSort: number;
    status?: string;
    remark?: string;
    createTime?: string
}

export const listPosts = (params: Record<string, unknown>) => request<RuoYiTableResponse<PostRecord>>('/system/post/list', {params});
export const getPost = (id: number) => request<RuoYiResponse<PostRecord>>(`/system/post/${id}`);
export const addPost = (data: PostRecord) => request('/system/post', {method: 'POST', data});
export const updatePost = (data: PostRecord) => request('/system/post', {method: 'PUT', data});
export const deletePosts = (ids: React.Key[]) => request(`/system/post/${ids.join(',')}`, {method: 'DELETE'});
