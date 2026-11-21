package com.amy.sunpalaceartspace.domain.criteria;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 由「生效的放号库存」推导出来的预约订单查询范围：一个项目 + 一个自然日。
 *
 * <p>startTime / endTime 分别是本地时区下当天 00:00:00（含）与次日 00:00:00（不含）。
 * SQL 用 {@code reservation_time >= startTime and reservation_time < endTime} 的<b>裸列半开区间</b>，
 *
 * @ClassName ReservationOrderStockScope
 * @Description 库存放号日期对应的订单查询范围
 * @Author fantasyfan
 * @Date 2026-11-21
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationOrderStockScope {

    /** 项目ID（库存行所属项目，订单必须同项目） */
    private Long projectId;

    /** 当天 00:00:00（含） */
    private Date startTime;

    /** 次日 00:00:00（不含） */
    private Date endTime;
}
