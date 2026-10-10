package com.amy.sunpalaceartspace.domain.entity;

import com.amy.common.core.web.domain.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 预约消息对象 biz_spas_reservation_message
 *
 * @Description 预约消息发送记录（outbox）：创建后由 MQ 发送，记录发送状态与重试次数。
 *              status 取值见 {@link com.amy.sunpalaceartspace.enums.ReservationMessageStatus}
 *              （WAITING=1 待发送 / SENT=2 已发送 / SUCCESS=3 成功 / FAIL=0 失败）。
 * @Author fantasyfan
 * @Date 2026-09-27
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_spas_reservation_message")
public class ReservationMessage extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 预约消息ID */
    @TableId(type = IdType.AUTO)
    private Long reservationMessageId;

    /**
     * 消息
     */
    private String msgContent;

    /** 预约消息状态 {@link com.amy.sunpalaceartspace.enums.ReservationMessageStatus} */
    private Integer status;

    /** 重试次数 */
    private Integer retryCount;
}
