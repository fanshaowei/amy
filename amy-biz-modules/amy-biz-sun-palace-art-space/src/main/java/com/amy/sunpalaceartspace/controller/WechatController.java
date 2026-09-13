package com.amy.sunpalaceartspace.controller;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import cn.binarywang.wx.miniapp.bean.WxMaUserInfo;
import com.amy.common.core.domain.R;
import com.amy.common.core.web.controller.BaseController;
import jakarta.websocket.server.PathParam;
import lombok.AllArgsConstructor;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @ClassName WechatController
 * @Description TODO
 * @Author fantasyfan
 * @Date 2026-09-12 20:26
 */
@AllArgsConstructor
@RestController("/wechat")
public class WechatController extends BaseController {
    private final WxMaService  wxMaService;

    @GetMapping("/access-token/get")
    public R<String> getAccessToken() throws WxErrorException {
        return R.ok(wxMaService.getAccessToken());
    }

    @GetMapping("/user/session/info")
    public R<WxMaJscode2SessionResult> getWechatSession(@PathParam("jsCode") String jsCode) throws WxErrorException {
        return R.ok(wxMaService.getUserService().getSessionInfo(jsCode));
    }

    @GetMapping("/user/info")
    public R<WxMaUserInfo> getWechatUserInfo(String sessionKey, String encryptedData, String ivStr ) {
        return R.ok(wxMaService.getUserService().getUserInfo(sessionKey, encryptedData, ivStr));
    }
}
