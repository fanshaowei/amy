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
    private String jsCode;
    private String avatarImgUrl;
    private String nickName;
}
