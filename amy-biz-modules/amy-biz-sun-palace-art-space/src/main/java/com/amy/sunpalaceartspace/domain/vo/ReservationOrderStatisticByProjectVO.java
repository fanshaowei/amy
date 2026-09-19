package com.amy.sunpalaceartspace.domain.vo;

import lombok.Data;

/**
 * 预约订单按项目分组统计结果项
 * <p>按 {@code projectId} 归并，统计指定日期时间范围内的预约订单总数。</p>
 *
 * @author fantasyfan
 * @date 2026-09-19
 */
@Data
public class ReservationOrderStatisticByProjectVO {

    /** 分组项目ID */
    private Long projectId;

    /** 该项目的预约订单总数 */
    private Integer count;
}
