package com.amy.sunpalaceartspace.constant;

/**
 * @ClassName WechatRedisContant
 * @Description TODO
 * @Author fantasyfan
 * @Date 2026-09-18 23:29
 */
public class SpasConstant {

    public static final String RESERVATION_ORDER_NUM_SEQ = "reservation:order:seq:%s";

    public static final String WX_API_TOKEN_OPEN_ID = "wx:api:token:%s";
    public static final String WX_OPEN_ID_SESSION_KEY = "wx:sessionKey:%s";

    public static final Long API_TOKEN_EXPIRE_INTERVAL = 7200 * 1000L;
    public static final String ISSUER = "amy-service";
    public static final String AUDIENCE = "MINA_APP";
}
