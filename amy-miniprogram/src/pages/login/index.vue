<script setup lang="ts">
import { ref } from 'vue'
import { useUserStore } from '@/stores/user'
import { uploadAvatar } from '@/api/user'
import { showError } from '@/utils/request'
import { clearLoginPending, HOME_URL } from '@/utils/authGuard'
import ArtEmblem from '@/components/ArtEmblem.vue'

const userStore = useUserStore()

const nickname = ref('')
const avatarUrl = ref('')
const loading = ref(false)

/** 登录接口返回 fistTimeLogin=true 后进入完善资料步骤，此时才展示头像/昵称编辑 */
const needProfile = ref(false)

/** 本次会话已上传到后端的头像持久化地址（换头像后重置，避免重复上传） */
const uploadedAvatarUrl = ref('')

const defaultAvatar = 'https://i.pravatar.cc/120?img=12'

/**
 * 登录成功后统一回首页（不再按来源页 redirect 回跳）。
 * 用户放弃授权直接返回时，由来源页的 requireLogin() 守卫统一 switchTab 回首页。
 */
function goBack() {
  clearLoginPending()
  uni.switchTab({ url: HOME_URL })
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
    // 1. 取微信登录凭证（首次进入只授权，不携带头像/昵称）
    const jsCode = await fetchWxCode()

    // 2. 授权登录；fistTimeLogin=true 表示首次登录，需要引导用户设置头像/昵称
    const { fistTimeLogin } = await userStore.login({ jsCode })

    if (fistTimeLogin) {
      // 预填后端默认资料；头像已是持久化地址，无需重复上传
      const user = userStore.user
      nickname.value = user?.nickname || ''
      avatarUrl.value = user?.avatarImgUrl || ''
      uploadedAvatarUrl.value = avatarUrl.value
      needProfile.value = true
      uni.showToast({ title: '首次登录，请完善头像和昵称', icon: 'none' })
      return
    }

    uni.showToast({ title: '登录成功', icon: 'success' })
    setTimeout(goBack, 600)
  } catch (e) {
    showError(e, '登录失败')
  } finally {
    loading.value = false
  }
}

/** 完善资料步骤：上传头像 + 保存昵称，然后进入应用 */
async function onSaveProfile() {
  if (loading.value) return
  loading.value = true
  try {
    // 头像上传为持久化地址（后端首次登录可能为空，允许不选）
    const avatarImgUrl = await ensureAvatarUploaded()
    await userStore.updateProfile(nickname.value.trim() || '微信用户', avatarImgUrl)
    uni.showToast({ title: '登录成功', icon: 'success' })
    setTimeout(goBack, 600)
  } catch (e) {
    showError(e, '保存失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <view class="login">
    <view class="login__logo"><ArtEmblem :size="132" /></view>
    <view class="login__title">太阳宫艺术空间</view>
    <view class="login__sub">
      {{ needProfile ? '请设置您的头像和昵称' : '请完成微信授权登录 · 开启艺术之约' }}
    </view>

    <!-- 首次登录（fistTimeLogin=true）才展示头像/昵称编辑 -->
    <template v-if="needProfile">
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
          <input placeholder-class="art-ph" class="val" v-model="nickname" type="nickname" placeholder="请输入昵称" />
        </view>
      </view>
    </template>

    <view class="login__btn" :class="{ disabled: loading }" @tap="needProfile ? onSaveProfile() : onLogin()">
      {{ loading ? (needProfile ? '保存中...' : '登录中...') : (needProfile ? '保存' : '授权登录') }}
    </view>
    <view class="login__tip">登录即表示同意《用户协议》和《隐私政策》</view>
  </view>
</template>

<style scoped lang="scss">
.login {
  min-height: 100vh;
  /* 品牌登录页整片深靛，和首页 Hero / 项目海报卡同一套底色 */
  background-color: #101f3f;
  background-image:
    radial-gradient(circle at 88% 6%, rgba(240, 207, 148, 0.2) 0%, rgba(240, 207, 148, 0) 40%),
    radial-gradient(circle at 4% 78%, rgba(31, 158, 154, 0.22) 0%, rgba(31, 158, 154, 0) 46%),
    linear-gradient(165deg, #14264c 0%, #101f3f 58%, #122b49 100%);
  padding: 40rpx 64rpx;
}
.login__logo {
  position: relative;
  width: 172rpx;
  height: 172rpx;
  margin: 48rpx auto 32rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: rgba(36, 58, 114, 0.86);
  background-image: linear-gradient(135deg, rgba(32, 51, 106, 0.94) 0%, rgba(31, 138, 132, 0.82) 100%);
  box-shadow: 0 16rpx 40rpx rgba(28, 52, 108, 0.28), inset 0 1rpx 0 rgba(255, 255, 255, 0.22);
}
.login__logo::after {
  content: '';
  position: absolute;
  left: -10rpx;
  right: -10rpx;
  top: -10rpx;
  bottom: -10rpx;
  border-radius: 50%;
  border: 1rpx solid rgba(200, 163, 92, 0.42);
}
.login__title {
  text-align: center;
  font-size: 44rpx;
  font-weight: 600;
  color: $art-text-strong;
  margin-bottom: 16rpx;
}
.login__sub {
  text-align: center;
  font-size: 26rpx;
  color: $art-text-dim;
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
  border: 3rpx solid rgba(240, 207, 148, 0.62);
  box-shadow: 0 8rpx 24rpx rgba(28, 44, 90, 0.18);
}
.login__avatar-tip {
  font-size: 22rpx;
  color: $art-text-dim;
  margin-top: 12rpx;
}
.login__form {
  @include art-card(0.6, 18px, 24rpx);
  padding: 0 32rpx;
  margin-bottom: 32rpx;
}
.item {
  padding: 28rpx 0;
  display: flex;
  align-items: center;
  gap: 24rpx;
  border-bottom: 1px solid $art-divider;
}
.item:last-child {
  border-bottom: 0;
}
.lbl {
  width: 100rpx;
  font-size: 26rpx;
  color: $art-text;
}
.val {
  flex: 1;
  font-size: 28rpx;
}
.login__btn {
  @include art-gold-button();
  
  padding: 26rpx;
  border-radius: 56rpx;
  text-align: center;
  font-size: 30rpx;
  font-weight: 500;
}
.login__btn.disabled {
  opacity: 0.6;
}
.login__tip {
  text-align: center;
  font-size: 22rpx;
  color: $art-text-dim;
  margin-top: 32rpx;
  line-height: 1.7;
}
</style>
