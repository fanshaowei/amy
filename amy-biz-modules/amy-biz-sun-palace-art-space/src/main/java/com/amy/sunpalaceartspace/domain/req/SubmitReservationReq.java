package com.amy.sunpalaceartspace.domain.req;

import com.amy.sunpalaceartspace.enums.ReservationOrderStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * @ClassName SubmitReservationReq
 * @Description TODO
 * @Author fantasyfan
 * @Date 2026-10-11 00:19
 */
@Data
public class SubmitReservationReq {
    /** 预约项目ID */
    @NotNull(message = "预约项目不能为空")
    private Long projectId;

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
    @NotNull(message = "预约人数不能为空")
    private Integer guestsNum;

    /** 预约时间 */
    @NotNull(message = "预约时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date reservationTime;

    /**
     * 预约随行人身份信息（数组列表字符串）。
     *
     * <p>每个元素内容为 {@code com.amy.sunpalaceartspace.domain.entity.UserIdentity}
     * 字段序列化后的 JSON 字符串，例如：
     * {@code "{\"name\":\"张三\",\"idNum\":\"110101199001011234\",\"phone\":\"13800138000\"}"}。</p>
     *
     * <p>前端可空数组 {@code []} 或 null；后端会在持久化时通过
     * {@link com.amy.sunpalaceartspace.config.StringListJsonTypeHandler}
     * 自动序列化为 JSON 数组字符串存入 {@code comp_users} 列。</p>
     */
    private List<String> compUsers;
}
