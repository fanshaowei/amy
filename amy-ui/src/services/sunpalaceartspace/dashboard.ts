import {request} from '@umijs/max';
import type {RuoYiResponse} from '@/types/api';

/** 控制台趋势项（按日期聚合的统计） */
export interface DashboardTrendItem {
    /** 日期（yyyy-MM-dd） */
    date: string;
    /** 当日数量 */
    count: number;
}

/** 控制台统计信息（对应后端 DashboardStatisticsResp） */
export interface DashboardStatistics {
    /** 今日预约人数 */
    todayReservationCount?: number;
    /** 今日核销人数 */
    todayVerifyCount?: number;
    /** 明日预约人数 */
    tomorrowReservationCount?: number;
    /** 预约总数 */
    totalReservationCount?: number;
    /** 今日新增会员 */
    todayNewUserCount?: number;
    /** 会员总数 */
    totalUserCount?: number;
    /** 近30天预约趋势（按日期顺序） */
    last30DaysReservationTrend?: DashboardTrendItem[];
    /** 近30天用户增长趋势（按日期顺序） */
    last30DaysUserTrend?: DashboardTrendItem[];
}

/** 获取控制台统计信息 */
export const getDashboardStatistics = () =>
    request<RuoYiResponse<DashboardStatistics>>('/amyBizSunPalaceArtSpace/dashboard/statistics');
