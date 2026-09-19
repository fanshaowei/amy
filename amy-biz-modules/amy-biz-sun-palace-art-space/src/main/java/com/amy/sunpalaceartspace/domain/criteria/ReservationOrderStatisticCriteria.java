package com.amy.sunpalaceartspace.domain.criteria;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

/**
 * 预约订单统计查询对象 ReservationOrderStatisticReq
 *
 * @Description 按时间段统计预约订单数的查询入参
 * @author fantasyfan
 * @date 2026-09-19
 */
@Builder
@Data
public class ReservationOrderStatisticCriteria {

    /** 预约项目ID（可选；为空表示统计全部项目） */
    private Long projectId;

    /** 预约时间查询起始（含），格式 yyyy-MM-dd HH:mm:ss */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date reservationTimeStart;

    /** 预约时间查询结束（含），格式 yyyy-MM-dd HH:mm:ss */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date reservationTimeEnd;
}
