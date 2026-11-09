package com.amy.sunpalaceartspace.domain.resp.mina;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @ClassName WechatApiAuthLoginResp
 * @Description TODO
 * @Author fantasyfan
 * @Date 2026-09-20 14:22
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WechatApiAuthLoginResp {
    private boolean fistTimeLogin;
    private String token;
}
