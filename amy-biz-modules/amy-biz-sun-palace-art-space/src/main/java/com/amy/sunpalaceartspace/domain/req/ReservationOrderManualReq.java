package com.amy.sunpalaceartspace.domain.req;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 后台「手动添加预约订单」入参。
 */
@Data
public class ReservationOrderManualReq {
    /** 预约项目ID */
    @NotNull(message = "预约项目不能为空")
    private Long projectId;

    /** 预约会员ID：后台补录不绑定会员，由 Controller 置 null */
    private Long reservationUserId;

    /** 预约人姓名 */
    @NotBlank(message = "姓名不能为空")
    @Size(min = 0, max = 50, message = "姓名长度不能超过50个字符")
    private String name;

    /** 联系电话 */
    @NotBlank(message = "联系电话不能为空")
    @Size(min = 0, max = 11, message = "联系电话长度不能超过11个字符")
    private String phone;

    /** 证件类型：中文名或 1~5 编码，Service 会规范化成中文名后入库 */
    @NotBlank(message = "证件类型不能为空")
    @Size(min = 0, max = 20, message = "证件类型长度不能超过20个字符")
    private String idType;

    /** 证件号码 */
    @NotBlank(message = "证件号码不能为空")
    @Size(min = 0, max = 50, message = "证件号码长度不能超过50个字符")
    private String idNum;

    private Integer guestsNum;

    /** 预约时间（yyyy-MM-dd HH:mm:ss） */
    @NotNull(message = "预约时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date reservationTime;

    /** 预约状态：由 Controller 固定为「待核销」，不接受前端传值 */
    private Integer reservationStatus;

    /** 随行人：加 @Valid 使 FollowerReq 上的 @NotBlank 级联生效 */
    @Valid
    private List<FollowerReq> compUsers;
}
