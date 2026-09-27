package com.amy.sunpalaceartspace.domain.bo;

import lombok.Builder;
import lombok.Data;

/**
 * @ClassName MqConsumerBO
 * @Description TODO
 * @Author fantasyfan
 * @Date 2026-09-27 11:56
 */
@Data
@Builder
public class ReservationOrderMqConsumerBO {
    private Long userId;
    private Object message;
}
