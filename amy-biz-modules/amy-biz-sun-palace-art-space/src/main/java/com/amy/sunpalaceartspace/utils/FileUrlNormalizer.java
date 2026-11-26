package com.amy.sunpalaceartspace.utils;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文件地址「入库前兜底清洗」。
 *
 * <p><b>存储约定</b>：数据库只存 amy-file 按 Nacos {@code file.prefix} 生成的资源 key
 * （例如 prefix=/statics 时形如 {@code /statics/2026/11/26/xxx.jpg}）。</p>
 *
 * <p><b>本类不认识、也不判断任何路径前缀</b>：前缀只有一个真源，就是 amy-file 的
 * {@code file.prefix}。改成任意值都不需要改这里，也不需要改前端 ——
 * 访问地址由两端前端 API 适配层拼装，表单回显地址由同一适配层在提交前还原成 key
 * （见 {@code fileUrl.ts} 的 {@code toStoredKey}）。历史数据由数据迁移单独处理。</p>
 *
 * <p>因此职责只有两条：</p>
 * <ul>
 *   <li>拒绝客户端临时地址（{@code wxfile://}、{@code file://}、{@code data:}、
 *       {@code blob:}、{@code http(s)://tmp/}）入库；</li>
 *   <li>相对地址去掉 query / fragment；完整 URL（含第三方外链）原样保留。</li>
 * </ul>
 *
 * <p>纯静态、无 Spring 依赖、不读配置，便于编译后用临时 main 直接断言。</p>
 *
 * @author amy
 */
public final class FileUrlNormalizer
{
    /** 小程序 / 浏览器本地临时资源，绝不能入库 */
    private static final String[] TEMP_PREFIXES = {
            "wxfile://", "file://", "data:", "blob:", "http://tmp/", "https://tmp/"
    };

    /** 富文本里 &lt;img src="..."&gt; 的 src（单双引号、大小写、属性空白均兼容） */
    private static final Pattern IMG_SRC = Pattern.compile(
            "(<img\\b[^>]*?\\bsrc\\s*=\\s*)([\"'])([^\"']+)(\\2)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private FileUrlNormalizer()
    {
    }

    /**
     * 入库前清洗：临时地址返回空串，其余尽量原样保留。
     *
     * <table border="1">
     *   <caption>典型输入</caption>
     *   <tr><th>输入</th><th>输出</th></tr>
     *   <tr><td>资源 key（任意 {@code file.prefix}）</td><td>原样</td></tr>
     *   <tr><td>带 query / fragment 的相对地址</td><td>去掉 query / fragment 后原样（提交前由前端适配层还原 key）</td></tr>
     *   <tr><td>{@code https://thirdwx.qlogo.cn/...}</td><td>原样（第三方外链）</td></tr>
     *   <tr><td>{@code wxfile://tmp_xxx}、{@code http://tmp/x.png}</td><td>{@code ''}（拒绝入库）</td></tr>
     * </table>
     */
    public static String normalize(String raw)
    {
        if (raw == null)
        {
            return "";
        }
        String value = raw.trim();
        if (value.isEmpty() || isTempPath(value))
        {
            return "";
        }
        // 完整 URL 原样保留：外链可能带 query（缩略图参数等），不能截断
        if (isHttpUrl(value))
        {
            return value;
        }
        return stripQuery(value);
    }

    /**
     * 富文本保存前清洗：逐个替换 &lt;img src&gt;，其余 HTML 原样保留。
     * 临时路径会被置成空 src（原本也无法展示），避免脏地址入库。
     */
    public static String normalizeHtml(String html)
    {
        if (html == null || html.isEmpty())
        {
            return html;
        }
        Matcher matcher = IMG_SRC.matcher(html);
        StringBuffer sb = new StringBuffer();
        while (matcher.find())
        {
            String src = matcher.group(3);
            String normalized = normalize(src);
            String replacement = matcher.group(1) + matcher.group(2) + normalized + matcher.group(4);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static boolean isTempPath(String value)
    {
        String lower = value.toLowerCase(Locale.ROOT);
        for (String prefix : TEMP_PREFIXES)
        {
            if (lower.startsWith(prefix))
            {
                return true;
            }
        }
        return false;
    }

    private static boolean isHttpUrl(String value)
    {
        String lower = value.toLowerCase(Locale.ROOT);
        return lower.startsWith("http://") || lower.startsWith("https://");
    }

    private static String stripQuery(String path)
    {
        int query = path.indexOf('?');
        int fragment = path.indexOf('#');
        int cut;
        if (query >= 0 && fragment >= 0)
        {
            cut = Math.min(query, fragment);
        }
        else
        {
            cut = query >= 0 ? query : fragment;
        }
        return cut >= 0 ? path.substring(0, cut) : path;
    }
}
