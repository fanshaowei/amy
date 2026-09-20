package com.amy.sunpalaceartspace.controller;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import cn.binarywang.wx.miniapp.bean.WxMaUserInfo;
import com.amy.common.core.domain.R;
import com.amy.common.core.web.controller.BaseController;
import com.amy.sunpalaceartspace.domain.req.WechatApiAuthLoginReq;
import com.amy.sunpalaceartspace.service.impl.WechatService;
import jakarta.websocket.server.PathParam;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.web.bind.annotation.*;

/**
 * @ClassName WechatController
 * @Description TODO
 * @Author fantasyfan
 * @Date 2026-09-12 20:26
 */
@Slf4j
@AllArgsConstructor
@RestController("/wechat")
public class WechatController extends BaseController {
    private final WxMaService  wxMaService;
    private final WechatService wechatService;

    /**
     * 小程序调用后端接口前，获取鉴权token,后续调用业务接口，都要在请求头携带该token
     * eg: Authorization: Bearer <token>
     * @param req
     * @return
     * @throws WxErrorException
     */
    @PostMapping("/mina/api/auth/login")
    public R<String> login(@RequestBody WechatApiAuthLoginReq req) throws WxErrorException {
        String token = wechatService.generalApiAuthToken(req);
        return R.ok(token);
    }

    /**
     * 获取用户手机号 并更新预约会号记录
     * @param token
     * @param jsCode
     * @return
     */
    @GetMapping("/user/phone/get/{jsCode}")
    public R<String> getWechatPhone(@RequestHeader("Authorization") String token,
                                    @PathVariable("jsCode") String jsCode) {
        return R.ok(wechatService.getWechatUserPhone(token, jsCode));
    }

    @GetMapping("/access-token/get")
    public R<String> getAccessToken() throws WxErrorException {
        return R.ok(wxMaService.getAccessToken());
    }

    @GetMapping("/user/session/info")
    public R<WxMaJscode2SessionResult> getWechatSession(@PathParam("jsCode") String jsCode) throws WxErrorException {
        return R.ok(wxMaService.getUserService().getSessionInfo(jsCode));
    }
}
