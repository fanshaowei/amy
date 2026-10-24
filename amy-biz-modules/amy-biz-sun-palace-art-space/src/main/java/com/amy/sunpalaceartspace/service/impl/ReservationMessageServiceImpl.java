package com.amy.sunpalaceartspace.service.impl;

import java.util.Arrays;
import java.util.List;

import com.amy.common.core.utils.DateUtils;
import com.amy.sunpalaceartspace.domain.entity.ReservationMessage;
import com.amy.sunpalaceartspace.enums.ReservationMessageStatus;
import com.amy.sunpalaceartspace.mapper.ReservationMessageMapper;
import com.amy.sunpalaceartspace.service.IReservationMessageService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 预约消息 服务层实现（MyBatis-Plus 模式，单表 CRUD）
 *
 * @author fantasyfan
 * @date 2026-09-28
 */
@Service
public class ReservationMessageServiceImpl extends ServiceImpl<ReservationMessageMapper, ReservationMessage>
        implements IReservationMessageService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveReservationMessage(ReservationMessage message) {
        if (message.getStatus() == null) {
            message.setStatus(ReservationMessageStatus.WAITING.getCode());
        }
        if (message.getRetryCount() == null) {
            message.setRetryCount(0);
        }
        message.setCreateTime(DateUtils.getNowDate());
        return save(message);
    }

    @Override
    public IPage<ReservationMessage> selectReservationMessagePage(Page<ReservationMessage> page,
            Long userId, Long projectId, String reservationId, Integer status) {
        LambdaQueryWrapper<ReservationMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(status != null, ReservationMessage::getStatus, status)
                .orderByDesc(ReservationMessage::getCreateTime);
        return page(page, wrapper);
    }

    @Override
    public List<ReservationMessage> selectByStatus(ReservationMessageStatus status) {
        LambdaQueryWrapper<ReservationMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReservationMessage::getStatus, status.getCode())
                .orderByAsc(ReservationMessage::getCreateTime);
        return list(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateStatus(Long reservationMessageId, ReservationMessageStatus status) {
        ReservationMessage update = new ReservationMessage();
        update.setStatus(status.getCode());
        update.setUpdateTime(DateUtils.getNowDate());
        LambdaUpdateWrapper<ReservationMessage> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ReservationMessage::getReservationMessageId, reservationMessageId);
        return update(update, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean markSent(Long reservationMessageId) {
        return updateStatus(reservationMessageId, ReservationMessageStatus.SENT);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean markSuccess(Long reservationMessageId) {
        return updateStatus(reservationMessageId, ReservationMessageStatus.SUCCESS);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean markFail(Long reservationMessageId) {
        return updateStatus(reservationMessageId, ReservationMessageStatus.FAIL);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean incrementRetryCount(Long reservationMessageId) {
        // 先取出当前重试次数，+1 后写回（避免并发下直接 set 累加丢失）
        ReservationMessage current = getById(reservationMessageId);
        if (current == null) {
            return false;
        }
        ReservationMessage update = new ReservationMessage();
        update.setRetryCount(current.getRetryCount() == null ? 1 : current.getRetryCount() + 1);
        update.setUpdateTime(DateUtils.getNowDate());
        LambdaUpdateWrapper<ReservationMessage> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ReservationMessage::getReservationMessageId, reservationMessageId);
        return update(update, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeReservationMessageByIds(Long[] reservationMessageIds) {
        return removeByIds(Arrays.asList(reservationMessageIds));
    }
}
