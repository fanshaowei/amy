import {request} from '@umijs/max';
import {toQueryString} from '@/utils/exportParams';

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
 * 导出文件：查询条件放 **JSON 请求体**。
 *
 * 适用于后端用 `@RequestBody` 接收查询条件的接口（如 spas 业务模块）：
 * ```java
 * @PostMapping("/export")
 * public void export(HttpServletResponse response, @RequestBody XxxReq req)
 * ```
 */
export async function downloadFile(url: string, data: Record<string, unknown>, filename: string) {
    const blob = await request<Blob>(url, {
        method: 'POST',
        data,
        responseType: 'blob'
    });
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
        responseType: 'blob'
    });
    saveBlob(blob, filename);
}
