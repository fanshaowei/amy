package com.amy.sunpalaceartspace.domain.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 小程序「openId 静默续登」请求对象
 *
 * <p>本地缓存了微信 openId、但 token 已过期（Redis 失效）时，用 openId 重新换发 token。</p>
 *
 * @Author fantasyfan
 * @Date 2026-11-09
 */
@Data
public class MinaAppOpenIdLoginReq {

    /** 微信 openId（登录成功后由 /mina/app/user/info 下发并缓存到本地） */
    @NotBlank(message = "openId不能为空")
    private String openId;
}
