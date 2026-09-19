package com.amy.sunpalaceartspace.domain.resp.mina;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * @ClassName MinaProjectReservationInfo
 * @Description TODO
 * @Author fantasyfan
 * @Date 2026-09-20 00:21
 */
@Data
@Builder
public class MinaProjectReservationInfoResp {
    /** 预约须知（富文本） */
    private String reservationNotes;

    private List<ReservationDetails> reservationDetails;

    @Data
    @Builder
    public class ReservationDetails{
        /** 预约日期 **/
        private String reservationDate;

        private List<ReservationTimeDetails> reservationTimeDetails;
    }

    @Data
    @Builder
    public class ReservationTimeDetails {
        /** 预约时间段 **/
        private String reservationTime;
        /** 剩余预约号数 **/
        private Integer remainingReservationCount;
        /** 预约状态 **/
        private Integer reservationStatus;
    }
}
