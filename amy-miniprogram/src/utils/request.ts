/**
 * 网络请求封装（对接真实后端）
 *
 * 后端服务：amy-biz-modules/amy-biz-sun-palace-art-space
 * 统一返回结构：R<T> = { code, msg, data }，成功码 200（Constants.SUCCESS）
 * 鉴权：请求头 Authorization 直接携带登录接口下发的 token 原文
 *       （后端 MinaApiAuthAspect / WechatService 均直接使用请求头内容检索 Redis，不可加 "Bearer " 前缀）
 *
 * 统一异常处理：所有失败（HTTP 错误、业务码非 200、网关限流、网络失败）都会
 * 优先使用后端返回的 msg 构造 RequestError，并由本层统一弹一次 toast；
 * 页面 catch 里统一调用 showError(e, '兜底文案') 即可，不会重复弹窗。
 * 需要静默的接口传 silent: true，由页面自行决定是否提示。
 */

import type { ApiResult, TableDataInfo } from '@/types'

/** 登录 token 缓存 key（与 stores/user.ts 保持一致） */
export const TOKEN_KEY = 'spas_token'

/**
 * 后端接口地址（统一走网关）
 * - 本地联调：网关端口 18080（见 amy-gateway bootstrap 配置），业务服务端口 9401
 * - 正式环境：需替换为 HTTPS 域名，并在微信公众平台「开发管理 - 服务器域名」中配置为 request 合法域名
 * - 若网关路由为该模块配置了前缀 / StripPrefix，请在此处同步调整
 */
export const BASE_URL = 'http://localhost:18080'

/** 后端 R 的成功码 */
const SUCCESS_CODE = 200

export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'DELETE'

export interface RequestOptions {
  /** 接口路径，如 /mina/app/user/info */
  url: string
  method?: HttpMethod
  data?: Record<string, any>
  header?: Record<string, string>
  /** 是否需要登录态，默认 true；登录接口传 false */
  auth?: boolean
  /**
   * 鉴权失败时是否自动跳转登录页，默认 true。
   * 首页等「游客可见」的展示型接口传 false，避免未登录用户被强制踢到登录页。
   */
  skipAuthRedirect?: boolean
  /**
   * 关闭统一错误提示，默认 false。
   * 用于「失败可忽略」的后台型请求（如首页预约动态刷新、登录时的资料同步），
   * 页面仍可用 showError() 自行决定是否提示。
   */
  silent?: boolean
  /** 后端未返回 msg 时的兜底提示文案 */
  errorMessage?: string
}

export function getToken(): string {
  return uni.getStorageSync(TOKEN_KEY) || ''
}

export function setToken(token: string) {
  uni.setStorageSync(TOKEN_KEY, token)
}

export function clearToken() {
  uni.removeStorageSync(TOKEN_KEY)
}

/**
 * 统一异常对象：message 恒为「用户可读文案」（优先后端 msg），
 * 同时保留业务码与 HTTP 状态码，便于页面按码做特殊处理。
 */
export class RequestError extends Error {
  /** 后端业务码，如 500 / 429 / 401 */
  code?: number
  /** HTTP 状态码 */
  statusCode?: number
  /** 统一拦截层已弹过提示，页面无需重复弹 */
  notified = false

  constructor(message: string, init?: { code?: number; statusCode?: number }) {
    super(message)
    this.name = 'RequestError'
    this.code = init?.code
    this.statusCode = init?.statusCode
  }
}

/** 同一条消息短时间内只弹一次：页面并发请求同时失败时不刷屏 */
let lastNotify = { msg: '', at: 0 }

/** 统一错误提示（微信 toast 对超长文案会截断，这里先做规整） */
export function notifyError(msg: string) {
  const text = (msg || '请求失败').trim().slice(0, 40)
  const now = Date.now()
  if (lastNotify.msg === text && now - lastNotify.at < 1200) return
  lastNotify = { msg: text, at: now }
  uni.showToast({ title: text, icon: 'none', duration: 2500 })
}

/**
 * 页面统一错误出口：在 catch 中调用即可。
 * - 请求层已弹过（notified=true）→ 跳过，避免出现两次 toast；
 * - 请求用了 silent、或错误来自本地逻辑 → 这里兜底弹一次，文案优先用后端 msg。
 */
export function showError(e: unknown, fallback = '请求失败') {
  const err = e as RequestError | null
  if (err && typeof err === 'object' && err.notified) return
  const msg = (e as Error)?.message || fallback
  notifyError(msg)
}

/** 构造并 reject 一个已统一提示过的异常 */
function rejectWith(
  reject: (reason?: any) => void,
  err: RequestError,
  silent: boolean
) {
  if (!silent) {
    err.notified = true
    notifyError(err.message)
  }
  reject(err)
}

/** 登录态失效时清理缓存并跳转登录页 */
function handleUnauthorized(msg: string) {
  clearToken()
  notifyError(msg)
  setTimeout(() => {
    uni.navigateTo({ url: '/pages/login/index' })
  }, 800)
}

const LOGIN_EXPIRED_RE = /未登录|登录已过期|token|鉴权/i

