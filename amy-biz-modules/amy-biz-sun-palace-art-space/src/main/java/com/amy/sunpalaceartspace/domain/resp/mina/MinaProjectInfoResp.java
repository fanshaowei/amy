package com.amy.sunpalaceartspace.domain.resp.mina;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @ClassName MinaProjectInfoResp
 * @Author fantasyfan
 * @Date 2026-09-19 16:17
 */
@NoArgsConstructor
@Data
public class MinaProjectInfoResp {
    /** 项目ID */
    private Long projectId;

    /** 项目名称 */
    private String projectName;

    /** 封面图URL（200*200） */
    private String coverArtUrl;

    /** 每日可预约截止时间（HH:mm:ss），过了该时间只能预约第二天的 */
    private String cutOffTime;

    /** 预约停留秒数（用户在预约界面可停留的时长） */
    private Integer reservationStaySecond;

    /** 预约开始时间（HH:mm:ss） */
    private String reservationStartTime;

    /** 预约结束时间（HH:mm:ss） */
    private String reservationEndTime;

    /** 预约间隔 */
    private Integer reservationIntervalSecond;

    /** 预约人数（每个时间段可预约的人数） */
    private Integer reservationCount;

    /** 可提前预约天数 */
    private Integer advanceReservationDays;

    /** 随行人数 */
    private Integer travelerCount;

    /** 预约须知（富文本） */
    private String reservationNotes;
}
