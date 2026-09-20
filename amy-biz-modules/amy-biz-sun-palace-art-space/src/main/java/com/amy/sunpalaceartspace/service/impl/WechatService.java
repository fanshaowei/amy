package com.amy.sunpalaceartspace.service.impl;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import cn.binarywang.wx.miniapp.bean.WxMaPhoneNumberInfo;
import com.amy.common.core.utils.JwtUtils;
import com.amy.common.core.utils.uuid.IdUtils;
import com.amy.common.redis.service.RedisService;
import com.amy.sunpalaceartspace.constant.SpasConstant;
import com.amy.sunpalaceartspace.domain.bo.WechatApiTokenClaimBO;
import com.amy.sunpalaceartspace.domain.req.ReservationUserReq;
import com.amy.sunpalaceartspace.domain.req.WechatApiAuthLoginReq;
import com.amy.sunpalaceartspace.service.IReservationUserService;
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
    private final IReservationUserService reservationUserService;

    public String generalApiAuthToken(WechatApiAuthLoginReq req) {
        WxMaJscode2SessionResult wxMaJscode2SessionResult = null;
        try {
            wxMaJscode2SessionResult = wxMaService.jsCode2SessionInfo(req.getJsCode());
        } catch (WxErrorException e) {
            log.error("fail to get session info by jsCode");
            throw new RuntimeException(e);
        }
        String openId = wxMaJscode2SessionResult.getOpenid();
        String sessionKey = wxMaJscode2SessionResult.getSessionKey();
        log.info("login openid: {}, sessionKey: {}", openId, sessionKey);

        // 创建预约会员记录
        createReservationUserByWechatInfo(req, openId, sessionKey);

        String token = generalJwt();
        saveMinaUserLoginCache(token, openId, sessionKey);
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

    private void saveMinaUserLoginCache(String token, String openId, String sessionKey) {
        Object cacheOpenId = redisService.getCacheObject(String.format(WX_API_TOKEN_OPEN_ID, token));
        if(null != cacheOpenId) {
            if(!openId.equals(cacheOpenId)) {
                log.error("token: {} and openId: {} not match", token, openId);
                throw new RuntimeException("illegal user,token and openId not match");
            }
        } else {
            redisService.setCacheObject(String.format(WX_API_TOKEN_OPEN_ID, token), openId);
            redisService.setCacheObject(String.format(SpasConstant.WX_OPEN_ID_SESSION_KEY, openId), sessionKey);
        }
    }

    private void createReservationUserByWechatInfo(WechatApiAuthLoginReq req, String openId, String sessionKey) {
        ReservationUserReq reservationUserReq = ReservationUserReq.builder()
                .openId(openId)
                .nickname(req.getNickName())
                .avatarImgUrl(req.getAvatarImgUrl())
                .status(0)
                .build();
        reservationUserService.saveReservationUser(reservationUserReq);
    }

    public String getWechatUserPhone(String token, String jsCode) {
        WxMaPhoneNumberInfo phoneNumberInfo = null;
        try {
            phoneNumberInfo = wxMaService.getUserService().getPhoneNumber(jsCode);
        } catch (WxErrorException e) {
            log.error("fail to get phone number by jsCode");
            throw new RuntimeException(e);
        }
        Object openId = redisService.getCacheObject(String.format(WX_API_TOKEN_OPEN_ID, token));
        if(null != openId) {
            log.info("start to update phone by openId:{}", openId);
            reservationUserService.updatePhoneByOpenId((String) openId, phoneNumberInfo.getPurePhoneNumber());
            log.info("end to update phone by openId");
        }
        return phoneNumberInfo.getPurePhoneNumber();
    }
}
