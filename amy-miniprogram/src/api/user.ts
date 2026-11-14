/**
 * 用户接口
 *
 * 后端：
 *  - POST /spas/mina/app/auth/login            微信登录换取 token（MinaAppController#login，返回 fistTimeLogin + token）
 *  - POST /spas/mina/app/reservation/user/update  修改会员头像/昵称（MinaAppController#updateReservationUser）
 *  - GET  /spas/mina/app/user/info             当前登录会员信息（MinaAppController#getUserInfo）
 *  - POST /spas/mina/app/user/verify         实名认证（MinaAppController#verifyUser）
 *  - GET  /spas/mina/app/user/followers/get  会员身份+随行人列表（MinaAppController#getReservationUserAndFollowers）
 *  - POST /spas/mina/app/user/follower/add    添加随行人（MinaAppController#addFollower）
 *  - POST /spas/mina/app/user/follower/update 修改随行人（MinaAppController#updateFollower）
 *  - POST /spas/mina/app/user/follower/delete/{id} 删除随行人（MinaAppController#deleteFollower）
 *  - GET  /spas/wechat/user/phone/get/{jsCode} 微信手机号换取并绑定（WechatController#getWechatPhone）
 *
 * 注意：后端 WechatController 已使用 @RequestMapping("/wechat")，该值即为路径前缀，
 * 因此手机号接口需带 /wechat 前缀。
 */

import type { User, UserIdentity } from '@/types'
import type { ApiResult } from '@/types'
import { BASE_URL, getToken, request } from '@/utils/request'

export interface LoginPayload {
  /** 微信登录凭证 code（uni.login 获取） */
  jsCode: string
  /** 微信头像地址 */
  avatarImgUrl?: string
  /** 微信昵称 */
  nickName?: string
}

/** /auth/login 返回结构（对应后端 WechatApiAuthLoginResp） */
export interface LoginResult {
  /** 是否首次登录（true 时小程序跳转编辑头像/昵称页） */
  fistTimeLogin: boolean
  /** 鉴权 token */
  token: string
}

/**
 * 微信登录，返回 fistTimeLogin 与 token
 * POST /spas/mina/app/auth/login
 */
export async function login(payload: LoginPayload): Promise<LoginResult> {
  return request<LoginResult>({
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
 * 修改会员头像/昵称
 * POST /spas/mina/app/reservation/user/update
 */
export async function updateProfile(payload: { nickName: string; avatarImgUrl: string }): Promise<boolean> {
  return request<boolean>({
    url: '/spas/mina/app/reservation/user/update',
    method: 'POST',
    data: {
      nickName: payload.nickName,
      avatarImgUrl: payload.avatarImgUrl,
    },
  })
}

/** 判断是否为本地临时文件路径（微信 chooseAvatar 返回 http://tmp/... 或 wxfile://...） */
export function isLocalTempFile(url: string): boolean {
  if (!url) return false
  return !/^https?:\/\//i.test(url) || /^https?:\/\/tmp\//i.test(url)
}

/**
 * 上传头像到文件服务（后端经 Feign 调 amy-file 落盘），返回可长期访问的 URL。
 * 保存头像前必须先走此接口，否则入库的是本地临时路径。
 * POST /spas/mina/app/avatar/upload（multipart，name=file）
 */
export async function uploadAvatar(filePath: string): Promise<string> {
  return new Promise((resolve, reject) => {
    const token = getToken()
    uni.uploadFile({
      url: `${BASE_URL}/spas/mina/app/avatar/upload`,
      filePath,
      name: 'file',
      header: token ? { Authorization: token } : {},
      success: (res) => {
        try {
          const body = JSON.parse(res.data) as ApiResult<{ name: string; url: string }>
          if (body.code === 200 && body.data?.url) {
            resolve(body.data.url)
          } else {
            reject(new Error(body.msg || '头像上传失败'))
          }
        } catch {
          reject(new Error('头像上传失败'))
        }
      },
      fail: (err) => reject(new Error(err.errMsg || '头像上传失败')),
    })
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

/** 实名认证/随行人表单（对应后端 UserIdentityReq） */
export interface UserIdentityPayload {
  /** 记录ID（修改时必填，新增时不用传） */
  userIdentityId?: number
  name: string
  /** 证件类型 1身份证 2护照 3港澳通行证 4台湾通行证 5军官证 */
  idType: number
  idNum: string
  phone: string
  isFollower?: boolean
}
 
/**
 * 实名认证（本人），成功后会员 is_verified 置为 true
 * POST /spas/mina/app/user/verify
 */
export async function verifyUser(payload: UserIdentityPayload): Promise<boolean> {
  return request<boolean>({
    url: '/spas/mina/app/user/verify',
    method: 'POST',
    data: {
      name: payload.name,
      idType: payload.idType,
      idNum: payload.idNum,
      phone: payload.phone,
    },
  })
}

/**
 * 获取当前会员身份记录（含本人实名记录与全部随行人，用 isFollower 区分）
 * GET /spas/mina/app/user/followers/get
 */
export async function getFollowers(): Promise<UserIdentity[]> {
  const list = await request<UserIdentity[]>({
    url: '/spas/mina/app/user/followers/get',
  })
  return Array.isArray(list) ? list : []
}

/**
 * 添加随行人，返回添加后的全部随行人列表
 * POST /spas/mina/app/user/follower/add
 */
export async function addFollower(payload: UserIdentityPayload): Promise<UserIdentity[]> {
  return request<UserIdentity[]>({
    url: '/spas/mina/app/user/follower/add',
    method: 'POST',
    data: {
      name: payload.name,
      idType: payload.idType,
      idNum: payload.idNum,
      phone: payload.phone,
    },
  })
}

/**
 * 修改随行人（userIdentityId 必填），返回修改后的全部随行人列表
 * POST /spas/mina/app/user/follower/update
 */
export async function updateFollower(payload: UserIdentityPayload): Promise<UserIdentity[]> {
  return request<UserIdentity[]>({
    url: '/spas/mina/app/user/follower/update',
    method: 'POST',
    data: {
      userIdentityId: payload.userIdentityId,
      name: payload.name,
      idType: payload.idType,
      idNum: payload.idNum,
      phone: payload.phone,
      isFollower: payload.isFollower || false
    },
  })
}

/**
 * 删除随行人（软删除），返回删除后的全部随行人列表
 * POST /spas/mina/app/user/follower/delete/{userIdentityId}
 */
export async function deleteFollower(userIdentityId: number): Promise<UserIdentity[]> {
  return request<UserIdentity[]>({
    url: `/spas/mina/app/user/follower/delete/${userIdentityId}`,
    method: 'POST',
  })
}
