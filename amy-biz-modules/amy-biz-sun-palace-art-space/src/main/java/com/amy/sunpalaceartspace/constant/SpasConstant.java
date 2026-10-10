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
    // redis cash, spasToken : openId
    public static final String CASH_WX_API_TOKEN_OPEN_ID = "wx:api:token:%s";
    // redis cash,  openId : sessionKey
    public static final String CASH_WX_OPEN_ID_SESSION_KEY = "wx:sessionKey:%s";

    /**
     * spasToken 的【绝对上限】，单位：秒。JWT 的 exp 固定为此值（7 天），仅作安全兜底（防 token 永不过期）。
     * 真正的会话有效期由 Redis 滑动 TTL（存 openId，每次请求续 2h）决定：访问越活跃越不过期，闲置超 2h 即失效。
     */
    public static final long API_TOKEN_ABSOLUTE_TTL_SECONDS = 7L * 24 * 3600;
    public static final String ISSUER = "SPAS";
    public static final String AUDIENCE = "MINA_APP";

    /********** 预约库存放号相关 ***********/
    // 每批次每时段放号数：批次1(12:00) 与 批次2(00:00) 各放 10，合计 20
    public static final int RESERVATION_RELEASE_BATCH_STEP = 10;

    // 放号分布式锁（与 SAPS_LOCK 区分，避免与其它任务相互阻塞）
    public static final String SAPS_RELEASE_LOCK = "spas:lock:release:stock";

    // 用户预约防并发重复标记 key（value=预约单号）；仅作 Lua 内的短期防并发重复（EX 600s 后自动失效），
    // 不再表示"一人一次"永久标记（预约频次改为按 DB 订单统计：1月最多2次、3天内不可重复）。
    public static final String RESERVATION_USER_DONE_KEY = "reservation:user:done:%s";

    /********** mq相关 ***********/
    public static final String MQ_TOPIC = "amy_message_spas_mq_topic";
    public static final String MQ_TAG = "amy_message_spas_mq_tag";
    public static final String MQ_CONSUMER_GROUP = "amy_message_spas_mq_consumer_group";
    public static final String MQ_DESTINATE = MQ_TOPIC + ":" + MQ_TAG;

    public static final String SAPS_LOCK = "reservation-stock-lock";
}
