package com.amy.sunpalaceartspace.domain.bo;

import com.amy.common.mq.bo.AmyMqProducerBO;
import lombok.Builder;
import lombok.Data;

import java.util.Date;

/**
 * @ClassName ReservationOrderMqSendBO
 * @Description TODO
 * @Author fantasyfan
 * @Date 2026-09-27 12:04
 */
@Builder
@Data
public class ReservationOrderMqSendBO extends AmyMqProducerBO {
    private Long reservationMessageId;
    private Long reservationUserId;
    private Long projectId;
    private String reservationNum;
    private Date reservationTime;
}
