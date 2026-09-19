package com.amy.sunpalaceartspace.domain.vo;

import lombok.Data;

/**
 * 预约订单按时间段统计结果项
 * <p>按 {@code reservationTime} 的日期归并，统计每个时间段内的预约订单数。</p>
 *
 * @author fantasyfan
 * @date 2026-09-19
 */
@Data
public class ReservationOrderStatisticVO {

    /** 分组项目ID */
    private Long projectId;

    /** 时间段（按预约时间日期归并，格式 yyyy-MM-dd） */
    private String reservationDate;

    /** 该时间段的预约订单数 */
    private Integer count = 0;
}
