package com.amy.sunpalaceartspace.service.impl;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import com.amy.common.core.utils.JwtUtils;
import com.amy.common.core.utils.uuid.IdUtils;
import com.amy.common.redis.service.RedisService;
import com.amy.sunpalaceartspace.constant.SpasConstant;
import com.amy.sunpalaceartspace.domain.bo.WechatApiTokenClaimBO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.stereotype.Service;

import java.util.Map;

import static com.amy.sunpalaceartspace.constant.SpasConstant.*;

/**
 * @ClassName WechatService
 * @Description TODO
 * @Author fantasyfan
 * @Date 2026-09-19 11:41
 */
@Slf4j
@AllArgsConstructor
@Service
public class WechatService {
    private final WxMaService wxMaService;
    private final JwtUtils jwtUtils;
    private final RedisService redisService;
    private final ObjectMapper objectMapper;

    public String generalApiAuthToken(String jsCode) {
        log.info("login jsCode: {}", jsCode);
        WxMaJscode2SessionResult wxMaJscode2SessionResult = null;
        try {
            wxMaJscode2SessionResult = wxMaService.jsCode2SessionInfo(jsCode);
        } catch (WxErrorException e) {
            log.error("fail to get session info by jsCode");
            throw new RuntimeException(e);
        }

        String openid = wxMaJscode2SessionResult.getOpenid();
        String sessionKey = wxMaJscode2SessionResult.getSessionKey();
        log.info("login openid: {}, sessionKey: {}", openid, sessionKey);

        String token = generalJwt();
        redisService.setCacheObject(String.format(WX_API_TOKEN_OPEN_ID, token), openid);
        redisService.setCacheObject(String.format(SpasConstant.WX_OPEN_ID_SESSION_KEY, openid), sessionKey);
        return token;
    }

    private String generalJwt() {
        WechatApiTokenClaimBO wechatApiTokenClaimBO = WechatApiTokenClaimBO.builder()
                .jti(IdUtils.fastUUID())
                .iss(ISSUER)
                .aud(AUDIENCE)
                .iat(System.currentTimeMillis())
                .nbf(System.currentTimeMillis())
                .exp(System.currentTimeMillis() + API_TOKEN_EXPIRE_INTERVAL)
                .build();
        return jwtUtils.createToken(objectMapper.convertValue(wechatApiTokenClaimBO, Map.class));
    }
}
