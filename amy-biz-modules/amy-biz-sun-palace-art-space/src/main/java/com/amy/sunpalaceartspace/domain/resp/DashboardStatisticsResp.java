package com.amy.sunpalaceartspace.domain.resp;

import java.util.List;

import com.amy.sunpalaceartspace.domain.vo.DashboardTrendItemVO;
import lombok.Data;

/**
 * 控制台统计响应对象
 *
 * @author fantasyfan
 * @date 2026-09-13
 */
@Data
public class DashboardStatisticsResp {

    /** 今日预约人数 */
    private Long todayReservationCount;

    /** 今日核销人数 */
    private Long todayVerifyCount;

    /** 明日预约人数 */
    private Long tomorrowReservationCount;

    /** 预约总数 */
    private Long totalReservationCount;

    /** 今日新增会员 */
    private Long todayNewUserCount;

    /** 会员总数 */
    private Long totalUserCount;

    /** 近30天预约趋势（按日期顺序） */
    private List<DashboardTrendItemVO> last30DaysReservationTrend;

    /** 近30天用户增长趋势（按日期顺序） */
    private List<DashboardTrendItemVO> last30DaysUserTrend;
}
