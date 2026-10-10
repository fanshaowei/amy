package com.amy.sunpalaceartspace.constant;

/**
 * @ClassName WechatRedisContant
 * @Description TODO
 * @Author fantasyfan
 * @Date 2026-09-18 23:29
 */
public class SpasConstant {
    /********** 预约订单相关 ***********/
    // 生成预约订单时，控制订单号唯一的序列
    public static final String RESERVATION_ORDER_NUM_SEQ = "reservation:order:seq:%s";

    // 每天的预约库存 eg: reservation:project:1:stock:202609270930
    public static final String RESERVATION_STOCK_KEY_PREFIX = "reservation:project:%s:stock:%s";

    // 每个用户每天的预约记录 eg:reservation:project:1:user:202609270930:001
    public static final String RESERVATION_USER_KEY_PREFIX = "reservation:project:%s:user:%s:%s";

    /********** 微信相关 ***********/
    public static final String WX_API_TOKEN_OPEN_ID = "wx:api:token:%s";
    public static final String WX_OPEN_ID_SESSION_KEY = "wx:sessionKey:%s";

    public static final Long API_TOKEN_EXPIRE_INTERVAL = 7200L;
    /**
     * spasToken 的【绝对上限】，单位：秒。JWT 的 exp 固定为此值（7 天），仅作安全兜底（防 token 永不过期）。
     * 真正的会话有效期由 Redis 滑动 TTL（存 openId，每次请求续 2h）决定：访问越活跃越不过期，闲置超 2h 即失效。
     */
    public static final long API_TOKEN_ABSOLUTE_TTL_SECONDS = 7L * 24 * 3600;
    public static final String ISSUER = "SPAS";
    public static final String AUDIENCE = "MINA_APP";

    /********** mq相关 ***********/
    public static final String MQ_TOPIC = "amy_message_spas_mq_topic";
    public static final String MQ_TAG = "amy_message_spas_mq_tag";
    public static final String MQ_CONSUMER_GROUP = "amy_message_spas_mq_consumer_group";
    public static final String MQ_DESTINATE = MQ_TOPIC + ":" + MQ_TAG;
}
