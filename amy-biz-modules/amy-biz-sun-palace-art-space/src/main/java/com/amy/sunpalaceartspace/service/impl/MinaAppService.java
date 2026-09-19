package com.amy.sunpalaceartspace.service.impl;

import com.amy.common.core.utils.DateUtils;
import com.amy.sunpalaceartspace.domain.criteria.ReservationOrderStatisticCriteria;
import com.amy.sunpalaceartspace.domain.entity.Projects;
import com.amy.sunpalaceartspace.domain.resp.mina.MinaHomePageProjectResp;
import com.amy.sunpalaceartspace.domain.vo.ReservationOrderStatisticByProjectVO;
import com.amy.sunpalaceartspace.enums.ReservationStatusEnum;
import com.amy.sunpalaceartspace.service.IProjectsService;
import com.amy.sunpalaceartspace.service.IReservationOrderService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.AllArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @ClassName MinaAppService
 * @Description TODO
 * @Author fantasyfan
 * @Date 2026-09-19 12:00
 */
@AllArgsConstructor
@Service
public class MinaAppService {
    private final IProjectsService projectsService;
    private final IReservationOrderService reservationOrderService;

    public IPage<MinaHomePageProjectResp> extractProjectInfo(Page<Projects> page) {
        Projects criteria = new Projects();
        criteria.setStatus("0");
        IPage<Projects> projectsIPage = projectsService.selectProjectsList(page, criteria);

        IPage<MinaHomePageProjectResp> pageResp = new Page();
        List<MinaHomePageProjectResp> list = projectsIPage.getRecords().stream().map(project -> {
            List<String> reservationDates = calculateReservationData(project.getAdvanceReservationDays());
            // 可预约总数
            Integer totalReservationCount = calculateTotalReservationCount(
                    project.getReservationStartTime(),
                    project.getReservationEndTime(),
                    project.getReservationIntervalSecond(),
                    project.getReservationCount());
            //剩余可预约次数 = 预约总次数 - 已预约次数
            Integer reservationCount = calculateDateReservationCount(project.getProjectId(), project.getAdvanceReservationDays());
            Integer remainingReservationCount = totalReservationCount - reservationCount;
            Integer reservationStatus = calculateReservationStatus(totalReservationCount, reservationCount);

            MinaHomePageProjectResp resp = MinaHomePageProjectResp.builder()
                    .projectId(project.getProjectId())
                    .projectName(project.getProjectName())
                    .coverArtUrl(project.getCoverArtUrl())
                    .reservationStartDate(reservationDates.get(0))
                    .reservationEndDate(reservationDates.get(1))
                    .reservationStartTime(project.getReservationStartTime())
                    .reservationEndTime(project.getReservationEndTime())
                    .totalReservationCount(totalReservationCount)
                    .remainingReservationCount(remainingReservationCount)
                    .reservationStatus(reservationStatus)
                    .cutOffTime(project.getCutOffTime())
                    .reservationStaySecond(project.getReservationStaySecond())
                    .build();

            BeanUtils.copyProperties(project, resp);
            return resp;
        }).toList();
        pageResp.setRecords(list);
        BeanUtils.copyProperties(projectsIPage, pageResp);

        return pageResp;
    }

    private List<String> calculateReservationData(Integer dateInterval) {
        Date dateStart = DateUtils.addDays(DateUtils.getNowDate(), 1);
        Date dateEnd = DateUtils.addDays(DateUtils.getNowDate(), dateInterval);
        return List.of(
                DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, dateStart),
                DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, dateEnd)
        );
    }

    private Integer calculateDateReservationCount(Long projectId, Integer dateInterval) {
        ReservationOrderStatisticCriteria criteria = ReservationOrderStatisticCriteria.builder()
                .projectId(projectId)
                .reservationTimeStart(DateUtils.addDays(DateUtils.parseDate(DateUtils.getDate() + "00:00:00"), 1))
                .reservationTimeEnd(DateUtils.addDays(DateUtils.parseDate(DateUtils.getDate() + "23:59:59"), dateInterval))
                .build();
        List<ReservationOrderStatisticByProjectVO> vos = reservationOrderService.selectReservationOrderStatisticByProject(criteria);
        return vos.getFirst().getCount();
    }

    private Integer calculateTotalReservationCount(String startTime, String endTime, Integer interval, Integer count){
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DateUtils.HH_MM_SS);
        LocalTime start = LocalTime.parse(startTime, formatter);
        LocalTime end = LocalTime.parse(endTime, formatter);

        List<String> result = new ArrayList<>();
        while (start.isBefore(end)) {
            result.add(start.format(formatter));
            start = start.plusMinutes(interval / 60);
        }

        return result.size() * count;
    }

    private Integer calculateReservationStatus(Integer totalReservationCount, Integer reservationCount) {
        Integer reservationPercent = 0;
        if(reservationCount > totalReservationCount) return ReservationStatusEnum.FILLED.getValue();

        reservationPercent = Math.divideExact(reservationCount, totalReservationCount) * 100;

        return reservationPercent > 80 ? (reservationPercent >= 100 ? ReservationStatusEnum.FILLED.getValue() : ReservationStatusEnum.TIGHT.getValue() )
                : ReservationStatusEnum.AMPLE.getValue();
    }
}
