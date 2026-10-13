package com.amy.sunpalaceartspace.domain.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @ClassName VerifyUserReq
 * @Description TODO
 * @Author fantasyfan
 * @Date 2026-10-13 01:29
 */
@Data
public class VerifyUserReq {
    /** 用户ID */
    private Long reservationUserId;

    /** 身份证号 */
    @NotBlank(message = "证件号码不能为空")
    private String idNum;

    /** 证件类型（0身份证 1护照 2港澳台证件 3军官证） */
    @NotNull(message = "证件类型不能为空")
    private Integer idType;

    /** 用户姓名 */
    @NotBlank(message = "用户姓名不能为空")
    private String name;

    /** 用户手机号 */
    @NotBlank(message = "手机号不能为空")
    private String phone;

    @NotBlank(message = "性别不能为空")
    private Integer gender;
}
