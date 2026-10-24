package com.amy.sunpalaceartspace.domain.req;

import jakarta.validation.constraints.NotBlank;
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
    @NotBlank(message = "jsCode不能为空")
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
