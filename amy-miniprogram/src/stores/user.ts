/**
 * 用户状态管理
 *
 * 登录流程：uni.login 取 code -> POST /spas/mina/app/auth/login（只带 jsCode）
 *    （返回 token + fistTimeLogin，后端只在新建会员时写入默认昵称，老会员不覆盖）
 * -> GET /spas/mina/app/user/info 取会员信息。
 *
 * 微信已回收静默获取头像/昵称的能力，登录页不再收集资料；资料不完整时
 * 「我的」页显示一次可跳过的轻引导，由用户到编辑资料页主动选头像/填昵称，
 * 走 POST /spas/mina/app/reservation/user/update 保存。
 */

import { defineStore } from 'pinia'
import type { User } from '@/types'
import * as userApi from '@/api/user'
import { clearToken, getToken, setToken } from '@/utils/request'
import { syncTokenFromStorage } from '@/utils/loginState'

const STORAGE_KEY = 'reservation_user'
/** 「已忽略完善资料引导」标记：用户点过「暂不设置」或成功保存过资料后写入 */
const GUIDE_DISMISSED_KEY = 'spas_profile_guide_dismissed'

function guideDismissed(): boolean {
  try {
    return !!uni.getStorageSync(GUIDE_DISMISSED_KEY)
  } catch (e) {
    return false
  }
}

export const useUserStore = defineStore('user', {
  state: () => ({
    token: '',
    user: null as User | null,
    /** 是否显示「完善头像昵称」引导；由登录态 + 忽略标记 + 资料完整度推导，不缓存草稿 */
    profileGuidePending: false,
  }),

  getters: {
    /**
     * 是否已登录：以 loginState 中的响应式 token 为唯一标准。
     * getToken() 读取的是 Vue ref，因此登录写入 / 退出清理 / 请求层判定过期时，
     * 这个 computed getter 会立即重算（不能改成直接读 uni.getStorageSync，
     * 那样没有响应式依赖，首次求值会被永久缓存）。
     */
    loggedIn: () => !!getToken(),
    /** 是否核销员 */
    isVerifier: (state) => state.user?.type === 2,
    /** 显示昵称 */
    displayName: (state) => state.user?.nickname || '游客',
    /** 会员ID */
    uid: (state) => state.user?.reservationUserId || 0,
    /** 会员手机号 */
    phone: (state) => state.user?.phone || '',
    /** 头像地址 */
    avatar: (state) => state.user?.avatarImgUrl || '',
    /** 资料是否已完善：有头像，且昵称不是登录时写入的默认占位 */
    profileComplete: (state) =>
      !!state.user?.avatarImgUrl &&
      !!state.user?.nickname &&
      state.user.nickname !== userApi.DEFAULT_NICKNAME,
  },

  actions: {
    /** 从本地缓存恢复登录态；同时把外部清缓存后的 storage 状态同步回响应式 token */
    restore() {
      try {
        syncTokenFromStorage()
        const token = getToken()
        const cached = uni.getStorageSync(STORAGE_KEY)
        this.token = token
        if (token && cached) {
          this.user = cached as User
        } else if (!token) {
          this.user = null
        }
        this.refreshProfileGuide()
      } catch (e) {
        console.error('restore user failed', e)
      }
    },

    /**
     * 重算「完善资料」引导是否显示：已登录 + 未主动忽略过 + 资料仍不完整。
     * 只由这三个事实推导，不依赖 fistTimeLogin，也不在页面里存草稿，
     * 因此放弃授权返回、重新授权、切前后台都不会造成「每次都要重选」。
     */
    refreshProfileGuide() {
      this.profileGuidePending = !!getToken() && !guideDismissed() && !this.profileComplete
    },

    /** 用户点「暂不设置」或在编辑资料页成功保存：持久忽略，之后不再提示 */
    dismissProfileGuide() {
      this.profileGuidePending = false
      try {
        uni.setStorageSync(GUIDE_DISMISSED_KEY, 1)
      } catch (e) {
        // storage 写入失败时至少保证本次会话不再提示
      }
    },

    /**
     * 微信登录：只携带 jsCode 换取 token（登录页不再收集头像/昵称）。
     * 后端仅在新建会员时写入默认昵称，老会员保持原资料不变。
     * 返回 { user, fistTimeLogin }，fistTimeLogin 只用于登录成功提示，
     * 前端不应据此上传头像或调用 updateProfile 二次保存。
     */
    async login(payload: userApi.LoginPayload) {
      const { token, fistTimeLogin } = await userApi.login(payload)
      setToken(token)
      this.token = token
      // 资料接口偶发失败不应推翻已成功的登录（token 已到手），
      // 否则界面会停在登录页却已持有有效 token，头像/昵称也永远拉不到。
      // 这里静默降级，页面 onShow 的 fetchProfile 会无感补拉。
      let user: User | null = null
      try {
        user = await userApi.getProfile(true)
      } catch (e) {
        console.warn('[user] 登录后拉取资料失败，稍后自动重试', e)
      }
      if (user) {
        this.user = user
        uni.setStorageSync(STORAGE_KEY, user)
      }
      this.refreshProfileGuide()
      return { user, fistTimeLogin }
    },

    /** 修改头像/昵称并同步本地状态（仅编辑资料页在用户主动保存时调用） */
    async updateProfile(nickName: string, avatarImgUrl: string) {
      // 对应 POST /spas/mina/app/reservation/user/update（MinaAppController#updateReservationUser）
      await userApi.updateReservationUser({ nickName, avatarImgUrl })
      if (this.user) {
        this.user = { ...this.user, nickname: nickName, avatarImgUrl }
        uni.setStorageSync(STORAGE_KEY, this.user)
      }
      // 用户已主动补全资料，引导随即消失（之后重新登录也不会再提示）
      this.dismissProfileGuide()
    },

    /** 刷新会员信息；silent=true 时失败不弹统一提示（用于页面无感刷新） */
    async fetchProfile(silent = false) {
      // 以缓存 token 为准，避免内存 token 残留导致过期后仍发请求
      if (!getToken()) return null
      const user = await userApi.getProfile(silent)
      this.user = user
      uni.setStorageSync(STORAGE_KEY, user)
      // 资料若在别处（管理后台等）已补全，引导自动收敛
      this.refreshProfileGuide()
      return user
    },

    /** 微信手机号授权后绑定手机号 */
    async updatePhone(jsCode: string) {
      const phone = await userApi.bindPhone(jsCode)
      if (this.user) {
        this.user = { ...this.user, phone }
        uni.setStorageSync(STORAGE_KEY, this.user)
      }
      return phone
    },

    /** 退出登录（保留「已忽略引导」标记，避免重新登录后又被提示一次） */
    logout() {
      this.token = ''
      this.user = null
      this.profileGuidePending = false
      clearToken()
      uni.removeStorageSync(STORAGE_KEY)
    },
  },
})
