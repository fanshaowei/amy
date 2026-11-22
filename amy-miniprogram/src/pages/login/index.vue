<script setup lang="ts">
import { ref } from 'vue'
import { useUserStore } from '@/stores/user'
import { showError } from '@/utils/request'
import { clearLoginPending, HOME_URL } from '@/utils/authGuard'
import ArtEmblem from '@/components/ArtEmblem.vue'

const userStore = useUserStore()
const loading = ref(false)

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

/**
 * 一键授权登录：只做身份交换，不要求填写任何资料。
 *
 * 微信已回收静默获取头像/昵称的能力（getUserProfile 只返回灰色头像和「微信用户」），
 * 唯一合规路径是用户主动点击 chooseAvatar / type="nickname"；而后端又只在
 * 「首次新建会员」时才写入资料，把资料绑在登录页会造成「每次授权都要重选、
 * 老会员填了也不入库」。因此资料改由「我的 → 编辑资料」按需补全，
 * 首次登录后在「我的」页给一次可跳过的轻引导。
 */
async function onLogin() {
  if (loading.value) return
  loading.value = true
  try {
    const jsCode = await fetchWxCode()
    // 页面只传 jsCode：不收集头像/昵称，API 层补默认占位，后端首次建会员写入
    const { fistTimeLogin } = await userStore.login({ jsCode })
    uni.showToast({
      title: fistTimeLogin ? '登录成功，欢迎首次到访' : '登录成功',
      icon: 'success',
    })
    setTimeout(goBack, 600)
  } catch (e) {
    showError(e, '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <view class="login">
    <view class="login__logo"><ArtEmblem :size="132" /></view>
    <view class="login__title">太阳宫艺术空间</view>
    <view class="login__sub">请完成微信授权登录 · 开启艺术之约</view>

    <view class="login__btn" :class="{ disabled: loading }" @tap="onLogin">
      {{ loading ? '登录中...' : '授权登录' }}
    </view>
    <view class="login__hint">无需填写资料，头像和昵称可稍后在「我的 → 编辑资料」设置</view>
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
  /* 下方不再有头像/昵称表单，留出呼吸感，避免按钮紧贴副标题 */
  margin-bottom: 120rpx;
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
.login__hint {
  text-align: center;
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.62);
  margin-top: 24rpx;
  line-height: 1.6;
}
.login__tip {
  text-align: center;
  font-size: 22rpx;
  color: $art-text-dim;
  margin-top: 20rpx;
  line-height: 1.7;
}
</style>
