package com.amy.sunpalaceartspace.service.impl;

import com.amy.common.core.exception.InnerAuthException;
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
import com.amy.sunpalaceartspace.domain.req.SubmitReservationReq;
import com.amy.sunpalaceartspace.domain.resp.mina.MinaHomePageProjectResp;
import com.amy.sunpalaceartspace.domain.resp.mina.MinaProjectReservationInfoResp;
import com.amy.sunpalaceartspace.domain.resp.mina.MinaReservationUserInfoResp;
import com.amy.sunpalaceartspace.domain.vo.ReservationOrderStatisticByProjectVO;
import com.amy.sunpalaceartspace.domain.vo.ReservationOrderStatisticByTimeVO;
import com.amy.sunpalaceartspace.enums.ReservationMessageStatus;
import com.amy.sunpalaceartspace.enums.ReservationOrderStatus;
import com.amy.sunpalaceartspace.enums.ReservationStatusEnum;
import com.amy.sunpalaceartspace.service.*;
import com.amy.sunpalaceartspace.utils.ReservationUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendStatus;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static com.amy.sunpalaceartspace.constant.SpasConstant.*;
import static org.apache.rocketmq.client.producer.SendStatus.SEND_OK;

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
    private final IReservationStockService reservationStockService;
    private final ObjectMapper objectMapper;

    public IPage<MinaHomePageProjectResp> extractHomePageProjectInfo(Page<Projects> page) {
        Projects criteria = new Projects();
        criteria.setStatus("0");
        IPage<Projects> projectsIPage = projectsService.selectProjectsList(page, criteria);

        IPage<MinaHomePageProjectResp> pageResp = new Page<>();
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
        BeanUtils.copyProperties(projectsIPage, pageResp);
        pageResp.setRecords(list);

        return pageResp;
    }

    private Integer calculateDateReservationCount(Long projectId, Integer dayInterval) {
        ReservationOrderStatisticCriteria criteria = ReservationOrderStatisticCriteria.builder()
                .projectId(projectId)
                .reservationTimeStart(DateUtils.addDays(DateUtils.parseDate(DateUtils.getDate() + " 00:00:00"), 1))
                .reservationTimeEnd(DateUtils.addDays(DateUtils.parseDate(DateUtils.getDate() + " 23:59:59"), dayInterval))
                .build();
        List<ReservationOrderStatisticByProjectVO> vos = reservationOrderService.selectReservationOrderStatisticByProject(criteria);
        if(vos.isEmpty()) return 0;
        else
            return Optional.ofNullable(vos.getFirst()).map(ReservationOrderStatisticByProjectVO::getCount).orElse(0);
    }

    private Integer calculateTotalReservationCount(String startTime, String endTime, Integer interval, Integer count){
        List<String> result = reservationUtil.extractReservationTimes(startTime, endTime, interval);
        return result.size() * count;
    }

    private Integer calculateReservationStatus(Integer totalReservationCount, Integer reservationCount) {
        Integer reservationPercent = 0;
        if(reservationCount >= totalReservationCount) return ReservationStatusEnum.FILLED.getValue();

        reservationPercent = Math.divideExact(reservationCount, totalReservationCount) * 100;

        return reservationPercent > 80 ?
                (reservationPercent >= 100 ? ReservationStatusEnum.FILLED.getValue() : ReservationStatusEnum.TIGHT.getValue() ) :
                ReservationStatusEnum.AMPLE.getValue();
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
        Object openId = redisService.getCacheObject(String.format(CASH_WX_API_TOKEN_OPEN_ID, token));
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

    public ReservationUser getUserByToken(String token) {
        Object openId = redisService.getCacheObject(String.format(CASH_WX_API_TOKEN_OPEN_ID, token));
        if (null == openId) {
            throw new InnerAuthException("用户未登录或登录已过期，请重新登录");
        }

        return reservationUserService.selectUserInfoByOpenId((String) openId);
    }

    @Transactional
    public String submitReservationOrder(String token, SubmitReservationReq req) {
        ReservationUser user = getUserByToken(token);;
        // 预约前校验
        reservationPreCheck(req, user);

        // 预约单号只生成一次，贯穿 Lua 抢号、消息、Redis 一人一次标记，保证全链路一致
        String reservationNum = reservationUtil.generateReservationNum(req.getReservationTime());

        // 削峰，将预订逻辑交由消费者
        sendReservationOrderMq(req, user, reservationNum);

        return reservationNum;
    }

    @Transactional
    public void sendReservationOrderMq(SubmitReservationReq req, ReservationUser user, String reservationNum) {
        // 构建要发送mq的消息体
        ReservationOrderMqSendBO bo = ReservationOrderMqSendBO.builder().build();
        BeanUtils.copyProperties(req, bo);
        bo.setReservationNum(reservationNum);
        bo.setReservationUserId(user.getReservationUserId());

        // 保存mq发送记录
        ReservationMessage reservationMessage = new ReservationMessage();
        try {
            reservationMessage.setMsgContent(objectMapper.writeValueAsString(bo));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        reservationMessage.setRetryCount(0);
        reservationMessage.setStatus(ReservationMessageStatus.WAITING.getCode());
        boolean success = reservationMessageService.saveReservationMessage(reservationMessage);
        if (!success) {
            log.error("userId:{} sendReservationOrderMq: {} saveReservationMessage fail.", user.getReservationUserId(), reservationNum);
            throw new ServiceException("预约失败，请稍后重试");
        }

        // 发送消息并更新发送状态
        amyMqProducer.asyncSend(MQ_DESTINATE, bo,
            (sendResult) -> {
                log.info("sendReservationOrderMq: {} sendResult: {}", bo, sendResult);
                SendStatus sendStatus = sendResult.getSendStatus();
                if(SEND_OK.equals(sendStatus)) {
                    log.info("userId:{} sendReservationOrderMq: {} send success.", user.getReservationUserId(), reservationNum);
                    reservationMessageService.updateStatus(reservationMessage.getReservationMessageId(), ReservationMessageStatus.SENT);
                } else {
                    log.error("userId:{} sendReservationOrderMq: {} send fail, status: {}", user.getReservationUserId(), reservationNum, sendStatus);
                    reservationMessageService.updateStatus(reservationMessage.getReservationMessageId(), ReservationMessageStatus.FAIL);
                }
            });
    }

    public void reservateOrder(ReservationOrderMqSendBO bo) {
        String reservationTimeStr = DateUtils.parseDateToStr(DateUtils.YYYYMMDDHHMM, bo.getReservationTime());
        String stockKey = String.format(RESERVATION_STOCK_KEY_PREFIX, bo.getProjectId(), reservationTimeStr);
        String userKey = String.format(RESERVATION_USER_DONE_KEY, bo.getReservationUserId());
        String reservationNum = bo.getReservationNum();

        // 原子校验 + 扣减库存（Lua），失败会直接抛异常
        checkStockAndReservation(bo.getProjectId(), bo.getReservationUserId(), stockKey, userKey, reservationTimeStr, reservationNum);

        // 落单（与 Redis 扣减解耦：若落单失败则回滚 Redis，避免永久占名额/永久锁用户）
        boolean saved;
        try {
            ReservationOrderReq req = new ReservationOrderReq();
            BeanUtils.copyProperties(bo, req);
            saved = reservationOrderService.saveReservationOrder(req);
        } catch (Exception e) {
            rollbackReservation(stockKey, userKey);
            throw new ServiceException("预约失败，请稍后重试");
        }
        if (!saved) {
            rollbackReservation(stockKey, userKey);
            throw new ServiceException("预约失败，请稍后重试");
        }

        // 成功后：累加已预约数（用于库存统计）。
        // 说明：不再永久标记 userKey（旧"一人一次"语义已废弃）；userKey 仅作 Lua 内短期防并发重复，600s 后自动失效。
        reservationStockService.incrementReservedCount(bo.getProjectId(), reservationTimeStr);
    }

    private void reservationPreCheck(SubmitReservationReq req, ReservationUser user) {
        // 验证预约会员是否存在
        boolean userExists = reservationUserService.checkUserExistById(user.getReservationUserId());
        if(!userExists) {
            throw new ServiceException("预约会员不存在，请先注册");
        }
        // 验证项目是否存在
        boolean projectExist = projectsService.checkProjectExistById(req.getProjectId());
        if(!projectExist) {
            throw new ServiceException("预约项目不存在，无法进行预约");
        }
        // 预约频次校验（微信号维度）：1月最多2次、取消满2次当月禁约、3天内不可重复预约
        validateReservationFrequency(user.getReservationUserId(), req.getIdNum());
    }

    /**
     * 预约频次校验（微信号=会员ID 维度；同时纳入证件号，防同一人换微信号绕过）：
     * 1) 3 天内已预约过（过期/取消/完成/待核销 任一状态均计数）→ 不可重复预约（3 天内仅能约 1 次）；
     * 2) 1 个月内预约次数（上述 4 种状态均计数）达 2 次 → 当月次数用尽；
     * 3) 1 个月内取消达 2 次 → 当月禁止再预约。
     */
    private void validateReservationFrequency(Long userId, String idNum) {
        Date now = new Date();
        // 计数状态：待核销(0)/已完成(1)/已过期(2)/已取消(3) —— 均算一次预约
        List<Integer> countingStatuses = Arrays.asList(
                ReservationOrderStatus.WAIT_VERIFY.getStatus(),
                ReservationOrderStatus.COMPLETED.getStatus(),
                ReservationOrderStatus.EXPIRED.getStatus(),
                ReservationOrderStatus.CANCELED.getStatus());

        // 3 天内已预约过 → 不可重复预约
        if (reservationOrderService.countUserOrders(userId, idNum, countingStatuses, DateUtils.addDays(now, -3)) >= 1) {
            throw new ServiceException("您3天内已预约过，暂不可重复预约");
        }

        // 1 个月内预约次数达 2 次 → 当月次数用尽
        if (reservationOrderService.countUserOrders(userId, idNum, countingStatuses, DateUtils.addMonths(now, -1)) >= 2) {
            throw new ServiceException("您本月预约次数已用完（每月最多2次）");
        }

        // 1 个月内取消满 2 次 → 当月禁止再预约
        if (reservationOrderService.countUserOrders(userId, idNum,
                Collections.singletonList(ReservationOrderStatus.CANCELED.getStatus()),
                DateUtils.addMonths(now, -1)) >= 2) {
            throw new ServiceException("您本月已取消2次，暂不可预约");
        }
    }

    private void checkStockAndReservation(Long projectId, Long userId, String stockKey, String userKey, String reservationTimeStr, String reservationNum) {
        Object rawResult = null;
        try {
            rawResult = redisService.redisTemplate.execute(reserveScript, List.of(stockKey, userKey), reservationNum, "600");
        } catch (Exception e) {
            log.error("执行redis预约脚本失败, {}", e.getMessage());
            throw new ServiceException("系统维护中,请联系管理员.");
        }
        Long result = (rawResult instanceof Number) ? ((Number) rawResult).longValue() : null;
        if (result == null) {
            log.error("reservationId: {} Redis script run fail.", reservationNum);
            throw new ServiceException("系统预约失败");
        }

        // 时段尚未放号 / 不可预约
        if (result == -1) {
            log.error("projectId: {} reservationTime: {} 时段尚未放号或不可预约.", projectId, reservationTimeStr);
            throw new ServiceException("该时段尚未放号，暂不可预约");
        }

        // 名额已满
        if (result == 0) {
            log.error("projectId: {} reservationTime: {} 预约名额已满.", projectId, reservationTimeStr);
            throw new ServiceException("预约名额已满");
        }

        // 重复预约
        if (result == 2) {
            log.error("projectId: {} reservationTime: {} 用户: {} 已预约.", projectId, reservationTimeStr, userId);
            throw new ServiceException("您已预约，不可重复预约");
        }
    }

    private void rollbackReservation(String stockKey, String userKey) {
        try {
            redisService.redisTemplate.opsForValue().increment(stockKey, 1);
            redisService.deleteObject(userKey);
        } catch (Exception e) {
            log.error("rollbackReservation failed, stockKey:{}, userKey:{}", stockKey, userKey, e);
        }
    }

}
