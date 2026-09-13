package com.amy.sunpalaceartspace.domain.vo;

import lombok.Data;

/**
 * 控制台趋势项（按日期聚合的统计）
 *
 * @author fantasyfan
 * @date 2026-09-13
 */
@Data
public class DashboardTrendItemVO {

    /** 日期（yyyy-MM-dd） */
    private String date;

    /** 当日数量 */
    private Long count;
}
