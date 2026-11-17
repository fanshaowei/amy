package com.amy.sunpalaceartspace.domain.resp.mina;

import lombok.Builder;
import lombok.Data;

/**
 * @ClassName MinaReservationUserInfo
 * @Description TODO
 * @Author fantasyfan
 * @Date 2026-09-20 12:55
 */
@Data
@Builder
public class MinaReservationUserInfoResp {
    /** 会员ID */
    private Long reservationUserId;

    /** 微信昵称 */
    private String nickname;

    /** 微信头像地址 */
    private String avatarImgUrl;

    /** 手机号 */
    private String phone;

    /** 类型（1用户 2核销员） */
    private Integer type;

    /** 是否已实名 */
    private Boolean isVerified;

    /** 证件号码 */
    private String idNum;

    /** 证件类型（与 IdType 枚举一致：1身份证 2护照 3港澳证件 4台湾通行证 5军官证） */
    private Integer idType;

    /** 实名姓名（未实名时为空） */
    private String name;
}
