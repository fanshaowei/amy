package com.amy.sunpalaceartspace.domain.vo;

import lombok.Data;

/**
 * 预约订单按日期+时间段统计结果项
 * <p>按 {@code reservationTime} 的日期与时段（到分钟）归并，统计每个时间段的预约订单数。</p>
 *
 * @author fantasyfan
 * @date 2026-09-19
 */
@Data
public class ReservationOrderStatisticByTimeVO {

    /** 分组项目ID */
    private Long projectId;

    /** 日期（yyyy-MM-dd） */
    private String reservationDate;

    /** 时间段（HH:mm:ss，如 09:00:00、09:30:00） */
    private String reservationTime;

    /** 该日期+时间段的预约订单数 */
    private Long count;
}
