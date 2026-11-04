/**
 * 用户接口
 *
 * 后端：
 *  - POST /spas/mina/app/auth/login          微信登录换取 token（MinaAppController#login）
 *  - GET  /spas/mina/app/user/info           当前登录会员信息（MinaAppController#getUserInfo）
 *  - GET  /spas/wechat/user/phone/get/{jsCode} 微信手机号换取并绑定（WechatController#getWechatPhone）
 *
 * 注意：后端 WechatController 已使用 @RequestMapping("/wechat")，该值即为路径前缀，
 * 因此手机号接口需带 /wechat 前缀。
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
 * POST /spas/mina/app/auth/login
 */
export async function login(payload: LoginPayload): Promise<string> {
  return request<string>({
    url: '/spas/mina/app/auth/login',
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
 * GET /spas/mina/app/user/info
 */
export async function getProfile(): Promise<User> {
  return request<User>({ url: '/spas/mina/app/user/info' })
}

/**
 * 微信手机号授权后换取手机号并绑定到当前会员
 * GET /spas/wechat/user/phone/get/{jsCode}
 */
export async function bindPhone(jsCode: string): Promise<string> {
  return request<string>({
    url: `/spas/wechat/user/phone/get/${encodeURIComponent(jsCode)}`,
  })
}
