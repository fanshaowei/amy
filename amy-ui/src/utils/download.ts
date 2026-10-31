import {request} from '@umijs/max';
import {toQueryString} from '@/utils/exportParams';

/**
 * 导出接口的超时时间（毫秒）
 *
 * 全局 request 配置的 timeout 只有 10 秒（见 app.tsx），正常接口够用，
 * 但导出要先查数据再生成 Excel，数据量大时必然超时，这里单独放宽。
 */
const EXPORT_TIMEOUT = 120_000;

/** 触发浏览器下载 */
const saveBlob = (blob: Blob, filename: string) => {
    const objectUrl = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = objectUrl;
    anchor.download = filename;
    anchor.click();
    URL.revokeObjectURL(objectUrl);
};

/**
 * 识别「导出失败」的响应
 *
 * ## 为什么需要它
 *
 * 导出接口正常返回文件流，但**校验不通过时后端返回的是 JSON**（如会员导出要求先选
 * 注册时间范围），内容为 `{"code":500,"msg":"..."}`。此时：
 * 1. 响应是 200，全局响应拦截器不会报错；
 * 2. 全局拦截器检查的 `response.data.code` 在这里读不到——`responseType: 'blob'`
 *    拿到的是 Blob，没有 code 字段；
 * 3. 若不做识别，浏览器会直接把这个 JSON 存成 .xlsx，用户得到一个打不开的文件。
 *
 * 所以这里按 Content-Type 判断（Blob 会保留响应头的 MIME），
 * 是 JSON 就解析出 msg 抛错，交给页面提示。
 */
const throwErrorIfJson = async (blob: Blob): Promise<void> => {
    if (!blob.type || !blob.type.includes('application/json')) {
        return;
    }
    const text = await blob.text();
    let msg = '导出失败';
    try {
        msg = JSON.parse(text)?.msg || msg;
    } catch {
        // JSON 解析失败时保留默认提示
    }
    throw new Error(msg);
};

/**
 * 导出文件：查询条件放 **JSON 请求体**。
 *
 * 适用于后端用 `@RequestBody` 接收查询条件的接口（如 spas 业务模块）：
 * ```java
 * @PostMapping("/export")
 * public void export(HttpServletResponse response, @RequestBody XxxReq req)
 * ```
 *
 * @throws Error 后端校验不通过时抛出，message 为后端返回的错误提示
 */
export async function downloadFile(url: string, data: Record<string, unknown>, filename: string) {
    const blob = await request<Blob>(url, {
        method: 'POST',
        data,
        responseType: 'blob',
        timeout: EXPORT_TIMEOUT
    });
    await throwErrorIfJson(blob);
    saveBlob(blob, filename);
}

/**
 * 导出文件：查询条件放 **query string**。
 *
 * 适用于后端用「实体形参」接收查询条件的接口（RuoYi 系系统模块）：
 * ```java
 * @PostMapping("/export")
 * public void export(HttpServletResponse response, SysUser user)   // 没有 @RequestBody
 * ```
 *
 * 这种写法下 Spring 走 `ModelAttribute` 绑定，只从 query string / form 请求体取值，
 * **JSON 请求体不参与绑定**，条件会被整体丢弃导致导出全量。
 * 所以这里把参数拼到 URL 上——与列表接口 `GET /xxx/list?a=1` 的绑定方式同源。
 *
 * @see toQueryString 参数展平规则（含 RuoYi params[beginTime] 约定）
 */
export async function downloadFileWithQuery(url: string, data: Record<string, unknown>, filename: string) {
    const query = toQueryString(data);
    const blob = await request<Blob>(`${url}${query ? `?${query}` : ''}`, {
        method: 'POST',
        responseType: 'blob',
        timeout: EXPORT_TIMEOUT
    });
    await throwErrorIfJson(blob);
    saveBlob(blob, filename);
}
