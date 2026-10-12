<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const binding = ref(false)
const defaultAvatar = 'https://i.pravatar.cc/120?img=12'

onShow(async () => {
  if (!userStore.loggedIn) return
  try {
    await userStore.fetchProfile()
  } catch (e) {
    // 刷新失败不阻塞页面展示
  }
})

/** 微信手机号授权后绑定（open-type="getPhoneNumber"） */
async function onGetPhoneNumber(e: any) {
  const jsCode = e?.detail?.code
  if (!jsCode) {
    uni.showToast({ title: '已取消手机号授权', icon: 'none' })
    return
  }
  binding.value = true
  try {
    const phone = await userStore.updatePhone(jsCode)
    uni.showToast({ title: `已绑定 ${phone}`, icon: 'none' })
  } catch (err: any) {
    uni.showToast({ title: err.message || '绑定失败', icon: 'none' })
  } finally {
    binding.value = false
  }
}
</script>

<template>
  <view class="edit">
    <view class="edit__avatar">
      <image class="avatar" :src="userStore.avatar || defaultAvatar" mode="aspectFill" />
      <text class="hint">头像与昵称由微信授权同步</text>
    </view>

    <view class="form">
      <view class="item">
        <text class="lbl">昵称</text>
        <text class="val">{{ userStore.user?.nickname || '未设置' }}</text>
      </view>
      <view class="item">
        <text class="lbl">手机</text>
        <text class="val">{{ userStore.phone || '未绑定' }}</text>
      </view>
    </view>

    <button
      class="save-btn"
      :class="{ disabled: binding }"
      open-type="getPhoneNumber"
      @getphonenumber="onGetPhoneNumber"
    >
      {{ binding ? '绑定中...' : userStore.phone ? '更换手机号' : '微信一键绑定手机号' }}
    </button>
    <view class="tip">如需修改昵称等资料，请联系场馆工作人员</view>
  </view>
</template>

<style scoped lang="scss">
.edit {
  min-height: 100vh;
  padding: 24rpx;
}
.edit__avatar {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40rpx 0;
}
.avatar {
  width: 140rpx;
  height: 140rpx;
  border-radius: 50%;
  border: 4rpx solid #f2f3f5;
}
.hint {
  font-size: 24rpx;
  color: #86909c;
  margin-top: 16rpx;
}
.form {
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
.save-btn {
  background: linear-gradient(90deg, #3b7dff, #5b95ff);
  color: #fff;
  padding: 26rpx;
  border-radius: 56rpx;
  text-align: center;
  font-size: 30rpx;
  font-weight: 500;
  box-shadow: 0 8rpx 32rpx rgba(59, 125, 255, 0.35);
}
.save-btn.disabled {
  opacity: 0.6;
}
.save-btn::after {
  border: none;
}
.tip {
  text-align: center;
  font-size: 22rpx;
  color: #86909c;
  margin-top: 24rpx;
}
</style>
