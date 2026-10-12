package com.amy.sunpalaceartspace.service.impl;

import com.amy.common.core.exception.ServiceException;
import com.amy.common.core.utils.DateUtils;
import com.amy.common.mq.bo.AmyMqProducerBO;
import com.amy.common.mq.service.AmyMqProducer;
import com.amy.common.redis.service.RedisService;
import com.amy.sunpalaceartspace.domain.bo.ReservationOrderMqSendBO;
import com.amy.sunpalaceartspace.domain.entity.ReservationMessage;
import com.amy.sunpalaceartspace.domain.entity.ReservationUser;
import com.amy.sunpalaceartspace.domain.req.ReservationOrderReq;
import com.amy.sunpalaceartspace.domain.req.SubmitReservationReq;
import com.amy.sunpalaceartspace.enums.ReservationMessageStatus;
import com.amy.sunpalaceartspace.enums.ReservationOrderStatus;
import com.amy.sunpalaceartspace.service.*;
import com.amy.sunpalaceartspace.utils.ReservationUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendStatus;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static com.amy.sunpalaceartspace.constant.SpasConstant.*;
import static org.apache.rocketmq.client.producer.SendStatus.SEND_OK;

/**
 * @ClassName SubmitReservationOrderService
 * @Description TODO
 * @Author fantasyfan
 * @Date 2026-10-13 02:13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubmitReservationOrderService {
    private final ReservationUtil reservationUtil;
    private final IReservationUserService reservationUserService;
    private final IProjectsService projectsService;
    private final ObjectMapper objectMapper;
    private final IReservationMessageService reservationMessageService;
    private final AmyMqProducer<AmyMqProducerBO> amyMqProducer;
    private final IReservationOrderService reservationOrderService;
    private final RedisService redisService;
    private final IReservationStockService reservationStockService;
    private final DefaultRedisScript<Long> reserveScript;
    private final MinaAppLoginService minaAppLoginService;

    @Transactional
    public String submitReservationOrder(String token, SubmitReservationReq req) {
        ReservationUser user = minaAppLoginService.getUserByToken(token);
        // 预约前校验
        reservationPreCheck(req, user);

        // 预约单号只生成一次，贯穿 Lua 抢号、消息、Redis 一人一次标记，保证全链路一致
        String reservationNum = reservationUtil.generateReservationNum(req.getReservationTime());

        // 削峰，将预订逻辑交由消费者
        sendReservationOrderMq(req, user, reservationNum);

        return reservationNum;
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

    /**
     * 由mq消费者执行订单预约逻辑
     *
     * @param bo
     */
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
