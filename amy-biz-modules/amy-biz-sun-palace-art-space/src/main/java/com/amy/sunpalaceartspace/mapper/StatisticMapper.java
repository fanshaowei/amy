package com.amy.sunpalaceartspace.mapper;

import java.util.List;

import com.amy.sunpalaceartspace.domain.vo.DashboardTrendItemVO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 控制台统计 Mapper
 *
 * @author fantasyfan
 * @date 2026-09-13
 */
@Mapper
public interface StatisticMapper {

    /**
     * 今日预约人数
     *
     * @return 数量
     */
    Long selectTodayReservationCount();

    /**
     * 今日核销人数
     *
     * @return 数量
     */
    Long selectTodayVerifyCount();

    /**
     * 明日预约人数
     *
     * @return 数量
     */
    Long selectTomorrowReservationCount();

    /**
     * 预约总数
     *
     * @return 数量
     */
    Long selectTotalReservationCount();

    /**
     * 今日新增会员
     *
     * @return 数量
     */
    Long selectTodayNewUserCount();

    /**
     * 会员总数
     *
     * @return 数量
     */
    Long selectTotalUserCount();

    /**
     * 近30天预约趋势
     *
     * @return 按日期聚合的数量
     */
    List<DashboardTrendItemVO> selectLast30DaysReservationTrend();

    /**
     * 近30天用户增长趋势
     *
     * @return 按日期聚合的数量
     */
    List<DashboardTrendItemVO> selectLast30DaysUserTrend();
}
