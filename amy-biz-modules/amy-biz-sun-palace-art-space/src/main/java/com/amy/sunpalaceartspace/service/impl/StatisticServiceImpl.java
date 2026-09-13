package com.amy.sunpalaceartspace.service.impl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
        DashboardStatisticsResp resp = new DashboardStatisticsResp();
        resp.setTodayReservationCount(statisticMapper.selectTodayReservationCount());
        resp.setTodayVerifyCount(statisticMapper.selectTodayVerifyCount());
        resp.setTomorrowReservationCount(statisticMapper.selectTomorrowReservationCount());
        resp.setTotalReservationCount(statisticMapper.selectTotalReservationCount());
        resp.setTodayNewUserCount(statisticMapper.selectTodayNewUserCount());
        resp.setTotalUserCount(statisticMapper.selectTotalUserCount());

        resp.setLast30DaysReservationTrend(
                fillTrend(statisticMapper.selectLast30DaysReservationTrend()));
        resp.setLast30DaysUserTrend(
                fillTrend(statisticMapper.selectLast30DaysUserTrend()));

        return resp;
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
