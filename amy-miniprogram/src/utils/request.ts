/**
 * 网络请求封装（对接真实后端）
 *
 * 后端服务：amy-biz-modules/amy-biz-sun-palace-art-space
 * 统一返回结构：R<T> = { code, msg, data }，成功码 200（Constants.SUCCESS）
 * 鉴权：请求头 Authorization 直接携带登录接口下发的 token 原文
 *       （后端 MinaApiAuthAspect / WechatService 均直接使用请求头内容检索 Redis，不可加 "Bearer " 前缀）
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

/** 登录态失效时清理缓存并跳转登录页 */
function handleUnauthorized(msg: string) {
  clearToken()
  uni.showToast({ title: msg, icon: 'none' })
  setTimeout(() => {
    uni.navigateTo({ url: '/pages/login/index' })
  }, 800)
}

const LOGIN_EXPIRED_RE = /未登录|登录已过期|token|鉴权/i

/**
 * 统一请求方法，直接解析出 R.data
 */
export function request<T = any>(options: RequestOptions): Promise<T> {
  return new Promise((resolve, reject) => {
    const token = getToken()
    const needAuth = options.auth !== false
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
        if (res.statusCode === 401 || res.statusCode === 403) {
          handleUnauthorized('登录已过期，请重新登录')
          reject(new Error('登录已过期，请重新登录'))
          return
        }
        const body = res.data as ApiResult<T>
        if (!body || typeof body.code === 'undefined') {
          reject(new Error('服务响应异常'))
          return
        }
        if (body.code === SUCCESS_CODE) {
          resolve(body.data)
          return
        }
        const msg = body.msg || '请求失败'
        if (body.code === 401 || (needAuth && LOGIN_EXPIRED_RE.test(msg))) {
          handleUnauthorized('登录已过期，请重新登录')
        }
        reject(new Error(msg))
      },
      fail: (err) => {
        reject(new Error(err.errMsg || '网络异常，请稍后重试'))
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
