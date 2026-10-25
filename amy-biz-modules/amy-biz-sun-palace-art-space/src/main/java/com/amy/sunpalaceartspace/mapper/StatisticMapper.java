package com.amy.sunpalaceartspace.mapper;

import java.util.Date;
import java.util.List;

import com.amy.sunpalaceartspace.domain.vo.DashboardTrendItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 控制台统计 Mapper
 *
 * <p>约定：所有按「自然日」统计的 SQL 一律使用<b>左闭右开区间</b>
 * {@code col >= #{start} and col < #{end}}，<b>禁止</b>对 where 字段套
 * {@code date(col)} / {@code DATE_FORMAT(col,...)} / {@code date_add} 等函数——
 * 一旦对列做运算，MySQL 就无法用该列上的索引做 range scan，只能全表扫。
 * 时间边界由 Service 层用 {@code LocalDate} 计算后传入，保证应用时区与 DB 时区解耦。
 *
 * @author fantasyfan
 * @date 2026-09-13
 */
@Mapper
public interface StatisticMapper {

    /**
     * 今日预约人数
     *
     * @param start 今日 00:00:00（含）
     * @param end   次日 00:00:00（不含）
     * @return 数量
     */
    Long selectTodayReservationCount(@Param("start") Date start, @Param("end") Date end);

    /**
     * 今日核销人数
     *
     * @param start 今日 00:00:00（含）
     * @param end   次日 00:00:00（不含）
     * @return 数量
     */
    Long selectTodayVerifyCount(@Param("start") Date start, @Param("end") Date end);

    /**
     * 明日预约人数
     *
     * @param start 次日 00:00:00（含）
     * @param end   第三日 00:00:00（不含）
     * @return 数量
     */
    Long selectTomorrowReservationCount(@Param("start") Date start, @Param("end") Date end);

    /**
     * 预约总数
     *
     * @return 数量
     */
    Long selectTotalReservationCount();

    /**
     * 今日新增会员
     *
     * @param start 今日 00:00:00（含）
     * @param end   次日 00:00:00（不含）
     * @return 数量
     */
    Long selectTodayNewUserCount(@Param("start") Date start, @Param("end") Date end);

    /**
     * 会员总数
     *
     * @return 数量
     */
    Long selectTotalUserCount();

    /**
     * 近30天预约趋势
     *
     * @param start 起始日 00:00:00（含），取 today-29
     * @param end   截止日 00:00:00（不含），取 tomorrow
     * @return 按日期聚合的数量
     */
    List<DashboardTrendItemVO> selectLast30DaysReservationTrend(@Param("start") Date start,
            @Param("end") Date end);

    /**
     * 近30天用户增长趋势
     *
     * @param start 起始日 00:00:00（含），取 today-29
     * @param end   截止日 00:00:00（不含），取 tomorrow
     * @return 按日期聚合的数量
     */
    List<DashboardTrendItemVO> selectLast30DaysUserTrend(@Param("start") Date start,
            @Param("end") Date end);
}
