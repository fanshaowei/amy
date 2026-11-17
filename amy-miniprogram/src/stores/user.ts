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

const STORAGE_KEY = 'reservation_user'

export const useUserStore = defineStore('user', {
  state: () => ({
    loggedIn: false,
    token: '',
    user: null as User | null,
  }),

  getters: {
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
    /** 从本地缓存恢复登录态 */
    restore() {
      try {
        const token = getToken()
        const cached = uni.getStorageSync(STORAGE_KEY)
        if (token && cached) {
          this.loggedIn = true
          this.token = token
          this.user = cached as User
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
      const user = await userApi.getProfile()
      this.user = user
      this.loggedIn = true
      uni.setStorageSync(STORAGE_KEY, user)
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
      if (!this.token) return null
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
      this.loggedIn = false
      this.token = ''
      this.user = null
      clearToken()
      uni.removeStorageSync(STORAGE_KEY)
    },
  },
})
