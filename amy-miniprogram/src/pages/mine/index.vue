<script setup lang="ts">
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { maskTel } from '@/utils'

const userStore = useUserStore()

const nickname = computed(() =>
  userStore.loggedIn ? userStore.user?.nickname || '微信用户' : '点击登录'
)
const meta = computed(() => {
  if (!userStore.loggedIn) return '登录后享受完整服务'
  const role = userStore.isVerifier ? '核销员' : '普通用户'
  const tel = userStore.phone ? maskTel(userStore.phone) : '未绑定'
  return `${role} · 手机 ${tel}`
})

const defaultAvatar = 'https://i.pravatar.cc/120?img=12'

function onCardClick() {
  if (userStore.loggedIn) {
    uni.navigateTo({ url: '/pages/edit/index' })
  } else {
    uni.navigateTo({ url: '/pages/login/index' })
  }
}

function goRecords() {
  uni.switchTab({ url: '/pages/records/index' })
}

function goScan() {
  uni.navigateTo({ url: '/pages/scan/index' })
}

function goAbout() {
  uni.navigateTo({ url: '/pages/about/index' })
}

onShow(async () => {
  if (!userStore.loggedIn) return
  try {
    await userStore.fetchProfile()
  } catch (e) {
    // 刷新失败不阻塞页面展示
  }
})
</script>

<template>
  <view class="mine">
    <view class="mine__header" />

    <view class="mine__card" @tap="onCardClick">
      <image
        class="mine__avatar"
        :src="userStore.avatar || defaultAvatar"
        mode="aspectFill"
      />
      <view class="mine__info">
        <view class="mine__nick">{{ nickname }}</view>
        <view class="mine__meta">{{ meta }}</view>
      </view>
      <text class="mine__edit">编辑 ›</text>
    </view>

    <view class="mine__menu">
      <view class="menu-item" @tap="goRecords">
        <text class="menu-item__icon">▤</text>
        <text class="menu-item__text">预约记录</text>
        <text class="menu-item__arrow">›</text>
      </view>
      <view v-if="userStore.isVerifier" class="menu-item" @tap="goScan">
        <text class="menu-item__icon">⌖</text>
        <text class="menu-item__text">核销</text>
        <text class="menu-item__arrow">›</text>
      </view>
      <view class="menu-item" @tap="goAbout">
        <text class="menu-item__icon">ⓘ</text>
        <text class="menu-item__text">关于我们</text>
        <text class="menu-item__arrow">›</text>
      </view>
    </view>
  </view>
</template>

<style scoped lang="scss">
.mine {
  min-height: 100vh;
}
.mine__header {
  height: 200rpx;
  background: linear-gradient(180deg, #3b7dff 0%, #5b95ff 100%);
}
.mine__card {
  background: #fff;
  margin: -100rpx 24rpx 0;
  border-radius: 32rpx;
  padding: 40rpx 32rpx;
  display: flex;
  align-items: center;
  gap: 28rpx;
  box-shadow: 0 8rpx 32rpx rgba(31, 35, 41, 0.06);
  position: relative;
}
.mine__avatar {
  width: 120rpx;
  height: 120rpx;
  border-radius: 50%;
  flex-shrink: 0;
}
.mine__info {
  flex: 1;
  min-width: 0;
}
.mine__nick {
  font-size: 34rpx;
  font-weight: 600;
  color: #1f2329;
}
.mine__meta {
  font-size: 24rpx;
  color: #86909c;
  margin-top: 8rpx;
}
.mine__edit {
  color: #3b7dff;
  font-size: 26rpx;
}
.mine__menu {
  background: #fff;
  margin: 24rpx;
  border-radius: 32rpx;
  overflow: hidden;
  box-shadow: 0 4rpx 24rpx rgba(31, 35, 41, 0.04);
}
.menu-item {
  padding: 32rpx;
  display: flex;
  align-items: center;
  gap: 24rpx;
  border-bottom: 1px solid #f2f3f5;
  font-size: 30rpx;
  color: #1f2329;
}
.menu-item:last-child {
  border-bottom: 0;
}
.menu-item__icon {
  width: 48rpx;
  font-size: 40rpx;
  color: #3b7dff;
  text-align: center;
}
.menu-item__text {
  flex: 1;
}
.menu-item__arrow {
  color: #c9cdd4;
  font-size: 28rpx;
}
</style>
