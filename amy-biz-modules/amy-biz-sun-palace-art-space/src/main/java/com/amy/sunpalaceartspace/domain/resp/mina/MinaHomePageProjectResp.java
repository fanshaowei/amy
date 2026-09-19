package com.amy.sunpalaceartspace.domain.resp.mina;

import com.amy.sunpalaceartspace.enums.ReservationOrderStatus;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @ClassName MinaProjectInfoResp
 * @Author fantasyfan
 * @Date 2026-09-19 16:17
 */
@Builder
@Data
public class MinaHomePageProjectResp {
    /** 项目ID */
    private Long projectId;

    /** 项目名称 */
    private String projectName;

    /** 封面图URL（200*200） */
    private String coverArtUrl;

    /** 可预约结束日期 **/
    private String reservationStartDate;

    /** 可预约结束日期 **/
    private String reservationEndDate;

    /** 预约开始时间（HH:mm:ss） */
    private String reservationStartTime;

    /** 预约结束时间（HH:mm:ss） */
    private String reservationEndTime;

    /** 剩余号数 **/
    private Integer remainingReservationCount;

    /** 总预约数 **/
    private Integer totalReservationCount;

    /** 每日可预约截止时间（HH:mm:ss），过了该时间只能预约第二天的 */
    private String cutOffTime;

    /** 预约状态 1:充足 2:紧张 3:已满
     * {@link com.amy.sunpalaceartspace.enums.ReservationStatusEnum}
     * **/
    private Integer reservationStatus;

    /** 随行人数 */
    private Integer travelerCount;

    /** 预约停留秒数（用户在预约界面可停留的时长） */
    private Integer reservationStaySecond;
}
