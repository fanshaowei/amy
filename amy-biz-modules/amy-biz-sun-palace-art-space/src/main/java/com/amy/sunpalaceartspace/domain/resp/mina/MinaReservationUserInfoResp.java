package com.amy.sunpalaceartspace.domain.resp.mina;

import com.amy.common.core.annotation.Excel;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
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
}
