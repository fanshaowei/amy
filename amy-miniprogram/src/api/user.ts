/**
 * 用户接口
 *
 * 后端：
 *  - POST /spas/mina/app/auth/login          微信登录换取 token（MinaAppController#login）
 *  - GET  /spas/mina/app/user/info           当前登录会员信息（MinaAppController#getUserInfo）
 *  - POST /spas/mina/app/user/avatar/upload  上传微信头像换持久化地址（MinaAppController#uploadReservationUserAvatar）
 *  - POST /spas/mina/app/reservation/user/update  更新会员昵称/头像（MinaAppController#updateReservationUser）
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
import { BASE_URL, request } from '@/utils/request'

export interface LoginPayload {
  /** 微信登录凭证 code（uni.login 获取） */
  jsCode: string
  /** 微信头像地址（uploadAvatar 换来的持久化 URL） */
  avatarImgUrl?: string
  /** 微信昵称 */
  nickName?: string
}

/**
 * 登录接口响应（对应后端 WechatApiAuthLoginResp）
 * fistTimeLogin=true 表示首次登录（后端已创建会员并写入本次头像/昵称）；
 * false 表示老会员，资料变更需另调 /reservation/user/update 同步。
 */
export interface LoginResult {
  token: string
  fistTimeLogin: boolean
}

/**
 * 微信登录，返回 token 与是否首次登录
 * POST /spas/mina/app/auth/login
 */
export async function login(payload: LoginPayload): Promise<LoginResult> {
  const data = await request<any>({
    url: '/spas/mina/app/auth/login',
    method: 'POST',
    auth: false,
    data: {
      jsCode: payload.jsCode,
      avatarImgUrl: payload.avatarImgUrl || '',
      nickName: payload.nickName || '',
    },
  })
  const token =
    typeof data === 'string' && data
      ? data
      : data && typeof data.token === 'string'
        ? data.token
        : ''
  if (!token) throw new Error('登录响应异常：未获取到 token')
  const fistTimeLogin =
    data && typeof data === 'object' ? data.fistTimeLogin === true : false
  return { token, fistTimeLogin }
}

/**
 * 上传微信头像，返回后端文件服务的持久化访问地址。
 *
 * chooseAvatar 返回的是 wxfile:// / http://tmp/ 临时路径，仅本次会话有效，
 * 必须先上传换成持久 URL，再随 /auth/login 一起提交给后端落库。
 * 该接口无 @MinaApiAuth（网关白名单 /spas/mina/app/**），登录前调用无需 token。
 * POST /spas/mina/app/user/avatar/upload（multipart 表单，字段名 file）
 *
 * 注意：不能走 request() —— uni.uploadFile 需由客户端自动生成
 * multipart/form-data boundary，手动设置 Content-Type 会导致上传失败。
 */
export function uploadAvatar(filePath: string): Promise<string> {
  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: `${BASE_URL}/spas/mina/app/user/avatar/upload`,
      filePath: normalizeAvatarPath(filePath),
      name: 'file', // 与后端 @RequestParam("file") 一致
      success: (res) => {
        try {
          const body =
            typeof res.data === 'string' && res.data ? JSON.parse(res.data) : res.data
          if (res.statusCode === 200 && body && body.code === 200 && body.data) {
            resolve(body.data as string)
          } else {
            reject(new Error(body?.msg || '头像上传失败'))
          }
        } catch (e) {
          reject(new Error('头像上传响应解析失败'))
        }
      },
      fail: (err) => reject(new Error(err.errMsg || '头像上传失败，请检查网络')),
    })
  })
}

/**
 * 规整头像临时文件路径：后端文件服务按扩展名白名单校验，
 * 而微信 chooseAvatar 返回的临时路径往往没有扩展名（如 wxfile://tmp_xxx），
 * 直接上传会被拒。这里复制一份带 .jpg 后缀的文件再上传（仅小程序端）。
 */
function normalizeAvatarPath(filePath: string): string {
  // #ifdef MP-WEIXIN
  if (!/\.(jpg|jpeg|png|gif|bmp|webp)$/i.test(filePath)) {
    try {
      const u = uni as any
      const dest = `${u.env.USER_DATA_PATH}/mina_avatar_${Date.now()}.jpg`
      u.getFileSystemManager().copyFileSync(filePath, dest)
      return dest
    } catch (e) {
      // 复制失败时兜底用原路径，交由后端再判一次
      return filePath
    }
  }
  // #endif
  return filePath
}

/** 更新会员资料入参（对应后端 UpdateReservationUserReq，nickName/avatarImgUrl 均必填） */
export interface UpdateReservationUserPayload {
  nickName: string
  avatarImgUrl: string
}

/**
 * 更新当前会员的昵称/头像（老会员重新授权登录时同步本次选择的资料）
 * POST /spas/mina/app/reservation/user/update
 */
export function updateReservationUser(payload: UpdateReservationUserPayload): Promise<User> {
  return request<User>({
    url: '/spas/mina/app/reservation/user/update',
    method: 'POST',
    data: {
      nickName: payload.nickName,
      avatarImgUrl: payload.avatarImgUrl,
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
