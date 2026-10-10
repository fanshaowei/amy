package com.amy.sunpalaceartspace.service.impl;

import com.amy.common.core.exception.InnerAuthException;
import com.amy.common.core.exception.PreAuthorizeException;
import com.amy.common.core.exception.ServiceException;
import com.amy.common.core.utils.DateUtils;
import com.amy.common.mq.bo.AmyMqProducerBO;
import com.amy.common.mq.service.AmyMqProducer;
import com.amy.common.redis.service.RedisService;
import com.amy.sunpalaceartspace.domain.bo.ReservationOrderMqSendBO;
import com.amy.sunpalaceartspace.domain.criteria.ReservationOrderStatisticCriteria;
import com.amy.sunpalaceartspace.domain.entity.Projects;
import com.amy.sunpalaceartspace.domain.entity.ReservationMessage;
import com.amy.sunpalaceartspace.domain.entity.ReservationOrder;
import com.amy.sunpalaceartspace.domain.entity.ReservationUser;
import com.amy.sunpalaceartspace.domain.req.ReservationOrderReq;
import com.amy.sunpalaceartspace.domain.req.WechatApiAuthLoginReq;
import com.amy.sunpalaceartspace.domain.resp.mina.MinaHomePageProjectResp;
import com.amy.sunpalaceartspace.domain.resp.mina.MinaProjectReservationInfoResp;
import com.amy.sunpalaceartspace.domain.resp.mina.MinaReservationUserInfoResp;
import com.amy.sunpalaceartspace.domain.vo.ReservationOrderStatisticByProjectVO;
import com.amy.sunpalaceartspace.domain.vo.ReservationOrderStatisticByTimeVO;
import com.amy.sunpalaceartspace.domain.vo.ReservationOrderVO;
import com.amy.sunpalaceartspace.enums.ReservationMessageStatus;
import com.amy.sunpalaceartspace.enums.ReservationOrderStatus;
import com.amy.sunpalaceartspace.enums.ReservationStatusEnum;
import com.amy.sunpalaceartspace.service.IProjectsService;
import com.amy.sunpalaceartspace.service.IReservationMessageService;
import com.amy.sunpalaceartspace.service.IReservationOrderService;
import com.amy.sunpalaceartspace.service.IReservationUserService;
import com.amy.sunpalaceartspace.utils.ReservationUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.amy.sunpalaceartspace.constant.SpasConstant.*;

/**
 * @ClassName MinaAppService
 * @Description TODO
 * @Author fantasyfan
 * @Date 2026-09-19 12:00
 */
@Slf4j
@AllArgsConstructor
@Service
public class MinaAppService {
    private final IProjectsService projectsService;
    private final IReservationOrderService reservationOrderService;
    private final IReservationUserService reservationUserService;
    private final RedisService redisService;
    private final AmyMqProducer<AmyMqProducerBO> amyMqProducer;
    private final DefaultRedisScript<Long> reserveScript;
    private final ReservationUtil reservationUtil;
    private final IReservationMessageService reservationMessageService;
    private final MinaAppLoginService minaAppLoginService;

    public void miniAppLogin(WechatApiAuthLoginReq req) throws WxErrorException {
        String token = minaAppLoginService.generalApiAuthToken(req);
    }

    public void sendReservationOrderMq(ReservationOrderMqSendBO bo) {
        amyMqProducer.asyncSend(MQ_DESTINATE,
                ReservationOrderMqSendBO.builder()
                        .userId(1000L)
                        .projectId(1L)
                        .reservationTime(DateUtils.parseDateToStr(DateUtils.YYYYMMDDHHMM, new Date()))
                        .build(),
                (sendResult) -> {
                    log.info("sendReservationOrderMq: {} sendResult: {}", bo, sendResult);
                });
    }

