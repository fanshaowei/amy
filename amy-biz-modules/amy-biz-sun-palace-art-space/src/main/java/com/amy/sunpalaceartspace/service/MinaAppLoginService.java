package com.amy.sunpalaceartspace.service;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import cn.binarywang.wx.miniapp.bean.WxMaPhoneNumberInfo;
import com.amy.common.core.domain.R;
import com.amy.common.core.exception.InnerAuthException;
import com.amy.common.core.exception.ServiceException;
import com.amy.common.core.utils.JwtUtils;
import com.amy.common.core.utils.uuid.IdUtils;
import com.amy.common.redis.service.RedisService;
import com.amy.sunpalaceartspace.constant.SpasConstant;
import com.amy.sunpalaceartspace.domain.bo.WechatApiTokenClaimBO;
import com.amy.sunpalaceartspace.domain.entity.ReservationUser;
import com.amy.sunpalaceartspace.domain.req.ReservationUserReq;
import com.amy.sunpalaceartspace.domain.req.mina.UpdateReservationUserReq;
import com.amy.sunpalaceartspace.domain.req.mina.WechatApiAuthLoginReq;
import com.amy.sunpalaceartspace.domain.resp.mina.WechatApiAuthLoginResp;
import com.amy.sunpalaceartspace.enums.ReservationUserStatusEnum;
import com.amy.sunpalaceartspace.utils.FileUrlNormalizer;
import com.amy.system.api.RemoteFileService;
import com.amy.system.api.domain.SysFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.amy.sunpalaceartspace.constant.SpasConstant.*;

