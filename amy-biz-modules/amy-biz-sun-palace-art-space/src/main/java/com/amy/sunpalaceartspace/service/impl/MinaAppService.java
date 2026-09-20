package com.amy.sunpalaceartspace.service.impl;

import com.amy.common.core.exception.PreAuthorizeException;
import com.amy.common.core.exception.ServiceException;
import com.amy.common.core.utils.DateUtils;
import com.amy.common.redis.service.RedisService;
import com.amy.sunpalaceartspace.domain.criteria.ReservationOrderStatisticCriteria;
import com.amy.sunpalaceartspace.domain.entity.Projects;
import com.amy.sunpalaceartspace.domain.entity.ReservationOrder;
import com.amy.sunpalaceartspace.domain.entity.ReservationUser;
import com.amy.sunpalaceartspace.domain.req.ReservationOrderReq;
import com.amy.sunpalaceartspace.domain.resp.mina.MinaHomePageProjectResp;
import com.amy.sunpalaceartspace.domain.resp.mina.MinaProjectReservationInfoResp;
import com.amy.sunpalaceartspace.domain.resp.mina.MinaReservationUserInfoResp;
import com.amy.sunpalaceartspace.domain.vo.ReservationOrderStatisticByProjectVO;
import com.amy.sunpalaceartspace.domain.vo.ReservationOrderStatisticByTimeVO;
import com.amy.sunpalaceartspace.domain.vo.ReservationOrderVO;
import com.amy.sunpalaceartspace.enums.ReservationOrderStatus;
import com.amy.sunpalaceartspace.enums.ReservationStatusEnum;
import com.amy.sunpalaceartspace.service.IProjectsService;
import com.amy.sunpalaceartspace.service.IReservationOrderService;
import com.amy.sunpalaceartspace.service.IReservationUserService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.AllArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import static com.amy.sunpalaceartspace.constant.SpasConstant.WX_API_TOKEN_OPEN_ID;

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
    private final IReservationUserService reservationUserService;
    private final RedisService redisService;

    public IPage<MinaHomePageProjectResp> extractHomePageProjectInfo(Page<Projects> page) {
        Projects criteria = new Projects();
        criteria.setStatus("0");
        IPage<Projects> projectsIPage = projectsService.selectProjectsList(page, criteria);

        IPage<MinaHomePageProjectResp> pageResp = new Page();
        List<MinaHomePageProjectResp> list = projectsIPage.getRecords().stream().map(project -> {
            List<String> reservationDates = calculateReservationDates(project.getAdvanceReservationDays());
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
                    .reservationStartDate(reservationDates.getFirst())
                    .reservationEndDate(reservationDates.getLast())
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

    private List<String> calculateReservationDates(Integer dateInterval) {
        List<String> dates = new LinkedList<>();

        Date dateStart = DateUtils.addDays(DateUtils.getNowDate(), 1);
        Date dateEnd = DateUtils.addDays(DateUtils.getNowDate(), dateInterval);

        while(!dateStart.after(dateEnd)) {
            dates.add(DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, dateStart));
            dateStart = DateUtils.addDays(dateStart, 1);
        }

        return dates;
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
        List<String> result = calculateReservationTimes(startTime, endTime, interval, count);
        return result.size() * count;
    }

    private List<String> calculateReservationTimes(String startTime, String endTime, Integer interval, Integer count){
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DateUtils.HH_MM_SS);
        LocalTime start = LocalTime.parse(startTime, formatter);
        LocalTime end = LocalTime.parse(endTime, formatter);

        List<String> result = new ArrayList<>();
        while (start.isBefore(end)) {
            result.add(start.format(formatter));
            start = start.plusMinutes(interval / 60);
        }

        return result;
    }

    private Integer calculateReservationStatus(Integer totalReservationCount, Integer reservationCount) {
        Integer reservationPercent = 0;
        if(reservationCount > totalReservationCount) return ReservationStatusEnum.FILLED.getValue();

        reservationPercent = Math.divideExact(reservationCount, totalReservationCount) * 100;

        return reservationPercent > 80 ? (reservationPercent >= 100 ? ReservationStatusEnum.FILLED.getValue() : ReservationStatusEnum.TIGHT.getValue() )
                : ReservationStatusEnum.AMPLE.getValue();
    }

    public MinaProjectReservationInfoResp extractProjectReservationInfo(Long projectId) {
        Projects project = projectsService.getBaseMapper().selectById(projectId);

        MinaProjectReservationInfoResp resp = MinaProjectReservationInfoResp.builder()
                .reservationNotes(project.getReservationNotes())
                .reservationDetails(new LinkedList<>())
                .build();

        // 获取预约时间段
        List<String> reservationTimes = calculateReservationTimes(
                project.getReservationStartTime(),
                project.getReservationEndTime(),
                project.getReservationIntervalSecond(),
                project.getReservationCount()
        );
        // 获取预约日期
        List<String> reservationDatas = calculateReservationDates(project.getAdvanceReservationDays());

        reservationDatas.forEach(reservationDate -> {
            // 构造返回的结果
            MinaProjectReservationInfoResp.ReservationDetails reservationDetails = MinaProjectReservationInfoResp.ReservationDetails
                    .builder()
                    .reservationDate(reservationDate)
                    .reservationTimeDetails(new LinkedList<>())
                    .build();

            // 查询对应日期的各个时间段已经预约数量
            ReservationOrderStatisticCriteria criteria = ReservationOrderStatisticCriteria.builder()
                    .projectId(projectId)
                    .reservationTimeStart(DateUtils.parseDate(reservationDate + "00:00:00"))
                    .reservationTimeEnd(DateUtils.parseDate(reservationDate + "23:59:59"))
                    .build();
            List<ReservationOrderStatisticByTimeVO> statisticByTimeVOS = reservationOrderService.selectReservationOrderStatisticByTime(criteria);

            reservationTimes.forEach(reservationTime -> {
                statisticByTimeVOS.stream()
                        // 匹配对就时间段的统计结果
                        .filter(statisticByTimeVO -> reservationTime.equals(statisticByTimeVO.getReservationTime()))
                        .findFirst()
                        .ifPresentOrElse(
                                statisticByTimeVO -> {
                                    // 设置匹配到的统计结果
                                    MinaProjectReservationInfoResp.ReservationTimeDetails reservationTimeDetails = MinaProjectReservationInfoResp.ReservationTimeDetails
                                            .builder()
                                            .reservationTime(reservationTime)
                                            .remainingReservationCount(project.getReservationCount() - statisticByTimeVO.getCount())
                                            .reservationStatus(calculateReservationStatus(project.getReservationCount(), statisticByTimeVO.getCount()))
                                            .build();
                                    reservationDetails.getReservationTimeDetails().add(reservationTimeDetails);
                                },
                                () -> {
                                    // 没有匹配到说明该时段没有人预约
                                    MinaProjectReservationInfoResp.ReservationTimeDetails reservationTimeDetails = MinaProjectReservationInfoResp.ReservationTimeDetails
                                            .builder()
                                            .reservationTime(reservationTime)
                                            .remainingReservationCount(project.getReservationCount())
                                            .reservationStatus(ReservationStatusEnum.AMPLE.getValue())
                                            .build();
                                    reservationDetails.getReservationTimeDetails().add(reservationTimeDetails);
                                }
                        );
            });

            resp.getReservationDetails().add(reservationDetails);
        });
        return resp;
    }

    public MinaReservationUserInfoResp extractUserInfo(String token) {
        Object openId = redisService.getCacheObject(String.format(WX_API_TOKEN_OPEN_ID, token));
        ReservationUser userInfo = reservationUserService.selectUserInfoByOpenId((String) openId);
        return MinaReservationUserInfoResp.builder()
                .reservationUserId(userInfo.getReservationUserId())
                .type(userInfo.getType())
                .nickname(userInfo.getNickname())
                .phone(userInfo.getPhone())
                .avatarImgUrl(userInfo.getAvatarImgUrl())
                .build();
    }

    @Transactional
    public boolean verifyReservationOrder(String token, String orderNum) {
        Object openId = redisService.getCacheObject(String.format(WX_API_TOKEN_OPEN_ID, token));
        if (null == openId) {
           throw new ServiceException("用户未登录或登录已过期，请重新登录");
        }
        ReservationUser verifyUser = reservationUserService.selectUserInfoByOpenId((String) openId);
        ReservationOrder reservationOrder = reservationOrderService.selectReservationOrderByNum(orderNum);

        ReservationOrderReq req = new ReservationOrderReq();
        BeanUtils.copyProperties(reservationOrder, req);

        req.setReservationStatus(ReservationOrderStatus.COMPLETED.getStatus());
        reservationOrder.setVerifyBy(verifyUser.getName());
        reservationOrder.setVerifyTime(new Date());
        return reservationOrderService.updateReservationOrder(req);
    }
}
