import dayjs from 'dayjs';

/**
 * 清洗「列表查询参数」，使其可以直接作为导出接口的请求体。
 *
 * ## 为什么需要它
 *
 * 列表接口是 GET（参数走 query string），导出接口是 POST（参数走 JSON body），
 * 两条链路对值的序列化方式不同，很容易出现「列表查得到，导出是空的 / 导出的不是当前筛选结果」。
 *
 * 正确做法是：**在 ProTable 的 request 里把处理好的查询参数存进 ref，导出时原样复用**
 * （见 `lastQueryRef` 模式），本函数只负责最后的清洗。
 *
 * ## 不要做的事
 *
 * 不要用 `formRef.current.getFieldsValue()` 现取表单值来拼导出参数：
 * - 它返回的是**表单原始值**，日期类字段是 dayjs 对象；
 * - dayjs 对象进 POST body 会被 `JSON.stringify` 调 `toJSON()` 序列化成
 *   带 Z 的 UTC 串（如 `2026-10-09T16:00:00.000Z`）；
 * - 而列表 query string 里的是 `2026-10-10`（ProTable 已按 dateFormatter 格式化过）；
 * - MySQL 的 `cast('...Z' as datetime)` 会**丢弃 Z** 按本地时间解释，两者相差一整天。
 *
 * ## 本函数做了什么
 *
 * 1. 剔除 `undefined` / `null` / `''` —— 空字符串传下去会让后端多出一个等值条件，查不到数据；
 * 2. 递归清洗嵌套对象（如 RuoYi 约定的 `params.beginTime` / `params.endTime`）；
 * 3. 兜底：把残留的 dayjs 对象格式化成 `YYYY-MM-DD`。
 *
 * @param values ProTable request 收到的查询参数（已去掉 current / pageSize）
 * @returns 可直接作为导出请求体的参数对象
 */
export const pruneQueryParams = (values: Record<string, unknown>): Record<string, unknown> => {
    const result: Record<string, unknown> = {};
    Object.entries(values).forEach(([key, value]) => {
        if (value === undefined || value === null || value === '') {
            return;
        }
        if (dayjs.isDayjs(value)) {
            result[key] = value.format('YYYY-MM-DD');
            return;
        }
        if (Array.isArray(value)) {
            result[key] = value.map(item => (dayjs.isDayjs(item) ? item.format('YYYY-MM-DD') : item));
            return;
        }
        if (typeof value === 'object') {
            const nested = pruneQueryParams(value as Record<string, unknown>);
            if (Object.keys(nested).length) {
                result[key] = nested;
            }
            return;
        }
        result[key] = value;
    });
    return result;
};

/**
 * 把查询参数序列化成 query string，供导出接口拼接在 URL 上。
 *
 * ## 为什么导出要参数走 query string 而不是请求体
 *
 * RuoYi 系后端的导出接口长这样（**注意没有 `@RequestBody`**）：
 *
 * ```java
 * @PostMapping("/export")
 * public void export(HttpServletResponse response, SysUser user)
 * ```
 *
 * 这种写法下 Spring 用 `ModelAttribute` 绑定，数据来源是 `request.getParameterMap()`，
 * 它**只认 query string 和 form-urlencoded 请求体**，`application/json` 的请求体不参与绑定。
 * 所以前端用 POST + JSON body 调导出时，后端实体所有字段全是 null → `selectXxxList(空实体)`
 * → **导出全量数据**，与列表结果对不上。
 *
 * 而列表接口恰好是 `GET /xxx/list?a=1&b=2`（query string 绑定），
 * 让导出也走 query string，两条链路的绑定机制就完全一致了，从根上杜绝「查询导出对不上」。
 *
 * 实测（用户管理，共 5 条数据）：
 * | 传参方式                 | list 基准 | export 结果 |
 * |--------------------------|-----------|-------------|
 * | POST + JSON body（原状） | 1 条      | 5 条（全量）|
 * | POST + query string      | 1 条      | 1 条        |
 *
 * ## 嵌套参数的展平规则
 *
 * RuoYi 的时间区间约定放在 `params` 里（`params.beginTime` / `params.endTime`），
 * Spring 的 `WebDataBinder` 用 `params[beginTime]=xxx` 这种方括号语法绑定到 `Map` 属性上，
 * 因此这里把 `params` 这一层展平成 `params[key]`。
 *
 * @param values 已清洗的查询参数（通常是 `pruneQueryParams(lastQueryRef.current)` 的结果）
 * @returns query string，不含前导 `?`；无有效参数时返回空字符串
 */
export const toQueryString = (values: Record<string, unknown>): string => {
    const searchParams = new URLSearchParams();

    Object.entries(values).forEach(([key, value]) => {
        if (value === undefined || value === null || value === '') {
            return;
        }

        // RuoYi 约定的 params（Map 类型）：展平成 params[beginTime]=...
        if (key === 'params' && typeof value === 'object' && !Array.isArray(value)) {
            Object.entries(value as Record<string, unknown>).forEach(([nestedKey, nestedValue]) => {
                if (nestedValue !== undefined && nestedValue !== null && nestedValue !== '') {
                    searchParams.append(`params[${nestedKey}]`, String(nestedValue));
                }
            });
            return;
        }

        searchParams.append(key, String(value));
    });

    return searchParams.toString();
};