    public IPage<MinaHomePageProjectResp> extractHomePageProjectInfo(Page<Projects> page) {
        Projects criteria = new Projects();
        criteria.setStatus("0");
        IPage<Projects> projectsIPage = projectsService.selectProjectsList(page, criteria);

        IPage<MinaHomePageProjectResp> pageResp = new Page();
        List<MinaHomePageProjectResp> list = projectsIPage.getRecords().stream().map(project -> {
            List<String> reservationDates = reservationUtil.extractReservationDatesFromCurrent(project.getAdvanceReservationDays());
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
        List<String> result = reservationUtil.extractReservationTimes(startTime, endTime, interval);
        return result.size() * count;
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
        List<String> reservationTimes = reservationUtil.extractReservationTimes(
                project.getReservationStartTime(),
                project.getReservationEndTime(),
                project.getReservationIntervalSecond()
        );
        // 获取预约日期
        List<String> reservationDatas = reservationUtil.extractReservationDatesFromCurrent(project.getAdvanceReservationDays());

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
        ReservationUser verifyUser = getUserByToken(token);
        ReservationOrder reservationOrder = reservationOrderService.selectReservationOrderByNum(orderNum);

        //校验订单是否待核销状态
        Integer reservationStatus = reservationOrder.getReservationStatus();
        if(!Objects.equals(ReservationOrderStatus.WAIT_VERIFY.getStatus(), reservationStatus)) {
            ReservationOrderStatus statusEnum = ReservationOrderStatus.fromCode(reservationStatus);
            throw new ServiceException("该订单" + statusEnum.getName());
        }

        ReservationOrderReq req = new ReservationOrderReq();
        BeanUtils.copyProperties(reservationOrder, req);

        req.setReservationStatus(ReservationOrderStatus.COMPLETED.getStatus());
        reservationOrder.setVerifyBy(verifyUser.getName());
        reservationOrder.setVerifyTime(new Date());
        boolean verifyFlag = reservationOrderService.updateReservationOrder(req);
        log.info("verifyReservationOrder: {} result: {}", orderNum, verifyFlag);
        return verifyFlag;
    }

    @Transactional
    public boolean submitReservationOrder(String token, ReservationOrderReq req) {
        ReservationUser user = getUserByToken(token);
        req.setReservationUserId(user.getReservationUserId());

        // 验证预约会员是否存在
        boolean userExists = reservationUserService.checkUserExistById(req.getReservationUserId());
        if(!userExists) {
            throw new ServiceException("预约会员不存在，请先注册");
        }
        // 验证项目是否存在
        boolean projectExist = projectsService.checkProjectExistById(req.getProjectId());
        if(!projectExist) {
            throw new ServiceException("预约项目不存在，无法进行预约");
        }
        //验证是否同一时间预约过
        LambdaQueryWrapper<ReservationOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ReservationOrder::getProjectId, req.getProjectId())
                .and(wrapper -> wrapper
                        .eq(ReservationOrder::getReservationUserId, req.getReservationUserId())
                        .or()
                        .eq(ReservationOrder::getIdNum, req.getIdNum())
                )
                .eq(ReservationOrder::getReservationTime, req.getReservationTime())
                .eq(ReservationOrder::getReservationStatus, ReservationOrderStatus.WAIT_VERIFY.getStatus());
        boolean exists = reservationOrderService.count(queryWrapper) > 0;
        if (exists) {
            throw new ServiceException("会员在同一时间已经存在预约订单，无法重复预约");
        }

        // 进入队列
        checkStockAndReservation(req.getProjectId(), req.getReservationUserId(), req.getReservationTime());
        return reservationOrderService.saveReservationOrder(req);
    }

    public ReservationUser getUserByToken(String token) {
        Object openId = redisService.getCacheObject(String.format(WX_API_TOKEN_OPEN_ID, token));
        if (null == openId) {
            throw new InnerAuthException("用户未登录或登录已过期，请重新登录");
        }

        return reservationUserService.selectUserInfoByOpenId((String) openId);
    }

    private void checkStockAndReservation(Long projectId, Long userId, Date reservationDate) {
        String reservationDateStr = DateUtils.parseDateToStr(DateUtils.YYYYMMDDHHMM, reservationDate);
        String reservationNum = reservationUtil.generateReservationNum(new Date());
        String stockKey = String.format(RESERVATION_STOCK_KEY_PREFIX, projectId, reservationDateStr);
        String userKey = String.format(RESERVATION_USER_KEY_PREFIX, projectId, reservationDateStr, userId);
        Object rawResult = null;
        try {
            rawResult = redisService.redisTemplate.execute(reserveScript, List.of(stockKey, userKey), reservationNum, "3600");
        } catch (Exception e) {
            log.error("执行redis预约脚本失败, {}", e.getMessage());
            throw new ServiceException("系统维护中,请联系管理员.");
        }
        Long result = (rawResult instanceof Number) ? ((Number) rawResult).longValue() : null;
        if (result == null) {
            log.error("reservationId: {} Redis script run fail.", reservationNum);
            throw new ServiceException("系统预约失败");
        }

        // 活动不存在
        if (result == -1) {
            log.error("projectId: {} reservationId: {} 项目不存在.", projectId, reservationNum);
            throw new ServiceException("预约项目不存在");
        }

        // 名额已满
        if (result == 0) {
            log.error("projectId: {} reservationId: {} 预约时间段: {} 预约名额已满.", projectId, reservationNum, reservationDateStr);
            throw new ServiceException("预约名额已满");
        }

        // 重复预约
        if (result == 2) {
            log.error("projectId: {} reservationId: {} 预约时间段: {} 用户: {} 已预约.", projectId, reservationNum, reservationDateStr, userId);
            throw new ServiceException("用户已预约");
        }

        // 抢到资格
        try {
            // 先记录消息任务
            ReservationMessage reservationMessage = new ReservationMessage();
            reservationMessage.setReservationId(reservationNum);
            reservationMessage.setReservationTime(reservationNum);
            reservationMessage.setUserId(userId);
            reservationMessage.setRetryCount(0);
            reservationMessage.setStatus(ReservationMessageStatus.WAITING.getCode());
            boolean success = reservationMessageService.saveReservationMessage(reservationMessage);
            if (success) {
                log.info("创建预约消息 userId:{} reservationNum: {},reservationNum: {}", userId, reservationNum, reservationNum);
                // 发送RocketMQ
                SendResult sendResult = amyMqProducer.syncSend(MQ_DESTINATE,
                        ReservationOrderMqSendBO.builder()
                                .reservationMessageId(reservationMessage.getReservationMessageId())
                                .userId(userId)
                                .projectId(projectId)
                                .reservationNum(reservationNum)
                                .reservationTime(reservationDateStr)
                                .build()
                );
                if(SendStatus.SEND_OK.equals(sendResult.getSendStatus())) {
                    boolean update = reservationMessageService.updateStatus(reservationMessage.getReservationMessageId(), ReservationMessageStatus.SENT);
                    log.info("更新预约消息userId:{} reservationNum: {},reservationNum: {}", userId, reservationNum, reservationNum);
                }
            }
        } catch (Exception e) {
            /*
             * 注意：这里不能简单认为MQ一定没有收到。
             * 所以不要立即Redis +1。
             * 保留WAITING状态，后面的补偿任务继续发送。
             */
            /*return new ReservationResponse(
                202,
                reservationId,
                "WAITING",
                "预约申请处理中"
            );*/
        }
    }

}
