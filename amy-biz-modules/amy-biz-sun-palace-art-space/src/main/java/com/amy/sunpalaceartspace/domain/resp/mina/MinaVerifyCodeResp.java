package com.amy.sunpalaceartspace.domain.resp.mina;

import lombok.Builder;
import lombok.Data;

/**
 * 小程序「预约详情」核销码刷新结果。
 * 每次调用都会换发一枚新核销码并重置 Redis 有效期，
 * 前端据此按 verifyCodeEffectiveDuration 倒计时，与后端 TTL 严格对齐。
 *
 * @author amy
 */
@Builder
@Data
public class MinaVerifyCodeResp {
    /** 新核销码（小程序订单为 Redis 中的一次性 UUID） */
    private String verifyCode;

    /** 核销码有效持续时长，单位分钟（与 spas.verifyCodeEffectiveDuration 一致） */
    private Long verifyCodeEffectiveDuration;
}
