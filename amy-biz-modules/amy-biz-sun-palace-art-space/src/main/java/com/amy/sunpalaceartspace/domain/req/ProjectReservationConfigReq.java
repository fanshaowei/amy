package com.amy.sunpalaceartspace.domain.req;

import lombok.Data;

@Data
public class ProjectReservationConfigReq {
    private static final long serialVersionUID = 1L;

    /** 配置ID */
    private Long configId;

    /** 项目ID */
    private Long projectId;

    /** 预约开始时间（HH:mm:ss），每天生效 */
    private String startTime;

    /** 预约结束时间（HH:mm:ss，不含，左闭右开） */
    private String endTime;

    /** 是否可预约（true=可预约 false=不可预约） */
    private Boolean reservationEnabled;
}
