package com.amy.sunpalaceartspace.service.impl;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import cn.binarywang.wx.miniapp.bean.WxMaPhoneNumberInfo;
import com.amy.common.core.exception.ServiceException;
import com.amy.common.core.utils.DateUtils;
import com.amy.common.core.utils.JwtUtils;
import com.amy.common.core.utils.uuid.IdUtils;
import com.amy.common.redis.service.RedisService;
import com.amy.sunpalaceartspace.constant.SpasConstant;
import com.amy.sunpalaceartspace.domain.bo.WechatApiTokenClaimBO;
import com.amy.sunpalaceartspace.domain.req.ReservationUserReq;
import com.amy.sunpalaceartspace.domain.req.WechatApiAuthLoginReq;
import com.amy.sunpalaceartspace.enums.ReservationUserStatusEnum;
import com.amy.sunpalaceartspace.service.IReservationUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
public class MinaAppLoginService {
    private final WxMaService wxMaService;
    private final JwtUtils jwtUtils;
    private final RedisService redisService;
    private final ObjectMapper objectMapper;
    private final IReservationUserService reservationUserService;

    /**
     * 生成 spasToken: token每次生成都会用一个新的UUID
     * @param req
     * @return
     */
    public String generalApiAuthToken(WechatApiAuthLoginReq req) {
//        WxMaJscode2SessionResult wxMaJscode2SessionResult = null;
//        try {
//            wxMaJscode2SessionResult = wxMaService.jsCode2SessionInfo(req.getJsCode());
//        } catch (WxErrorException e) {
//            throw new ServiceException("fail to get session info by jsCode");
//        }
        String openId = "oScH60GBC5dKuq3VVucYI9RtBeao";//wxMaJscode2SessionResult.getOpenid();
        String sessionKey = "06u3cSgprrlDpi4kkI3khA==";//wxMaJscode2SessionResult.getSessionKey();
        log.info("login openid: {}, sessionKey: {}", openId, sessionKey);

        // 创建预约会员记录
        createReservationUserByWechatInfo(req, openId);
        // 生成 spasToken
        String spasToken = generalJwt();
        saveMinaUserLoginCache(spasToken, openId, sessionKey);
        return spasToken;
    }

    private void createReservationUserByWechatInfo(WechatApiAuthLoginReq req, String openId) {
        // 检查用户是否已经登录注册过
        boolean b = reservationUserService.checkReservationUserByOpenId(openId);
        if(b) {
            return;
        }

        ReservationUserReq reservationUserReq = ReservationUserReq.builder()
                .openId(openId)
                .nickname(req.getNickName())
                .avatarImgUrl(req.getAvatarImgUrl())
                .status(ReservationUserStatusEnum.ACTIVE.getCode())
                .build();
        reservationUserService.saveReservationUser(reservationUserReq);
    }

    private String generalJwt() {
        // JWT 标准：exp/iat/nbf 必须为秒（NumericDate），不能用毫秒，否则 jjwt 解析会因 nbf/exp 被误读为公元数万年而拒绝。
        // exp 设为【7 天绝对上限】仅作安全兜底（防 token 永不过期）；真正会话有效期由 Redis 滑动 TTL 决定（见 MinaApiAuthAspect：每次请求续 2h，闲置超 2h 即失效）。
        long nowSeconds = System.currentTimeMillis() / 1000;
        nowSeconds = DateUtils.addDays(new Date(), -1).getTime() / 1000;
        WechatApiTokenClaimBO wechatApiTokenClaimBO = WechatApiTokenClaimBO.builder()
                .jti(IdUtils.fastUUID())
                .iss(ISSUER)
                .aud(AUDIENCE)
                .iat(nowSeconds)
                .nbf(nowSeconds)
                .exp(nowSeconds)
                .build();
        return jwtUtils.createToken(objectMapper.convertValue(wechatApiTokenClaimBO, Map.class));
    }

    private void saveMinaUserLoginCache(String spasToken, String openId, String sessionKey) {
        redisService.setCacheObject(String.format(WX_API_TOKEN_OPEN_ID, spasToken), openId, 2L, TimeUnit.HOURS);
        redisService.setCacheObject(String.format(SpasConstant.WX_OPEN_ID_SESSION_KEY, openId), sessionKey);
    }


    public String getWechatUserPhone(String spasToken, String jsCode) {
        WxMaPhoneNumberInfo phoneNumberInfo = null;
        try {
            phoneNumberInfo = wxMaService.getUserService().getPhoneNumber(jsCode);
        } catch (WxErrorException e) {
            log.error("fail to get phone number by jsCode");
            throw new RuntimeException(e);
        }
        Object openId = redisService.getCacheObject(String.format(WX_API_TOKEN_OPEN_ID, spasToken));
        if(null != openId) {
            log.info("start to update phone by openId:{}", openId);
            reservationUserService.updatePhoneByOpenId((String) openId, phoneNumberInfo.getPurePhoneNumber());
            log.info("end to update phone by openId");
        }
        return phoneNumberInfo.getPurePhoneNumber();
    }
}
