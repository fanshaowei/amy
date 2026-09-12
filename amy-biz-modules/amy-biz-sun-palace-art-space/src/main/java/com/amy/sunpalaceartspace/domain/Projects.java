package com.amy.sunpalaceartspace.domain;

import com.amy.common.core.web.domain.BaseEntity;
import lombok.Data;

/**
 * @ClassName Projects
 * @Description 项目信息
 * @Author fantasyfan
 * @Date 2026-09-12 21:49
 */
@Data
public class Projects extends BaseEntity {
    private String projectId;
    private String projectName;
    private String coverArtUrl;
    private String cutOffTime;
    private Integer reservationStaySecond;
    private String reservationStartTime;
    private String reservationEndTime;
    private Integer reservationIntervalSecond;
    private Integer reservationCount;
    private Integer advanceReservationDays;
    private Integer travelerCount;
    private String reservationNotes;
    private Integer sort;
}
