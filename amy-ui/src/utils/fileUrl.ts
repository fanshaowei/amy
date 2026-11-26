/**
 * 文件地址解析（纯 B：访问地址只在前端这一处决定）
 *
 * 后端所有接口返回的是<b>资源 key</b>，形如 `<file.prefix>/2026/11/26/xxx.jpg`；
 * 前缀由 amy-file 的 Nacos `file.prefix` 决定。这里<b>不识别、也不关心</b>具体前缀：
 * 只要是相对路径就拼上文件访问 base，于是 file.prefix 改成任何值都不用改前端。
 *
 * FILE_BASE 是网关文件路由的同源前缀（Umi dev proxy / nginx 把 /api 转到网关，
 * 网关 /file/** 带 StripPrefix=1 落到 amy-file 的静态映射），与 file.prefix 无关。
 * 返回相对路径而非绝对 URL 是刻意的 —— 换域名时 DB、后端、前端都不用动。
 *
 * 展示与提交是一对逆操作：services 适配层读接口时 resolveFileSrc/resolveRichText，
 * 写接口提交前 toStoredKey/toStoredHtml 还原成 key，保证库里永远不含域名或网关前缀。
 * 绝对 URL（微信头像等第三方外链）一律原样保留，不需要任何域名/前缀白名单。
 */

/** 文件访问 base：与 config/config.ts 的 proxy、nginx 的 location /api/ 对齐 */
const FILE_BASE = '/api/file';
/** 客户端本地临时地址，绝不能当成可访问 URL */
const TEMP_PATTERN = /^(wxfile:|file:|data:|blob:|https?:\/\/tmp\/)/i;

/** 展示用：资源 key -> 同源可访问地址；外链原样、无效地址返回空串；幂等 */
export function resolveFileSrc(raw?: string | null): string {
    if (!raw) return '';
    const value = String(raw).trim();
    if (!value) return '';
    if (TEMP_PATTERN.test(value)) return '';
    if (/^(https?:)?\/\//i.test(value)) return value;
    const path = value.split(/[?#]/)[0] ?? '';
    if (!path.startsWith('/')) return value;
    if (path === FILE_BASE || path.startsWith(FILE_BASE + '/')) return path;
    return FILE_BASE + path;
}

/** 提交用：展示地址 -> 资源 key（resolveFileSrc 的逆操作）；外链与已是 key 的值原样 */
export function toStoredKey(raw?: string | null): string {
    if (!raw) return '';
    const value = String(raw).trim();
    if (!value) return '';
    if (TEMP_PATTERN.test(value)) return '';
    if (/^(https?:)?\/\//i.test(value)) return value;
    const path = value.split(/[?#]/)[0] ?? '';
    return path.startsWith(FILE_BASE + '/') ? path.slice(FILE_BASE.length) : path;
}

/** 富文本展示：逐个替换 <img src>；幂等 */
export function resolveRichText(html?: string | null): string {
    return mapImgSrc(html, resolveFileSrc);
}

/** 富文本提交：逐个还原 <img src> 为资源 key */
export function toStoredHtml(html?: string | null): string {
    return mapImgSrc(html, toStoredKey);
}

function mapImgSrc(html: string | null | undefined, mapper: (src: string) => string): string {
    if (!html) return '';
    return html.replace(
        /(<img\b[^>]*?\bsrc\s*=\s*)(["'])([^"']+)(\2)/gi,
        (_all, before: string, quote: string, src: string) =>
            before + quote + mapper(src) + quote,
    );
}
