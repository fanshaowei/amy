package com.amy.sunpalaceartspace.service;

import com.amy.sunpalaceartspace.domain.resp.DashboardStatisticsResp;

/**
 * 控制台统计 Service
 *
 * @author fantasyfan
 * @date 2026-09-13
 */
public interface IStatisticService {

    /**
     * 获取控制台统计信息
     *
     * @return 统计响应对象
     */
    DashboardStatisticsResp getDashboardStatistics();
}
