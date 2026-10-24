package com.amy.sunpalaceartspace.domain.bo;

import com.amy.common.mq.bo.AmyMqProducerBO;
import com.amy.sunpalaceartspace.domain.req.FollowerReq;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

/**
 * @ClassName ReservationOrderMqSendBO
 * @Description TODO
 * @Author fantasyfan
 * @Date 2026-09-27 12:04
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class ReservationOrderMqSendBO extends AmyMqProducerBO {
    private Long projectId;
    private String name;
    private String phone;
    private String idType;
    private String idNum;
    private Integer guestsNum;
    private Date reservationTime;
    private List<FollowerReq> compUsers;

    private String reservationNum;
    private Long reservationUserId;
}
