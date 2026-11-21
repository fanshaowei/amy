/**
 * 用户状态管理
 *
 * 登录流程：uni.login 取 code -> POST /spas/mina/app/auth/login（返回 token + fistTimeLogin）
 * -> GET /spas/mina/app/user/info 取会员信息
 * -> fistTimeLogin=true 时登录页引导用户设置头像/昵称（上传头像 + /reservation/user/update 保存）
 */

import { defineStore } from 'pinia'
import type { User } from '@/types'
import * as userApi from '@/api/user'
import { clearToken, getToken, setToken } from '@/utils/request'
import { syncTokenFromStorage } from '@/utils/loginState'

const STORAGE_KEY = 'reservation_user'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: '',
    user: null as User | null,
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
      } catch (e) {
        console.error('restore user failed', e)
      }
    },

    /**
     * 微信登录：仅携带 jsCode 换取 token 并拉取会员信息。
     * 返回 { user, fistTimeLogin }：
     * fistTimeLogin=true 时由登录页引导用户补全头像/昵称（走 updateProfile 保存）。
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
      return { user, fistTimeLogin }
    },

    /** 修改头像/昵称并同步本地状态 */
    async updateProfile(nickName: string, avatarImgUrl: string) {
      // 对应 POST /spas/mina/app/reservation/user/update（MinaAppController#updateReservationUser）
      await userApi.updateReservationUser({ nickName, avatarImgUrl })
      if (this.user) {
        this.user = { ...this.user, nickname: nickName, avatarImgUrl }
        uni.setStorageSync(STORAGE_KEY, this.user)
      }
    },

    /** 刷新会员信息；silent=true 时失败不弹统一提示（用于页面无感刷新） */
    async fetchProfile(silent = false) {
      // 以缓存 token 为准，避免内存 token 残留导致过期后仍发请求
      if (!getToken()) return null
      const user = await userApi.getProfile(silent)
      this.user = user
      uni.setStorageSync(STORAGE_KEY, user)
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

    /** 退出登录 */
    logout() {
      this.token = ''
      this.user = null
      clearToken()
      uni.removeStorageSync(STORAGE_KEY)
    },
  },
})
