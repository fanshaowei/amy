/**
 * 用户接口
 *
 * 后端：
 *  - POST /spas/mina/app/auth/login          微信登录换取 token（MinaAppController#login，只负责身份）
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
import { BASE_URL, networkError, request, responseError } from '@/utils/request'

/**，
 * 登录接口只传 jsCode，后端首次建会员时由本常量占位；
 * 用户随后在「我的 → 编辑资料」主动填写真实昵称。
 */
export const DEFAULT_NICKNAME = '微信用户'

export interface LoginPayload {
  /** 微信登录凭证 code（uni.login 获取） */
  jsCode: string
  /**
   * 兼容后端保留字段：登录页不再传头像。
   * 微信 chooseAvatar 只能由用户主动点击，且后端只在首次建会员时写入，
   * 因此资料统一放到编辑资料页（/reservation/user/update）维护。
   */
  avatarImgUrl?: string
  /** 兼容后端保留字段：登录页不再传昵称，缺省由 DEFAULT_NICKNAME 占位 */
  nickName?: string
}

/**
 * 登录接口响应（对应后端 WechatApiAuthLoginResp）
 * fistTimeLogin=true 仅表示「本次新建了会员记录」，后端写入的是默认昵称 + 空头像；
 * false 表示老会员，登录接口不会覆盖其资料，前端也不应再上传头像或调 update 二次保存。
 * 头像/昵称的唯一主动更新入口是编辑资料页（/reservation/user/update）。
 * 注意：后端字段名拼写即为 fistTimeLogin（少一个 r），此处保持一致。
 */
export interface LoginResult {
  token: string
  fistTimeLogin: boolean
}

/**
 * 微信登录，返回 token 与是否首次登录
 * POST /spas/mina/app/auth/login
 *
 * 登录只做身份交换，不再收集头像/昵称：
 * 微信无法静默获取真实资料（只能由用户主动 chooseAvatar / type="nickname"），
 * 而后端又只在首次建会员时写入，导致每次重新授权都要重填且老会员填了也不入库。
 * 页面只传 jsCode；本层保留后端字段（头像恒为空串、昵称用 DEFAULT_NICKNAME 占位），
 * 真实资料由编辑资料页按需补全。
 */
export async function login(payload: LoginPayload): Promise<LoginResult> {
  // 兼容后端 data 直接是 token 字符串的历史情况
  const data = await request<LoginResult | string>({
    url: '/spas/mina/app/auth/login',
    method: 'POST',
    auth: false,
    data: {
      jsCode: payload.jsCode,
      avatarImgUrl: payload.avatarImgUrl || '',
      nickName: payload.nickName || DEFAULT_NICKNAME,
    },
  })
  const token = typeof data === 'string' ? data : data?.token || ''
  if (!token) throw new Error('登录响应异常：未获取到 token')
  const fistTimeLogin =
    typeof data === 'object' && data !== null ? data.fistTimeLogin === true : false
  return { token, fistTimeLogin }
}

/**
 * 判断头像地址是否为「微信 chooseAvatar 返回的本地临时路径」。
 *
 * 临时路径（wxfile://tmp_xxx、http://tmp/xxx、file://...）仅本次会话有效，
 * 直接入库会导致下次打开小程序头像空白；只有这种路径才需要上传文件服务换持久 URL。
 * 文件服务的绝对 http(s) 地址、以及 /upload/... 这类服务端相对路径都视为持久地址，
 * 直接复用、不重复上传（也不会把相对路径当本地文件误传）。
 */
export function isLocalTempFile(url: string): boolean {
  if (!url) return false
  if (/^https?:\/\//i.test(url)) return /^https?:\/\/tmp\//i.test(url)
  if (url.startsWith('/')) return false
  return true
}

/**
 * 上传微信头像，返回后端文件服务的持久化访问地址。
 *
 * chooseAvatar 返回的是 wxfile:// / http://tmp/ 临时路径，仅本次会话有效，
 * 必须先上传换成持久 URL 才能入库。
 * 登录流程不再使用本接口；只有编辑资料页重新选头像时才上传，
 * 随后调 /reservation/user/update 保存新地址。
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
            // 上传通道不走 request()，这里复用同一套异常解析，保证后端 msg 不丢失
            reject(responseError(res.data, res.statusCode, '头像上传失败'))
          }
        } catch (e) {
          reject(responseError(null, res.statusCode, '头像上传响应解析失败'))
        }
      },
      fail: (err) => reject(networkError(err?.errMsg, '头像上传失败，请检查网络')),
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
 * 更新当前会员的昵称/头像
 * POST /spas/mina/app/reservation/user/update
 *
 * 注意：后端返回的是 R<ReservationUser>**数据库实体**（含 token/status、无 idType），
 * 与 /user/info 返回的 MinaReservationUserInfoResp 不是同一套结构，
 * 不能当作前端 User 使用。这里只返回 void，本地会员信息由调用方合并或重新拉取。
 *
 * 仅由「编辑资料页」在用户主动保存时调用；登录流程不再调用本接口做二次保存。
 *
 * @param silent 后台无感同步时传 true 不弹统一提示
 */
export async function updateReservationUser(
  payload: UpdateReservationUserPayload,
  silent = false
): Promise<void> {
  await request<unknown>({
    url: '/spas/mina/app/reservation/user/update',
    method: 'POST',
    silent: silent || undefined,
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
export async function getProfile(silent = false): Promise<User> {
  return request<User>({ url: '/spas/mina/app/user/info', silent: silent || undefined })
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
      // 该接口固定用于修改随行人，后端 updateFollower 也会强制 setIsFollower(true)
      isFollower: payload.isFollower ?? true,
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
