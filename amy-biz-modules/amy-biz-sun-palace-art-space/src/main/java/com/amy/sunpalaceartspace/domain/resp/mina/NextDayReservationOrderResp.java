package com.amy.sunpalaceartspace.domain.resp.mina;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 次日（明天）预约订单响应对象
 *
 * <p>用于「查询预约了第二天（明天）的号的订单」，典型场景是<b>次日到访提醒</b>
 * （定时任务拉取明天的订单 → 按会员推送微信订阅消息）。</p>
 *
 * <p>字段由 {@code ReservationOrderMapper.xml} 的 {@code NextDayReservationOrderResult}
 * resultMap 直接映射（含项目名、会员姓名两张 LEFT JOIN）。</p>
 *
 * @ClassName NextDayReservationOrderResp
 * @Description 次日预约订单返回对象
 * @Author fantasyfan
 * @Date 2026-10-26 01:07
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NextDayReservationOrderResp {
    /** 预约人姓名 */
    private String name;

    /** 联系电话 */
    private String phone;

    /** 预约时间（含时间段，如 2026-10-27 09:30:00） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date reservationTime;

}
