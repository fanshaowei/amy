package com.amy.sunpalaceartspace.domain.bo;

import lombok.Builder;
import lombok.Data;

/**
 * @ClassName WechatApiTokenClaim
 * @Description TODO
 * @Author fantasyfan
 * @Date 2026-09-19 11:06
 */
@Builder
@Data
public class WechatApiTokenClaimBO {
    // jwt id, token 唯一标识
    private String jti;
    // 签发者
    private String iss;
    // 主题/用户标识
    private String sub;
    // 接收方
    private String aud;
    // 过期时间
    private Long exp;
    // 签发时间
    private Long iat;
    // 在此之前不可用
    private Long nbf;
}
