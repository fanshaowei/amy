package com.amy.sunpalaceartspace.utils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.Map;

import com.amy.common.core.web.domain.BaseEntity;

import lombok.extern.slf4j.Slf4j;

/**
 * 查询时间范围参数（{@code BaseEntity#params} 里的 beginTime / endTime）归一化工具
 *
 * <p><b>为什么需要它</b>：同一个「按日期查询」功能，前端列表请求和导出请求传出來的日期格式不一样：</p>
 * <pre>
 *   列表(GET  query string) : params[beginTime]=2026-10-10
 *   导出(POST JSON body)    : {"params":{"beginTime":"2026-10-09T16:00:00.000Z"}}
 * </pre>
 *
 * <p>前者是纯日期，后者是 ISO-8601 瞬时时间（dayjs/moment 对象被 JSON 序列化后的默认形态）。
 * 后端 SQL 里的时间条件是 {@code create_time >= concat(#{beginTime}, ' 00:00:00')}，
 * 一旦收到后者就会拼出非法的 datetime 字面量，MySQL 比较结果为 NULL（falsy）→ 查不到任何行。</p>
 *
 * <p><b>更隐蔽的坑：不能直接把 ISO 串交给 MySQL 解析。</b>
 * MySQL 的 {@code cast('2026-10-09T16:00:00.000Z' as datetime)} 结果是
 * {@code 2026-10-09 16:00:00} —— 它<b>丢弃了 Z 时区标记</b>，按本地时间解释。
 * 而这个时间点真正的北京时间（UTC+8）是 {@code 2026-10-10 00:00:00}，相差一天，
 * 查询结果会整体偏移一天。所以必须先在 Java 侧按时区换算，再以 {@code yyyy-MM-dd} 交给 SQL。</p>
 *
 * <p>本工具把入参统一归一化为 {@code yyyy-MM-dd} 字符串写回 params，
 * SQL 侧就只需处理一种格式，列表与导出的行为也必然一致。</p>
 *
 * @author fantasyfan
 * @Date 2026-11-01
 */
@Slf4j
public final class DateRangeParamUtils {

    /** params 里的开始时间 key */
    public static final String BEGIN_TIME = "beginTime";

    /** params 里的结束时间 key */
    public static final String END_TIME = "endTime";

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private DateRangeParamUtils() {
    }

    /**
     * 把查询对象 params 里的 beginTime / endTime 归一化为 {@code yyyy-MM-dd}。
     *
     * <p>无法解析的值会被<b>移除</b>（而不是原样透传）—— 透传非法值会让 SQL 比较得到 NULL，
     * 表现为「查不到任何数据」且没有任何报错，比直接忽略更难排查。</p>
     *
     * @param entity 查询条件对象（继承 BaseEntity）
     */
    public static void normalize(BaseEntity entity) {
        if (entity == null) {
            return;
        }
        Map<String, Object> params = entity.getParams();
        if (params == null || params.isEmpty()) {
            return;
        }
        normalizeKey(params, BEGIN_TIME);
        normalizeKey(params, END_TIME);
    }

    private static void normalizeKey(Map<String, Object> params, String key) {
        Object raw = params.get(key);
        if (raw == null) {
            return;
        }
        try {
            LocalDate date = toLocalDate(raw);
            if (date == null) {
                params.remove(key);
                return;
            }
            params.put(key, date.format(DATE_FORMATTER));
        } catch (Exception e) {
            log.warn("查询时间参数解析失败，已忽略该条件: {}={}", key, raw);
            params.remove(key);
        }
    }

    /**
     * 把时间截断到<b>当天 00:00:00</b>（按 JVM 默认时区）。
     *
     * <p>用途：预约订单列表「按日期筛选」时，Mapper 已改成半开区间
     * {@code reservation_time >= #{start} and reservation_time < date_add(#{start}, interval 1 day)}，
     * 用裸列做 range scan 以避免对列套 {@code date()} 导致索引失效。
     * 区间起点必须是<b>当天零点</b>，否则语义会从「查当天」退化成「查接下来 24 小时」。</p>
     *
     * <p>注意 {@code date_add} 只作用在<b>参数</b>上，不作用在列上，因此不影响索引使用，
     * 且是纯算术运算，不依赖 MySQL 会话时区。</p>
     *
     * @param value 原始时间，可为 null
     * @return 当天零点；入参为 null 或无法解析时返回 null
     */
    public static Date startOfDay(Date value) {
        if (value == null) {
            return null;
        }
        try {
            LocalDate date = toLocalDate(value);
            if (date == null) {
                return null;
            }
            return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
        } catch (Exception e) {
            log.warn("时间参数解析失败，已忽略该条件: {}", value);
            return null;
        }
    }

    /**
     * 把任意常见时间表示转换为 {@link LocalDate}（按 JVM 默认时区换算）
     *
     * <p>支持：{@code LocalDate} / {@code LocalDateTime} / {@code java.util.Date} /
     * {@code 2026-10-10} / {@code 2026-10-10 16:00:00} / {@code 2026-10-10T16:00:00} /
     * {@code 2026-10-09T16:00:00.000Z} / {@code 2026-10-09T16:00:00+08:00}</p>
     *
     * @param value 时间值
     * @return 日期，无法解析时返回 null
     */
    public static LocalDate toLocalDate(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime.toLocalDate();
        }
        if (value instanceof Date date) {
            return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()).toLocalDate();
        }

        String text = value.toString().trim();
        if (text.isEmpty()) {
            return null;
        }

        // 1) 纯日期 yyyy-MM-dd：直接解析，无时区问题
        if (text.length() == 10) {
            return LocalDate.parse(text, DATE_FORMATTER);
        }

        // 2) ISO-8601 瞬时时间（以 Z 结尾，如 2026-10-09T16:00:00.000Z）：
        //    先按 UTC 解析成 Instant，再换算到 JVM 时区取日期。这一步是关键，交给 MySQL 会丢时区。
        if (text.endsWith("Z") || text.endsWith("z")) {
            return LocalDateTime.ofInstant(Instant.parse(text), ZoneId.systemDefault()).toLocalDate();
        }

        // 3) 带偏移的日期时间（如 2026-10-09T16:00:00+08:00）
        try {
            return OffsetDateTime.parse(text).atZoneSameInstant(ZoneId.systemDefault()).toLocalDate();
        } catch (DateTimeParseException ignored) {
            // 落到 4
        }

        // 4) 无时区的日期时间（2026-10-10 16:00:00 / 2026-10-10T16:00:00[.000]）：
        //    业务上前端传的就是本地时间，直接取日期部分即可
        String normalized = text.replace('T', ' ');
        int dotIndex = normalized.indexOf('.');
        if (dotIndex > 0) {
            normalized = normalized.substring(0, dotIndex);
        }
        return LocalDateTime.parse(normalized, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).toLocalDate();
    }
}
