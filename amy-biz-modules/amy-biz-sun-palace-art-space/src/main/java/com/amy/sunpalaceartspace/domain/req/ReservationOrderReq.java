package com.amy.sunpalaceartspace.domain.req;

import java.util.Date;

import com.amy.sunpalaceartspace.enums.ReservationOrderStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 预约订单请求对象 ReservationOrderReq
 *
 * @Description 新增/修改预约订单接口入参
 * @Author fantasyfan
 * @Date 2026-09-13
 */
@Data
public class ReservationOrderReq {

    /** 预约订单ID（新增时为空，修改时必填） */
    private Long reservationOrderId;

    /** 预约项目ID */
    @NotNull(message = "预约项目不能为空")
    private Long projectId;

    /**
     * 预约会员ID（关联 biz_spas_reservation_user.reservation_user_id）。
     * <p>可空：允许匿名预约；非空时后端不会校验会员是否存在，由数据库外键约束兜底。</p>
     */
    private Long reservationUserId;

    /** 预约人姓名 */
    @NotBlank(message = "姓名不能为空")
    @Size(min = 0, max = 50, message = "姓名长度不能超过50个字符")
    private String name;

    /** 联系电话 */
    @NotBlank(message = "联系电话不能为空")
    @Size(min = 0, max = 11, message = "联系电话长度不能超过11个字符")
    private String phone;

    /** 证件类型（身份证、护照等） */
    @NotBlank(message = "证件类型不能为空")
    @Size(min = 0, max = 20, message = "证件类型长度不能超过20个字符")
    private String idType;

    /** 证件号码 */
    @NotBlank(message = "证件号码不能为空")
    @Size(min = 0, max = 50, message = "证件号码长度不能超过50个字符")
    private String idNum;

    /** 预约人数（含预约人本人） */
    private Integer guestsNum;

    /** 预约时间 */
    @NotNull(message = "预约时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date reservationTime;

    /**
     * 预约状态。前端可传 {@code "0"/"1"/"2"/"3"} 或 {@code "WAIT_VERIFY"/...}，
     * 后端通过 {@link ReservationOrderStatus} 的 {@code @JsonCreator} 解析。
     */
    private ReservationOrderStatus reservationStatus;


}
