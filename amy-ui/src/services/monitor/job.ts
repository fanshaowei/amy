import {request} from '@umijs/max';
import type {RuoYiResponse, RuoYiTableResponse} from '@/types/api';

/** 定时任务调度表 sys_job（字段均为字符串，与后端 SysJob 实体一致） */
export interface JobRecord {
    jobId?: number;
    jobName?: string;
    /** 任务组名 DEFAULT / SYSTEM */
    jobGroup?: string;
    /** 调用目标字符串，如 ryTask.ryParams('ry') */
    invokeTarget?: string;
    /** cron 执行表达式 */
    cronExpression?: string;
    /** 计划策略 0默认 1立即触发 2触发一次 3不触发 */
    misfirePolicy?: string;
    /** 是否并发 0允许 1禁止 */
    concurrent?: string;
    /** 任务状态 0正常 1暂停 */
    status?: string;
    createBy?: string;
    createTime?: string;
    updateBy?: string;
    updateTime?: string;
    remark?: string;
    /** 下一次执行时间（后端根据 cronExpression 计算，只读） */
    nextValidTime?: string;
}

/** 定时任务调度日志表 sys_job_log */
export interface JobLogRecord {
    jobLogId?: number;
    jobName?: string;
    jobGroup?: string;
    invokeTarget?: string;
    /** 日志信息 */
    jobMessage?: string;
    /** 执行状态 0正常 1失败 */
    status?: string;
    /** 异常信息 */
    exceptionInfo?: string;
    startTime?: string;
    endTime?: string;
    createTime?: string;
}

const JOB_BASE = '/schedule/job';

export const listJobs = (params: Record<string, unknown>) =>
    request<RuoYiTableResponse<JobRecord>>(`${JOB_BASE}/list`, {params});

export const getJob = (jobId: number) =>
    request<RuoYiResponse<JobRecord>>(`${JOB_BASE}/${jobId}`);

export const addJob = (data: JobRecord) =>
    request<RuoYiResponse>(JOB_BASE, {method: 'POST', data});

export const updateJob = (data: JobRecord) =>
    request<RuoYiResponse>(JOB_BASE, {method: 'PUT', data});

export const deleteJobs = (ids: React.Key[]) =>
    request<RuoYiResponse>(`${JOB_BASE}/${ids.join(',')}`, {method: 'DELETE'});

/** 任务状态修改（启用 / 停用） */
export const changeJobStatus = (jobId: number, status: string) =>
    request<RuoYiResponse>(`${JOB_BASE}/changeStatus`, {method: 'PUT', data: {jobId, status}});

/** 定时任务立即执行一次 */
export const runJob = (jobId: number, jobGroup: string) =>
    request<RuoYiResponse>(`${JOB_BASE}/run`, {method: 'PUT', data: {jobId, jobGroup}});

const LOG_BASE = '/schedule/job/log';

export const listJobLogs = (params: Record<string, unknown>) =>
    request<RuoYiTableResponse<JobLogRecord>>(`${LOG_BASE}/list`, {params});

export const deleteJobLogs = (ids: React.Key[]) =>
    request<RuoYiResponse>(`${LOG_BASE}/${ids.join(',')}`, {method: 'DELETE'});

export const cleanJobLogs = () =>
    request<RuoYiResponse>(`${LOG_BASE}/clean`, {method: 'DELETE'});
