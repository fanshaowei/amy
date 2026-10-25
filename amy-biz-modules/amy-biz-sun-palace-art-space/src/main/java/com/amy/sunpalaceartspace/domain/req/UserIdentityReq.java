package com.amy.sunpalaceartspace.domain.req;

import com.amy.sunpalaceartspace.enums.IdType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

    /** 所属会员ID（关联 biz_spas_reservation_user.reservation_user_id；可空表示未绑定会员/随行人员） */
    private Long reservationUserId;

    /** 用户姓名 */
    @NotBlank(message = "用户姓名不能为空")
    @Size(min = 0, max = 50, message = "用户姓名长度不能超过50个字符")
    private String name;

    /** 证件类型（1身份证 2护照 3港澳证件 4台湾通行证 5军官证） */
    @NotNull(message = "证件类型不能为空")
    private IdType idType;

    /** 证件号码 */
    @NotBlank(message = "证件号码不能为空")
    @Size(min = 0, max = 50, message = "证件号码长度不能超过50个字符")
    private String idNum;

    /** 性别（0女 1男）实名认证后会自动更新 */
    private Integer gender;

    /** 手机号码 */
    @NotBlank(message = "手机号码不能为空")
    @Size(min = 0, max = 11, message = "手机号码长度不能超过11个字符")
    private String phone;

    /** 是否跟随者（随行人）：false=否，true=是 */
    private Boolean isFollower;
}
