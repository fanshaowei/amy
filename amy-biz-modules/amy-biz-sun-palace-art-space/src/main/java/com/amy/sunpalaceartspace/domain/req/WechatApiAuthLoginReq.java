package com.amy.sunpalaceartspace.domain.req;

import lombok.Data;

/**
 * @ClassName WechatApiAuthLoginReq
 * @Description TODO
 * @Author fantasyfan
 * @Date 2026-09-20 14:22
 */
@Data
public class WechatApiAuthLoginReq {
    /**
     * 微信jsCode
     */
    private String jsCode;
    /**
     * 微信头像
     */
    private String avatarImgUrl;
    /**
     * 微信昵称
     */
    private String nickName;
}
