package com.amy.sunpalaceartspace.domain.req;

import com.amy.sunpalaceartspace.enums.ReservationOrderStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class ReservationOrderManualReq {
    /** 预约项目ID */
    @NotNull(message = "预约项目不能为空")
    private Long projectId;

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

    private Integer guestsNum;

    /** 预约时间 */
    @NotNull(message = "预约时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date reservationTime;

    private Integer reservationStatus;

    private List<FollowerReq> compUsers;
}
