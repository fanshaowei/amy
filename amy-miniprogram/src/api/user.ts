/**
 * 用户接口
 *
 * 后端：
 *  - POST /mina/api/auth/login          微信登录换取 token（WechatController#apiAuth 登录）
 *  - GET  /mina/app/user/info           当前登录会员信息（MinaAppController#userInfo）
 *  - GET  /user/phone/get/{jsCode}      微信手机号换取并绑定（WechatController#getWechatPhone）
 *
 * 注意：后端 WechatController 使用 @RestController("/wechat")，在 Spring 中该值为 Bean 名称
 * 而非路径前缀，因此实际路径不含 /wechat 前缀。若后端修正为 @RequestMapping("/wechat")，
 * 请同步在下方路径补上 /wechat 前缀。
 */

import type { User } from '@/types'
import { request } from '@/utils/request'

export interface LoginPayload {
  /** 微信登录凭证 code（uni.login 获取） */
  jsCode: string
  /** 微信头像地址 */
  avatarImgUrl?: string
  /** 微信昵称 */
  nickName?: string
}

/**
 * 微信登录，返回后端鉴权 token
 * POST /mina/api/auth/login
 */
export async function login(payload: LoginPayload): Promise<string> {
  return request<string>({
    url: '/mina/api/auth/login',
    method: 'POST',
    auth: false,
    data: {
      jsCode: payload.jsCode,
      avatarImgUrl: payload.avatarImgUrl || '',
      nickName: payload.nickName || '',
    },
  })
}

/**
 * 获取当前登录会员信息
 * GET /mina/app/user/info
 */
export async function getProfile(): Promise<User> {
  return request<User>({ url: '/mina/app/user/info' })
}

/**
 * 微信手机号授权后换取手机号并绑定到当前会员
 * GET /user/phone/get/{jsCode}
 */
export async function bindPhone(jsCode: string): Promise<string> {
  return request<string>({
    url: `/user/phone/get/${encodeURIComponent(jsCode)}`,
  })
}