/**
 * @ClassName WechatService
 * @Description 小程序登录处理类
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
    private final RemoteFileService remoteFileService;

    /**
     * 上传小程序微信头像，返回可入库的资源 key。
     * Feign 返回的就是 amy-file 按 file.prefix 产出的 key；归一化只兜底拒绝临时地址。
     */
    public String uploadReservationUserAvatar(MultipartFile file) {
        R<SysFile> upload = remoteFileService.upload(file);
        if (upload == null || upload.getData() == null || !StringUtils.hasText(upload.getData().getUrl())) {
            throw new ServiceException("头像上传失败，请稍后重试");
        }
        return FileUrlNormalizer.normalize(upload.getData().getUrl());
    }

    public ReservationUser updateReservationUserInfo(String token, UpdateReservationUserReq req) {
        ReservationUser reservationUser = getUserByToken(token);
        reservationUser.setNickname(req.getNickName());
        // 前端适配层提交前已把展示地址还原成 key；这里只兜底拒绝 wxfile:// 等临时路径，
        // 直接拒绝而不是把已有头像清空（编辑页的头像字段是必填的）
        String avatarKey = FileUrlNormalizer.normalize(req.getAvatarImgUrl());
        if (!StringUtils.hasText(avatarKey)) {
            throw new ServiceException("头像地址无效，请重新选择头像");
        }
        reservationUser.setAvatarImgUrl(avatarKey);
        reservationUserService.getBaseMapper().updateById(reservationUser);
        return reservationUser;
    }

    /**
     * 生成 spasToken: token每次生成都会用一个新的UUID
     * @param req
     * @return
     */
    public WechatApiAuthLoginResp generalApiAuthToken(WechatApiAuthLoginReq req) {
        WxMaJscode2SessionResult wxMaJscode2SessionResult = null;
        try {
            wxMaJscode2SessionResult = wxMaService.jsCode2SessionInfo(req.getJsCode());
        } catch (WxErrorException e) {
            throw new ServiceException("fail to get session info by jsCode");
        }
        String openId = wxMaJscode2SessionResult.getOpenid();
        String sessionKey = wxMaJscode2SessionResult.getSessionKey();
        log.info("login openid: {}, sessionKey: {}", openId, sessionKey);

        // 首次登录（库里无该 openId 会员）时创建会员记录并写入本次提交的昵称/头像
        boolean userExists = reservationUserService.checkReservationUserByOpenId(openId);
        boolean firstTimeLogin = !userExists;
        if (firstTimeLogin) {
            createReservationUserByWechatInfo(req, openId);
        }
        // 生成 spasToken
        String spasToken = generalJwt();
        saveMinaUserLoginCache(spasToken, openId, sessionKey);
        // fistTimeLogin=true 表示本次新建了会员（本次提交的头像/昵称已随登录写入）；
        // false 表示老会员，登录接口不覆盖其资料，也不重复上传头像；
        // 头像/昵称仅在编辑资料页由用户主动调 /reservation/user/update 更新
        return WechatApiAuthLoginResp.builder()
                .fistTimeLogin(firstTimeLogin)
                .token(spasToken)
                .build();
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
                // 首登资料同样只落资源 key
                .avatarImgUrl(FileUrlNormalizer.normalize(req.getAvatarImgUrl()))
                .status(ReservationUserStatusEnum.ACTIVE.getCode())
                .build();
        reservationUserService.saveReservationUser(reservationUserReq);
    }

    /**
     * exp 设为【7 天绝对上限】仅作安全兜底（防 token 永不过期）；
     * 真正会话有效期由 Redis 滑动 TTL 决定（见 MinaApiAuthAspect：每次请求续 2h，闲置超 2h 即失效）。
     * @return
     */
    private String generalJwt() {
        // JWT 标准：exp/iat/nbf 必须为秒（NumericDate），不能用毫秒，否则 jjwt 解析会因 nbf/exp 被误读为公元数万年而拒绝。
        long nowSeconds = System.currentTimeMillis() / 1000;
        //nowSeconds = DateUtils.addDays(new Date(), -1).getTime() / 1000;
        WechatApiTokenClaimBO wechatApiTokenClaimBO = WechatApiTokenClaimBO.builder()
                .jti(IdUtils.fastUUID())
                .iss(ISSUER)
                .aud(AUDIENCE)
                .iat(nowSeconds)
                .nbf(nowSeconds)
                .exp(nowSeconds + API_TOKEN_ABSOLUTE_TTL_SECONDS)
                .build();
        return jwtUtils.createToken(objectMapper.convertValue(wechatApiTokenClaimBO, Map.class));
    }

    private void saveMinaUserLoginCache(String spasToken, String openId, String sessionKey) {
        redisService.setCacheObject(String.format(CASH_WX_API_TOKEN_OPEN_ID, spasToken), openId, 24L, TimeUnit.HOURS);
        redisService.setCacheObject(String.format(SpasConstant.CASH_WX_OPEN_ID_SESSION_KEY, openId), sessionKey);
    }


    public String getWechatUserPhone(String spasToken, String jsCode) {
        WxMaPhoneNumberInfo phoneNumberInfo = null;
        try {
            phoneNumberInfo = wxMaService.getUserService().getPhoneNumber(jsCode);
        } catch (WxErrorException e) {
            log.error("fail to get phone number by jsCode");
            throw new RuntimeException(e);
        }
        Object openId = redisService.getCacheObject(String.format(CASH_WX_API_TOKEN_OPEN_ID, spasToken));
        if(null != openId) {
            log.info("start to update phone by openId:{}", openId);
            reservationUserService.updatePhoneByOpenId((String) openId, phoneNumberInfo.getPurePhoneNumber());
            log.info("end to update phone by openId");
        }
        return phoneNumberInfo.getPurePhoneNumber();
    }

    public ReservationUser getUserByToken(String token) {
        Object openId = redisService.getCacheObject(String.format(CASH_WX_API_TOKEN_OPEN_ID, token));
        if (null == openId) {
            throw new InnerAuthException("用户未登录或登录已过期，请重新登录");
        }

        return reservationUserService.selectUserInfoByOpenId((String) openId);
    }
}