/** 网关/服务端返回非 R 结构（HTML、纯文本）时，按 HTTP 状态码给出可读文案 */
function httpErrorMessage(statusCode: number): string {
  if (statusCode === 429) return '操作过于频繁，请稍候再试'
  if (statusCode >= 500) return '服务暂时不可用，请稍后重试'
  if (statusCode === 404) return '接口不存在，请联系管理员'
  return `请求失败（${statusCode}）`
}

/** 微信 fail 回调里的 errMsg 是英文原始串（request:fail timeout 等），转成中文 */
function networkErrorMessage(errMsg?: string): string {
  if (!errMsg) return '网络异常，请稍后重试'
  if (/timeout/i.test(errMsg)) return '请求超时，请稍后重试'
  if (/abort/i.test(errMsg)) return '请求已取消'
  return '网络异常，请检查网络后重试'
}

/** uni.request 的 data 可能是 JSON 字符串（网关直接写回时常见），统一解析成对象 */
function parseBody<T>(raw: any): ApiResult<T> | null {
  if (raw && typeof raw === 'object') return raw as ApiResult<T>
  if (typeof raw === 'string' && raw.trim()) {
    try {
      const parsed = JSON.parse(raw)
      return parsed && typeof parsed === 'object' ? (parsed as ApiResult<T>) : null
    } catch (e) {
      return null
    }
  }
  return null
}

/**
 * 供 uni.uploadFile 等不走 request() 的通道复用：
 * 按同一套规则解析响应并生成「已提示过」的异常。
 */
export function responseError(body: any, statusCode: number, fallback = '请求失败'): RequestError {
  const parsed = parseBody(body)
  const err = new RequestError(
    (parsed && typeof parsed.msg === 'string' && parsed.msg) ||
      (statusCode >= 400 ? httpErrorMessage(statusCode) : fallback),
    { code: parsed?.code, statusCode }
  )
  err.notified = true
  notifyError(err.message)
  return err
}

/** 同上，供 uni.uploadFile 的 fail 回调复用（无 HTTP 响应） */
export function networkError(errMsg?: string, fallback = '网络异常，请稍后重试'): RequestError {
  const err = new RequestError(errMsg ? networkErrorMessage(errMsg) : fallback)
  err.notified = true
  notifyError(err.message)
  return err
}

/**
 * 统一请求方法，直接解析出 R.data
 */
export function request<T = any>(options: RequestOptions): Promise<T> {
  return new Promise((resolve, reject) => {
    const token = getToken()
    const needAuth = options.auth !== false
    const skipAuthRedirect = options.skipAuthRedirect === true
    const silent = options.silent === true
    uni.request({
      url: BASE_URL + options.url,
      method: options.method || 'GET',
      data: options.data,
      header: {
        'Content-Type': 'application/json',
        ...(needAuth && token ? { Authorization: token } : {}),
        ...(options.header || {}),
      },
      success: (res) => {
        const body = parseBody<T>(res.data)
        const statusCode = res.statusCode

        // 1. 鉴权类 HTTP 状态：固定提示 + 清理登录态
        if (statusCode === 401 || statusCode === 403) {
          const msg = '登录已过期，请重新登录'
          const err = new RequestError(msg, { code: statusCode, statusCode })
          err.notified = true
          if (!skipAuthRedirect) {
            handleUnauthorized(msg)
          } else if (!silent) {
            notifyError(msg)
          }
          reject(err)
          return
        }

        // 2. 拿不到标准 R 结构：网关限流页 / 服务异常 HTML / 空响应
        if (!body || typeof body.code === 'undefined') {
          rejectWith(
            reject,
            new RequestError(
              statusCode >= 400
                ? httpErrorMessage(statusCode)
                : options.errorMessage || '服务响应异常',
              { statusCode }
            ),
            silent
          )
          return
        }

        if (body.code === SUCCESS_CODE) {
          resolve(body.data)
          return
        }

        // 3. 业务失败：优先使用后端返回的 msg（限流、参数校验、业务规则等）
        const msg = body.msg || options.errorMessage || '请求失败'
        const err = new RequestError(msg, { code: body.code, statusCode })
        if (
          !skipAuthRedirect &&
          (body.code === 401 || (needAuth && LOGIN_EXPIRED_RE.test(msg)))
        ) {
          err.notified = true
          handleUnauthorized('登录已过期，请重新登录')
          reject(err)
          return
        }
        rejectWith(reject, err, silent)
      },
      fail: (err: any) => {
        rejectWith(
          reject,
          new RequestError(networkErrorMessage(err?.errMsg), {}),
          silent
        )
      },
    })
  })
}

/**
 * 分页查询：自动展开后端 TableDataInfo（data.rows / data.total）
 */
export async function requestPage<T>(
  options: RequestOptions & { pageNum?: number; pageSize?: number }
): Promise<TableDataInfo<T>> {
  const { pageNum = 1, pageSize = 100, ...rest } = options
  const data = await request<TableDataInfo<T>>({
    ...rest,
    data: { ...(rest.data || {}), pageNum, pageSize },
  })
  return {
    total: data?.total || 0,
    rows: data?.rows || [],
  }
}
