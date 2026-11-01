package com.amy.sunpalaceartspace.utils;

import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

/**
 * 联系方式格式校验工具类。
 *
 * <p>只做<b>号码格式</b>校验（号段、长度、字符组合），不校验号码是否真实在用——真实有效性
 * 取决于运营商分配，无法离线判断。</p>
 *
 * <p>各联系方式规则说明（规则来源：国内现行号码编码惯例）：</p>
 * <ul>
 *   <li>手机号：11 位，1 开头、第二位 3~9（1[3-9]\d{9}）。兼容带国家码前缀的写法，如
 *       {@code +86 138...}、{@code 0086 138...}、{@code 86-138...}（前缀会被自动剥离后判定）。</li>
 *   <li>座机/固定电话：区号(0\d{2,3}) + 本地号(7~8 位)，中间允许一个连字符，如
 *       {@code 010-12345678}、{@code 0571-87654321}；可带分机号（连字符后 1~5 位）。</li>
 *   <li>400/800 服务号：10 位（400/800 + 7 位），允许连字符，如 {@code 400-123-4567}。</li>
 *   <li>电话（isPhone）：手机号 <b>或</b> 座机，覆盖「填写任意可接通号码」的场景。</li>
 *   <li>联系方式（isContact）：手机号 <b>或</b> 座机 <b>或</b> 400/800 服务号，最宽松的兜底入口。</li>
 * </ul>
 *
 * @ClassName PhoneVerifyUtil
 * @Description 手机号 / 座机 / 服务号等联系方式格式校验
 * @Author fantasyfan
 * @Date 2026-11-02 01:09
 */
public class PhoneVerifyUtil {

    /** 手机号：1 开头，第二位 3~9，共 11 位 */
    private static final Pattern MOBILE = Pattern.compile("^1[3-9]\\d{9}$");

    /** 座机/固定电话：区号(0\d{2,3}) + 本地号(7~8 位)，可带连字符与分机 */
    private static final Pattern LANDLINE = Pattern.compile("^0\\d{2,3}-?\\d{7,8}(-\\d{1,5})?$");

    /** 400/800 服务号：10 位，允许连字符 */
    private static final Pattern SERVICE = Pattern.compile("^(400|800)-?\\d{3}-?\\d{4}$");

    /**
     * 手机号校验（自动剥离 +86 / 86 / 0086 等常见国家码前缀及空白、连字符）。
     *
     * @param phone 待校验号码
     * @return true=合法手机号
     */
    public static boolean isMobile(String phone) {
        String cleaned = cleanMobile(phone);
        return cleaned != null && MOBILE.matcher(cleaned).matches();
    }

    /**
     * 座机/固定电话校验（允许一个连字符与分机号，前后空白会被忽略）。
     */
    public static boolean isLandline(String phone) {
        return match(LANDLINE, phone);
    }

    /**
     * 400/800 服务号校验（允许连字符）。
     */
    public static boolean isServiceNumber(String phone) {
        return match(SERVICE, phone);
    }

    /**
     * 电话校验：手机号 <b>或</b> 座机。
     */
    public static boolean isPhone(String phone) {
        return isMobile(phone) || isLandline(phone);
    }

    /**
     * 联系方式校验（最宽松入口）：手机号 <b>或</b> 座机 <b>或</b> 400/800 服务号。
     *
     * <p>用于「联系方式」这种允许填任意常见号码的字段；若业务只允许手机，请改用 {@link #isMobile}。</p>
     */
    public static boolean isContact(String phone) {
        return isMobile(phone) || isLandline(phone) || isServiceNumber(phone);
    }

    /**
     * 归一化手机号：剥离国家码前缀（+86 / 86 / 0086）、空白与连字符，返回纯 11 位数字串。
     *
     * <p>仅当输入确为合法手机号时返回归一化结果；非法输入返回 {@code null}（不抛异常）。</p>
     *
     * @param phone 原始号码
     * @return 纯 11 位手机号，或 null
     */
    public static String normalizeMobile(String phone) {
        String cleaned = cleanMobile(phone);
        return (cleaned != null && MOBILE.matcher(cleaned).matches()) ? cleaned : null;
    }

    /**
     * 把带常见国家码前缀的手机号清洗成纯数字串，便于统一判定。
     *
     * <p>处理过程：trim → 去除所有空白与连字符 → 依次剥离 {@code +86} / {@code 0086} / {@code 86} 前缀
     * （仅当剩余正好 11 位时，避免误删本身以 86 开头却非国家码的号码）。</p>
     */
    private static String cleanMobile(String phone) {
        if (!StringUtils.hasText(phone)) {
            return null;
        }
        String s = phone.trim().replaceAll("[\\s-]", "");
        if (s.startsWith("+86")) {
            s = s.substring(3);
        } else if (s.startsWith("0086")) {
            s = s.substring(4);
        } else if (s.startsWith("86") && s.length() == 13) {
            // 86 + 11 位手机号：把 86 视为国家码前缀
            s = s.substring(2);
        }
        return s;
    }

    /** 去空格后逐个正则匹配（用于座机 / 服务号等无需前缀处理的号码） */
    private static boolean match(Pattern pattern, String phone) {
        if (!StringUtils.hasText(phone)) {
            return false;
        }
        return pattern.matcher(phone.trim()).matches();
    }
}
