<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { uploadAvatar } from '@/api/user'
import type { User } from '@/types'

const userStore = useUserStore()

/** 本地用户缓存 key（与 stores/user.ts 保持一致） */
const STORAGE_KEY = 'reservation_user'
const nickname = ref('')
const avatarUrl = ref('')
const loading = ref(false)

/** 本次会话已上传到后端的头像持久化地址（换头像后重置，避免重复上传） */
const uploadedAvatarUrl = ref('')
const redirect = ref('')

const defaultAvatar = 'https://i.pravatar.cc/120?img=12'

/** 登录成功后回跳来源页 */
function goBack() {
  if (redirect.value) {
    uni.redirectTo({ url: redirect.value })
  } else {
    uni.navigateBack()
  }
}

/** 调起微信登录拿 code（仅小程序端有效） */
async function fetchWxCode(): Promise<string> {
  return new Promise((resolve, reject) => {
    // #ifdef MP-WEIXIN
    uni.login({
      provider: 'weixin',
      success: (res) => {
        if (res.code) resolve(res.code)
        else reject(new Error('未获取到微信登录凭证'))
      },
      fail: (err) => reject(new Error(err.errMsg || '微信登录失败')),
    })
    // #endif
    // #ifndef MP-WEIXIN
    reject(new Error('当前平台不支持微信登录'))
    // #endif
  })
}

/** 选择微信头像（open-type="chooseAvatar"），拿到的是临时路径 */
function onChooseAvatar(e: any) {
  const url = e?.detail?.avatarUrl
  if (!url) return
  avatarUrl.value = url
  // 换了头像，上次上传的持久地址作废，登录时需按新路径重新上传
  uploadedAvatarUrl.value = ''
}

/**
 * 确保头像已上传为持久化地址：
 * chooseAvatar 给的临时路径只在本次会话有效，直接落库必然失效，
 * 必须先调 /spas/mina/app/user/avatar/upload 换成后端文件服务的 URL。
 * 未选头像返回空串（后端首次登录也允许为空）。
 */
async function ensureAvatarUploaded(): Promise<string> {
  if (!avatarUrl.value) return ''
  if (uploadedAvatarUrl.value) return uploadedAvatarUrl.value
  const url = await uploadAvatar(avatarUrl.value)
  uploadedAvatarUrl.value = url
  return url
}

async function onLogin() {
  if (loading.value) return
  loading.value = true
  try {
    // 1. 先上传头像换取持久化地址（失败则阻断登录，避免临时地址落库）
    const avatarImgUrl = await ensureAvatarUploaded()
    // 2. 取微信登录凭证
    const jsCode = await fetchWxCode()

    // 3. 携带持久化头像地址一起授权登录（后端首次登录写入会员表；
    //    老会员由 stores/user.ts 比对后走 /reservation/user/update 同步）
    await userStore.login({
      jsCode,
      nickName: nickname.value.trim() || '微信用户',
      avatarImgUrl,
    })
    uni.showToast({ title: '登录成功', icon: 'success' })
    setTimeout(goBack, 600)
  } catch (e: any) {
    uni.showToast({ title: e.message || '登录失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

onLoad((query) => {
  redirect.value = query?.redirect || ''
  // 预填缓存昵称，避免默认值「微信用户」在老会员重新授权时覆盖原昵称
  try {
    const cached = uni.getStorageSync(STORAGE_KEY) as User | null
    if (cached?.nickname) nickname.value = cached.nickname
  } catch (e) {
    /* ignore */
  }
})
</script>

<template>
  <view class="login">
    <view class="login__logo">🎫</view>
    <view class="login__title">预约门票</view>
    <view class="login__sub">请完成微信授权登录</view>

    <view class="login__avatar">
      <!-- #ifdef MP-WEIXIN -->
      <button class="avatar-btn" open-type="chooseAvatar" @chooseavatar="onChooseAvatar">
        <image class="avatar" :src="avatarUrl || defaultAvatar" mode="aspectFill" />
      </button>
      <!-- #endif -->
      <!-- #ifndef MP-WEIXIN -->
      <image class="avatar" :src="avatarUrl || defaultAvatar" mode="aspectFill" />
      <!-- #endif -->
      <view class="login__avatar-tip">点击选择微信头像</view>
    </view>

    <view class="login__form">
      <view class="item">
        <text class="lbl">昵称</text>
        <input class="val" v-model="nickname" type="nickname" placeholder="请输入昵称" />
      </view>
    </view>

    <view class="login__btn" :class="{ disabled: loading }" @tap="onLogin">
      {{ loading ? '登录中...' : '授权登录' }}
    </view>
    <view class="login__tip">登录即表示同意《用户协议》和《隐私政策》</view>
  </view>
</template>

<style scoped lang="scss">
.login {
  min-height: 100vh;
  background: #fff;
  padding: 40rpx 64rpx;
}
.login__logo {
  width: 160rpx;
  height: 160rpx;
  margin: 48rpx auto 32rpx;
  background: linear-gradient(135deg, #3b7dff, #5b95ff);
  border-radius: 40rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 72rpx;
  box-shadow: 0 16rpx 48rpx rgba(59, 125, 255, 0.3);
}
.login__title {
  text-align: center;
  font-size: 44rpx;
  font-weight: 600;
  color: #1f2329;
  margin-bottom: 16rpx;
}
.login__sub {
  text-align: center;
  font-size: 26rpx;
  color: #86909c;
  margin-bottom: 64rpx;
}
.login__avatar {
  text-align: center;
  margin-bottom: 32rpx;
}
.avatar-btn {
  width: 128rpx;
  height: 128rpx;
  padding: 0;
  margin: 0 auto;
  background: transparent;
  border: none;
  line-height: 1;
}
.avatar-btn::after {
  border: none;
}
.avatar {
  width: 128rpx;
  height: 128rpx;
  border-radius: 50%;
  border: 4rpx solid #f2f3f5;
}
.login__avatar-tip {
  font-size: 22rpx;
  color: #86909c;
  margin-top: 12rpx;
}
.login__form {
  background: #f7f8fa;
  border-radius: 24rpx;
  padding: 0 32rpx;
  margin-bottom: 32rpx;
}
.item {
  padding: 28rpx 0;
  display: flex;
  align-items: center;
  gap: 24rpx;
  border-bottom: 1px solid #ecedf0;
}
.item:last-child {
  border-bottom: 0;
}
.lbl {
  width: 100rpx;
  font-size: 26rpx;
  color: #4e5969;
}
.val {
  flex: 1;
  font-size: 28rpx;
}
.login__btn {
  background: linear-gradient(90deg, #3b7dff, #5b95ff);
  color: #fff;
  padding: 26rpx;
  border-radius: 56rpx;
  text-align: center;
  font-size: 30rpx;
  font-weight: 500;
  box-shadow: 0 8rpx 32rpx rgba(59, 125, 255, 0.35);
}
.login__btn.disabled {
  opacity: 0.6;
}
.login__tip {
  text-align: center;
  font-size: 22rpx;
  color: #86909c;
  margin-top: 32rpx;
  line-height: 1.7;
}
</style>
