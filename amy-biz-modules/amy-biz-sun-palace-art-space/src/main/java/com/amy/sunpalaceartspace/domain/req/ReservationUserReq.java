package com.amy.sunpalaceartspace.domain.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

/**
 * 预约会员请求对象 ReservationUserReq
 *
 * @Description 新增/修改预约会员接口入参
 * @Author fantasyfan
 * @Date 2026-09-13
 */
@Builder
@Data
public class ReservationUserReq {

    /** 会员ID（新增时为空，修改时必填） */
    private Long reservationUserId;

    /** 会员编号（唯一） */
    @NotBlank(message = "会员编号不能为空")
    @Size(min = 0, max = 32, message = "会员编号长度不能超过32个字符")
    private String number;

    /** 微信昵称 */
    @Size(min = 0, max = 100, message = "微信昵称长度不能超过100个字符")
    private String nickname;

    /** 真实姓名 */
    @Size(min = 0, max = 50, message = "姓名长度不能超过50个字符")
    private String name;

    /** 手机号 */
    @NotBlank(message = "手机号不能为空")
    @Size(min = 0, max = 11, message = "手机号长度不能超过11个字符")
    private String phone;

    /** 微信头像地址 */
    @Size(min = 0, max = 255, message = "头像地址长度不能超过255个字符")
    private String avatarImgUrl;

    /** 微信 openid */
    @Size(min = 0, max = 100, message = "openid 长度不能超过100个字符")
    private String openId;

    /** 身份证号码 */
    @Size(min = 0, max = 18, message = "身份证号长度不能超过18个字符")
    private String idNum;

    /** 微信接口 token */
    @Size(min = 0, max = 500, message = "token 长度不能超过500个字符")
    private String token;

    /** 状态（0=正常,1=停用,2=删除） */
    @NotNull(message = "状态不能为空")
    private Integer status;

    /** 类型（1用户 2核销员） */
    @NotNull(message = "类型不能为空")
    private Integer type;

    /** 是否实名认证 */
    @NotNull(message = "实名认证状态不能为空")
    private Boolean isVerified;
}
