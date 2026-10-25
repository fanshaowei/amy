package com.amy.sunpalaceartspace.utils;

import com.amy.sunpalaceartspace.enums.IdType;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.regex.Pattern;

/**
 * 证件号码格式校验工具类。
 *
 * <p>只做<b>号码格式/合法性</b>校验（长度、字符组合、出生日期、身份证校验位），
 * 不校验真伪——真伪必须走 {@code AliIdCheckService} 的实名核验接口。</p>
 *
 * <p>各证件规则说明（规则来源：各证件现行编码惯例）：</p>
 * <ul>
 *   <li>身份证：18 位，含 ISO 7064:1983 MOD 11-2 校验位；兼容 15 位老身份证（无校验位）。</li>
 *   <li>护照：1~2 位字母 + 6~10 位数字，覆盖中国电子/普通/外交/公务护照（E/G/D/P/EA 等）。</li>
 *   <li>港澳通行证：C/W 开头（内地居民往来港澳）+ 8 位数字；H/M 开头（港澳居民来往内地，即回乡证）同样放行。</li>
 *   <li>台湾通行证：8 位数字，或 T + 8 位数字（含台胞证）。</li>
 *   <li>军官证：<b>无全国统一编码规则</b>，各军区/军种格式不一，只能做宽松校验（中文/字母/数字，长度 4~20）。</li>
 * </ul>
 *
 * @ClassName IdVerifyUtil
 * @Description 证件号码校验
 * @Author fantasyfan
 * @Date 2026-10-25 20:18
 */
public class IdVerifyUtil {

    /** 18 位身份证：6 位地区码(首位非 0) + 8 位出生日期 + 3 位顺序码 + 1 位校验码(数字或 X) */
    private static final Pattern ID_CARD_18 =
            Pattern.compile("^[1-9]\\d{5}(18|19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[\\dXx]$");

    /** 15 位老身份证：6 位地区码 + 6 位出生日期(yyMMdd) + 3 位顺序码，全是数字且无校验位 */
    private static final Pattern ID_CARD_15 =
            Pattern.compile("^[1-9]\\d{5}\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}$");

    /** 护照：1~2 位字母 + 6~10 位数字 */
    private static final Pattern PASSPORT = Pattern.compile("^[A-Za-z]{1,2}\\d{6,10}$");

    /** 港澳通行证：C/W/H/M + 8 位数字 */
    private static final Pattern HONGKONG_MACAO_PASS = Pattern.compile("^[CWHM]\\d{8}$");

    /** 台湾通行证：8 位数字，或 T + 8 位数字 */
    private static final Pattern TAIWAN_PASS = Pattern.compile("^[Tt]?\\d{8}$");

    /** 军官证：无统一格式，宽松校验（中文/字母/数字/短横线，长度 4~20） */
    private static final Pattern MILITARY_OFFICER_CARD = Pattern.compile("^[\\u4e00-\\u9fa5A-Za-z0-9-]{4,20}$");

    /** 身份证校验位加权因子（ISO 7064:1983 MOD 11-2） */
    private static final int[] ID_CARD_WEIGHT = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};

    /** 身份证校验码表：下标为 (加权和 % 11) */
    private static final char[] ID_CARD_CHECK_CODE = {'1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'};

    /**
     * 严格模式日期格式：uuuuMMdd。
     * 用 STRICT 而非默认 SMART，否则 2 月 30 日这类非法日期会被 SMART 自动修正成 2 月 28/29 日而误判为合法。
     */
    private static final DateTimeFormatter BIRTHDAY_FORMATTER =
            DateTimeFormatter.ofPattern("uuuuMMdd").withResolverStyle(ResolverStyle.STRICT);

    /**
     * 按证件类型校验号码格式（推荐入口）。
     *
     * @param idType 证件类型枚举
     * @param idNum  证件号码
     * @return true=格式合法
     */
    public static boolean verify(IdType idType, String idNum) {
        if (idType == null || !StringUtils.hasText(idNum)) {
            return false;
        }
        return switch (idType) {
            case ID_CARD -> isIdCard(idNum);
            case PASSPORT -> isPassport(idNum);
            case HONGKONG_MACAO_PASS -> isHongKongMacaoPass(idNum);
            case TAIWAN_PASS -> isTaiwanPass(idNum);
            case MILITARY_OFFICER_CARD -> isMilitaryOfficerCard(idNum);
        };
    }

    /**
     * 按证件类型编码校验号码格式。
     *
     * @param type  证件类型编码（1 身份证 / 2 护照 / 3 港澳 / 4 台湾 / 5 军官证）
     * @param idNum 证件号码
     * @return true=格式合法；类型编码无法识别时返回 false
     */
    public static boolean verify(Integer type, String idNum) {
        return verify(IdType.fromType(type), idNum);
    }

    /**
     * 校验不通过时抛出业务异常的便捷入口。
     *
     * @param type  证件类型编码
     * @param idNum 证件号码
     * @throws com.amy.common.core.exception.ServiceException 号码格式不合法
     */
    public static void verifyOrThrow(Integer type, String idNum) {
        IdType idType = IdType.fromType(type);
        if (idType == null) {
            throw new com.amy.common.core.exception.ServiceException("不支持的证件类型");
        }
        if (!verify(idType, idNum)) {
            throw new com.amy.common.core.exception.ServiceException(idType.getName() + "号码格式不正确");
        }
    }

    /**
     * 身份证号校验：18 位（含 MOD 11-2 校验位与出生日期合法性），兼容 15 位老身份证。
     */
    public static boolean isIdCard(String idNum) {
        if (!StringUtils.hasText(idNum)) {
            return false;
        }
        String id = idNum.trim();
        if (ID_CARD_18.matcher(id).matches()) {
            return isIdCard18Valid(id);
        }
        if (ID_CARD_15.matcher(id).matches()) {
            // 15 位无校验位，只校验出生日期（补世纪位 19）
            return isBirthdayValid("19" + id.substring(6, 12));
        }
        return false;
    }

    /** 护照号校验 */
    public static boolean isPassport(String idNum) {
        return match(PASSPORT, idNum);
    }

    /** 港澳通行证号校验 */
    public static boolean isHongKongMacaoPass(String idNum) {
        return match(HONGKONG_MACAO_PASS, idNum);
    }

    /** 台湾通行证号校验 */
    public static boolean isTaiwanPass(String idNum) {
        return match(TAIWAN_PASS, idNum);
    }

    /** 军官证号校验（无统一编码规则，仅做宽松校验） */
    public static boolean isMilitaryOfficerCard(String idNum) {
        return match(MILITARY_OFFICER_CARD, idNum);
    }

    /**
     * 18 位身份证：校验出生日期 + MOD 11-2 校验位。
     */
    private static boolean isIdCard18Valid(String id) {
        if (!isBirthdayValid(id.substring(6, 14))) {
            return false;
        }
        int sum = 0;
        for (int i = 0; i < 17; i++) {
            sum += (id.charAt(i) - '0') * ID_CARD_WEIGHT[i];
        }
        char expected = ID_CARD_CHECK_CODE[sum % 11];
        return expected == Character.toUpperCase(id.charAt(17));
    }

    /** 出生日期合法性（严格模式，排除 2 月 30 日这类非法日期） */
    private static boolean isBirthdayValid(String yyyyMMdd) {
        try {
            LocalDate.parse(yyyyMMdd, BIRTHDAY_FORMATTER);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    /** 去空格后逐个正则匹配 */
    private static boolean match(Pattern pattern, String idNum) {
        if (!StringUtils.hasText(idNum)) {
            return false;
        }
        return pattern.matcher(idNum.trim()).matches();
    }
}
