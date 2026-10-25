package com.amy.sunpalaceartspace.service.impl;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.amy.sunpalaceartspace.domain.resp.DashboardStatisticsResp;
import com.amy.sunpalaceartspace.domain.vo.DashboardTrendItemVO;
import com.amy.sunpalaceartspace.mapper.StatisticMapper;
import com.amy.sunpalaceartspace.service.IStatisticService;
import lombok.RequiredArgsConstructor;

/**
 * 控制台统计 Service 实现
 *
 * @author fantasyfan
 * @date 2026-09-13
 */
@Service
@RequiredArgsConstructor
public class StatisticServiceImpl implements IStatisticService {

    private final StatisticMapper statisticMapper;

    @Override
    public DashboardStatisticsResp getDashboardStatistics()
    {
        // 时间边界统一在应用层按「自然日」切成左闭右开区间 [start, end)，
        // SQL 里只做裸列比较，保证 reservation_time / verify_time / create_time 上的索引可用
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        LocalDate dayAfterTomorrow = tomorrow.plusDays(1);
        // 近 30 天 = [today-29, today]，与前端展示的 30 个点一一对应，不扫描未来数据
        LocalDate trendStart = today.minusDays(29);

        Date todayStart = startOfDay(today);
        Date tomorrowStart = startOfDay(tomorrow);
        Date dayAfterTomorrowStart = startOfDay(dayAfterTomorrow);
        Date trendStartDay = startOfDay(trendStart);

        DashboardStatisticsResp resp = new DashboardStatisticsResp();
        resp.setTodayReservationCount(
                statisticMapper.selectTodayReservationCount(todayStart, tomorrowStart));
        resp.setTodayVerifyCount(
                statisticMapper.selectTodayVerifyCount(todayStart, tomorrowStart));
        resp.setTomorrowReservationCount(
                statisticMapper.selectTomorrowReservationCount(tomorrowStart, dayAfterTomorrowStart));
        resp.setTotalReservationCount(statisticMapper.selectTotalReservationCount());
        resp.setTodayNewUserCount(
                statisticMapper.selectTodayNewUserCount(todayStart, tomorrowStart));
        resp.setTotalUserCount(statisticMapper.selectTotalUserCount());

        resp.setLast30DaysReservationTrend(
                fillTrend(statisticMapper.selectLast30DaysReservationTrend(trendStartDay, tomorrowStart)));
        resp.setLast30DaysUserTrend(
                fillTrend(statisticMapper.selectLast30DaysUserTrend(trendStartDay, tomorrowStart)));

        return resp;
    }

    /**
     * 取某个自然日的零点时刻（左闭右开区间的边界）
     *
     * @param date 自然日
     * @return 该日 00:00:00 对应的 {@link Date}
     */
    private Date startOfDay(LocalDate date)
    {
        return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    /**
     * 把数据库返回的稀疏日期数据补齐为最近 30 天连续日期（缺失日期 count=0）
     */
    private List<DashboardTrendItemVO> fillTrend(List<DashboardTrendItemVO> dbList)
    {
        Map<String, Long> dateCountMap = dbList.stream()
                .collect(Collectors.toMap(DashboardTrendItemVO::getDate,
                        item -> item.getCount() != null ? item.getCount() : 0L,
                        (a, b) -> a));

        List<DashboardTrendItemVO> result = new ArrayList<>(30);
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;
        for (int i = 29; i >= 0; i--)
        {
            LocalDate date = today.minusDays(i);
            String dateStr = date.format(formatter);

            DashboardTrendItemVO item = new DashboardTrendItemVO();
            item.setDate(dateStr);
            item.setCount(dateCountMap.getOrDefault(dateStr, 0L));
            result.add(item);
        }
        return result;
    }
}
