package com.amy.sunpalaceartspace.domain.resp.mina;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 小程序「预约动态」订单响应对象。
 *
 * <p>数据口径：先取 {@code biz_spas_reservation_stock} 中 status=1 的生效放号库存的
 * {@code reservation_time}，再按「项目 + 日期」去订单表匹配，因此日期由放号库存决定</p>
 *
 * @ClassName NextDayReservationOrderResp
 * @Description 生效放号日期下的预约订单返回对象
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
