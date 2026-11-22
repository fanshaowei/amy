<script setup lang="ts">
import PageHero from '@/components/PageHero.vue'
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { maskTel } from '@/utils'
import { openLogin, backHomeIfLoginAbandoned } from '@/utils/authGuard'
import AppIcon from '@/components/AppIcon.vue'

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

/** 是否已实名（未登录视为未实名） */
const isVerified = computed(() => !!userStore.user?.isVerified)

/**
 * 首登资料引导：store 已综合「登录态 / 是否忽略过 / 资料是否完整」，
 * 这里只做展示。它是非阻断的，用户可跳过，也可直接点上方会员卡去编辑资料。
 */
const showProfileGuide = computed(() => userStore.loggedIn && userStore.profileGuidePending)

function onGuideGo() {
  // 跳转时不清标记：用户中途返回，下次进「我的」还能看到；
  // 只有在编辑资料页保存成功才持久忽略（stores/user.ts#updateProfile）
  uni.navigateTo({ url: '/pages/edit/index' })
}

function onGuideSkip() {
  userStore.dismissProfileGuide()
}

function onCardClick() {
  if (userStore.loggedIn) {
    uni.navigateTo({ url: '/pages/edit/index' })
  } else {
    // 主动登录入口：记下待授权状态，放弃授权返回时统一回首页
    openLogin()
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

function goVerify() {
  uni.navigateTo({ url: '/pages/verify/index' })
}

/** 退出登录：清除 token 与 reservation_user 缓存 */
function onLogout() {
  uni.showModal({
    title: '退出登录',
    content: '确定要退出当前账号吗？',
    confirmColor: '#f53f3f',
    success: (res) => {
      if (!res.confirm) return
      userStore.logout()
      uni.showToast({ title: '已退出登录', icon: 'none' })
    },
  })
}

/** 清除本地缓存：清空本地存储（token、用户缓存等）并回到首页 */
function onClearCache() {
  uni.showModal({
    title: '清除本地缓存',
    content: '将清除本地的登录状态和缓存数据，确定继续吗？',
    confirmColor: '#f53f3f',
    success: (res) => {
      if (!res.confirm) return
      try {
        uni.clearStorageSync()
      } catch (e) {
        // 清除失败也继续重置内存态
      }
      // 同步重置内存中的登录态，避免页面仍显示已登录
      userStore.logout()
      uni.showToast({ title: '缓存已清除', icon: 'none' })
      setTimeout(() => {
        uni.reLaunch({ url: '/pages/home/index' })
      }, 600)
    },
  })
}

onShow(async () => {
  // 从登录页放弃授权返回 → 统一回首页（本页保持游客可见，作为主动登录入口）
  if (backHomeIfLoginAbandoned()) return
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
    <PageHero title="我的" eyebrow="MY · SUN PALACE" subtitle="个人信息与管理" compact />

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

    <!-- 资料引导：只在资料不完整且未主动忽略时出现，可跳过、不阻断其它操作 -->
    <view v-if="showProfileGuide" class="profile-guide">
      <view class="profile-guide__top">
        <view class="profile-guide__badge">
          <AppIcon name="info" :size="30" color="#f0cf94" />
        </view>
        <view class="profile-guide__text">
          <view class="profile-guide__title">设置头像和昵称</view>
          <view class="profile-guide__desc">让预约信息更好辨认，也可以稍后再设置</view>
        </view>
      </view>
      <view class="profile-guide__actions">
        <text class="profile-guide__skip" @tap="onGuideSkip">暂不设置</text>
        <view class="profile-guide__go" @tap="onGuideGo">去设置</view>
      </view>
    </view>

    <view class="mine__menu">
      <view class="menu-item" @tap="goRecords">
        <view class="menu-item__icon menu-item__icon--gold">
          <AppIcon name="list" :size="32" color="#f0cf94" />
        </view>
        <text class="menu-item__text">预约记录</text>
        <AppIcon class="menu-item__arrow" name="next" :size="24" color="rgba(255, 255, 255, 0.42)" />
      </view>
      <view class="menu-item" @tap="goVerify">
        <view class="menu-item__icon menu-item__icon--jade">
          <AppIcon name="shield" :size="32" color="#8ce0d2" />
        </view>
        <text class="menu-item__text">实名认证</text>
        <text class="menu-item__status" :class="{ ok: isVerified }">{{ isVerified ? '已实名' : '未实名' }}</text>
        <AppIcon class="menu-item__arrow" name="next" :size="24" color="rgba(255, 255, 255, 0.42)" />
      </view>
      <view v-if="userStore.isVerifier" class="menu-item" @tap="goScan">
        <view class="menu-item__icon menu-item__icon--indigo">
          <AppIcon name="scan" :size="32" color="#b9c9ff" />
        </view>
        <text class="menu-item__text">核销</text>
        <AppIcon class="menu-item__arrow" name="next" :size="24" color="rgba(255, 255, 255, 0.42)" />
      </view>
      <view class="menu-item" @tap="goAbout">
        <view class="menu-item__icon menu-item__icon--indigo">
          <AppIcon name="info" :size="32" color="#b9c9ff" />
        </view>
        <text class="menu-item__text">关于我们</text>
        <AppIcon class="menu-item__arrow" name="next" :size="24" color="rgba(255, 255, 255, 0.42)" />
      </view>
      <view class="menu-item" @tap="onClearCache">
        <view class="menu-item__icon menu-item__icon--rose">
          <AppIcon name="trash" :size="32" color="#f4bcd2" />
        </view>
        <text class="menu-item__text">清除本地缓存</text>
        <AppIcon class="menu-item__arrow" name="next" :size="24" color="rgba(255, 255, 255, 0.42)" />
      </view>
    </view>

    <view v-if="userStore.loggedIn" class="logout-btn" @tap="onLogout">退出登录</view>
  </view>
</template>

<style scoped lang="scss">
.mine {
  min-height: 100vh;
  /* 未登录时不渲染「退出登录」，页尾留白由根容器兜底 */
  padding-bottom: $art-page-bottom;
}
.mine__card {
  @include art-card(0.62, 20px, 32rpx);
  /* 不再用负 margin 叠压页头：Hero 与卡片各自独立，留出清晰边界 */
  margin: $art-gap 24rpx 0;
  padding: 40rpx 32rpx;
  display: flex;
  align-items: center;
  gap: 28rpx;
  position: relative;
  /* 会员卡比下方菜单卡亮一档 + 右上暖金光晕，避免两块同色深靛卡贴在一起看不出层次 */
  background-image:
    radial-gradient(circle at 88% 6%, rgba(240, 207, 148, 0.22) 0%, rgba(240, 207, 148, 0) 44%),
    linear-gradient(125deg, rgba(58, 92, 162, 0.86) 0%, rgba(20, 38, 78, 0.9) 64%);
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
  color: $art-text-strong;
}
.mine__meta {
  font-size: 24rpx;
  color: $art-text-dim;
  margin-top: 8rpx;
}
.mine__edit {
  color: $art-accent;
  font-size: 26rpx;
}
.profile-guide {
  @include art-card(0.6, 18px, 24rpx);
  margin: $art-gap 24rpx 0;
  padding: 28rpx 32rpx;
  /* 弱金描边与下方菜单卡拉开层次，避免连续三块同色深靛卡 */
  border: 1rpx solid rgba(240, 207, 148, 0.26);
}
.profile-guide__top {
  display: flex;
  align-items: center;
  gap: 20rpx;
}
.profile-guide__badge {
  width: 56rpx;
  height: 56rpx;
  border-radius: 50%;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(240, 207, 148, 0.14);
}
.profile-guide__text {
  flex: 1;
  min-width: 0;
}
.profile-guide__title {
  font-size: 28rpx;
  font-weight: 500;
  color: $art-text-strong;
}
.profile-guide__desc {
  font-size: 22rpx;
  color: $art-text-dim;
  margin-top: 6rpx;
  line-height: 1.5;
}
.profile-guide__actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 32rpx;
  margin-top: 24rpx;
}
.profile-guide__skip {
  font-size: 24rpx;
  color: $art-text-dim;
  padding: 10rpx 8rpx;
}
.profile-guide__go {
  font-size: 24rpx;
  color: $art-on-gold;
  background: $art-accent;
  padding: 12rpx 36rpx;
  border-radius: 40rpx;
}
.mine__menu {
  @include art-card(0.58, 18px, 32rpx);
  /* 与上方会员卡拉开距离，两块深靛卡不再连成一片 */
  margin: $art-gap 24rpx 0;
  overflow: hidden;
}
.menu-item {
  padding: 32rpx;
  display: flex;
  align-items: center;
  gap: 24rpx;
  border-bottom: 1px solid $art-divider;
  font-size: 30rpx;
  color: $art-text-strong;
}
.menu-item:last-child {
  border-bottom: 0;
}
.menu-item__icon {
  @include art-chip();
  display: flex;
  align-items: center;
  justify-content: center;
  width: 64rpx;
  height: 64rpx;
  border-radius: 22rpx;
  flex-shrink: 0;
}
.menu-item__icon--gold {
  background: rgba(240, 207, 148, 0.14);
}
.menu-item__icon--jade {
  background: rgba(140, 224, 210, 0.13);
}
.menu-item__icon--indigo {
  background: rgba(185, 201, 255, 0.13);
}
.menu-item__icon--rose {
  background: rgba(244, 188, 210, 0.13);
}
.menu-item__text {
  flex: 1;
}
.menu-item__status {
  font-size: 24rpx;
  color: #ff9c9c;
  margin-right: 8rpx;
}
.menu-item__status.ok {
  color: #7fe0a8;
}
.logout-btn {
  background-color: rgba(200,38,76,1)!important;
  margin: $art-gap-lg 24rpx $art-page-bottom;
  @include art-chip();
  color: #ffffff;
  padding: 28rpx;
  border-radius: 56rpx;
  text-align: center;
  font-size: 30rpx;
}
.menu-item__arrow {
  flex-shrink: 0;
  margin-left: 4rpx;
}
</style>
