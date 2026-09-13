package com.amy.sunpalaceartspace.domain.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户身份信息请求对象 UserIdentityReq
 *
 * @Description 新增/修改用户身份信息接口入参
 * @Author fantasyfan
 * @Date 2026-09-13
 */
@Data
public class UserIdentityReq {

    /** 记录ID（新增时为空，修改时必填） */
    private Long userIdentityId;

    /** 用户姓名 */
    @NotBlank(message = "用户姓名不能为空")
    @Size(min = 0, max = 50, message = "用户姓名长度不能超过50个字符")
    private String name;

    /** 证件类型（0身份证 1护照 2港澳台证件 3军官证） */
    private Integer idType;

    /** 证件号码 */
    @NotBlank(message = "证件号码不能为空")
    @Size(min = 0, max = 50, message = "证件号码长度不能超过50个字符")
    private String idNum;

    /** 性别（0女 1男） */
    private Integer gender;

    /** 手机号码 */
    @NotBlank(message = "手机号码不能为空")
    @Size(min = 0, max = 11, message = "手机号码长度不能超过11个字符")
    private String phone;
}
