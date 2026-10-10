package com.amy.sunpalaceartspace.service;

import java.util.List;

import com.amy.sunpalaceartspace.domain.entity.ReservationMessage;
import com.amy.sunpalaceartspace.enums.ReservationMessageStatus;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;

/**
 * 预约消息 服务层
 *
 * @author fantasyfan
 * @date 2026-09-28
 */
public interface IReservationMessageService extends IService<ReservationMessage> {

    /**
     * 新增预约消息（默认状态 WAITING 待发送，重试次数 0）
     *
     * @param message 预约消息
     * @return 是否成功
     */
    boolean saveReservationMessage(ReservationMessage message);

    /**
     * 分页查询预约消息（可按会员、项目、预约ID、状态过滤；入参为空表示不限制）
     *
     * @param page          分页参数
     * @param userId        会员ID（可空）
     * @param projectId     项目ID（可空）
     * @param reservationId 预约ID（可空）
     * @param status        状态 code（可空）
     * @return 分页结果
     */
    IPage<ReservationMessage> selectReservationMessagePage(Page<ReservationMessage> page,
            Long userId, Long projectId, String reservationId, Integer status);

    /**
     * 按状态查询预约消息列表（用于发送/重试任务捞取 WAITING 或 FAIL 的消息）
     *
     * @param status 状态枚举
     * @return 消息列表（按创建时间升序）
     */
    List<ReservationMessage> selectByStatus(ReservationMessageStatus status);

    /**
     * 更新预约消息状态
     *
     * @param reservationMessageId 消息ID
     * @param status               目标状态
     * @return 是否成功
     */
    boolean updateStatus(Long reservationMessageId, ReservationMessageStatus status);

    /**
     * 标记消息为已发送 SENT（已投递到 MQ）
     *
     * @param reservationMessageId 消息ID
     * @return 是否成功
     */
    boolean markSent(Long reservationMessageId);

    /**
     * 标记消息为发送成功 SUCCESS
     *
     * @param reservationMessageId 消息ID
     * @return 是否成功
     */
    boolean markSuccess(Long reservationMessageId);

    /**
     * 标记消息为发送失败 FAIL
     *
     * @param reservationMessageId 消息ID
     * @return 是否成功
     */
    boolean markFail(Long reservationMessageId);

    /**
     * 重试次数 +1（配合 markFail 用于失败重试场景）
     *
     * @param reservationMessageId 消息ID
     * @return 是否成功
     */
    boolean incrementRetryCount(Long reservationMessageId);

    /**
     * 批量删除预约消息（软删除）
     *
     * @param reservationMessageIds 消息ID集合
     * @return 是否成功
     */
    boolean removeReservationMessageByIds(Long[] reservationMessageIds);
}
