/**
 * 用户状态管理
 *
 * 登录流程：uni.login 取 code -> POST /mina/app/auth/login（返回 fistTimeLogin + token）
 * -> fistTimeLogin=true 跳编辑头像/昵称页，false 进首页 -> GET /mina/app/user/info 取会员信息
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
     * 微信登录：换取 token 并拉取会员信息。
     * 首次登录（fistTimeLogin=true）：后端已创建会员并写入本次头像/昵称，无需再更新；
     * 老会员（fistTimeLogin=false）：本次提交的头像/昵称与库中不一致时，
     * 走 /reservation/user/update 同步，同步失败不阻断登录（下次登录自动重试）。
     */
    async login(payload: userApi.LoginPayload) {
      const { token, fistTimeLogin } = await userApi.login(payload)
      setToken(token)
      this.token = token
      let user = await userApi.getProfile()
      if (!fistTimeLogin) {
        const nickName = (payload.nickName || '').trim()
        const avatarImgUrl = payload.avatarImgUrl || ''
        const avatarChanged = !!avatarImgUrl && avatarImgUrl !== user.avatarImgUrl
        const nickChanged = !!nickName && nickName !== user.nickname
        if (avatarChanged || nickChanged) {
          try {
            user =
              (await userApi.updateReservationUser({
                nickName: nickName || user.nickname || '',
                avatarImgUrl: avatarImgUrl || user.avatarImgUrl || '',
              })) || user
          } catch (e) {
            console.warn('sync reservation user profile failed', e)
          }
        }
      }
      this.user = user
      this.loggedIn = true
      uni.setStorageSync(STORAGE_KEY, user)
      return user
    },

    /** 修改头像/昵称并同步本地状态 */
    async updateProfile(nickName: string, avatarImgUrl: string) {
      await userApi.updateProfile({ nickName, avatarImgUrl })
      if (this.user) {
        this.user = { ...this.user, nickname: nickName, avatarImgUrl }
        uni.setStorageSync(STORAGE_KEY, this.user)
      }
    },

    /** 刷新会员信息 */
    async fetchProfile() {
      if (!this.token) return null
      const user = await userApi.getProfile()
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
