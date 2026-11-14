<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow, onPullDownRefresh } from '@dcloudio/uni-app'
import type { Activity, NextDayReservation } from '@/types'
import { getActivities } from '@/api/activity'
import { getNextDayReservations } from '@/api/reserve'
import { useUserStore } from '@/stores/user'
import { maskName } from '@/utils'
import { showError } from '@/utils/request'
import ActivityCard from '@/components/ActivityCard.vue'
import EmptyState from '@/components/EmptyState.vue'

const userStore = useUserStore()
const activities = ref<Activity[]>([])
const loading = ref(true)

/* 首页「预约动态」：明日已预约名单（公开信息，姓名脱敏） */
const nextDayReservations = ref<NextDayReservation[]>([])

const noticeItems = computed(() =>
  nextDayReservations.value.map((item) => {
    const time = (item.reservationTime || '').slice(11, 16)
    const who = maskName(item.name) + '(' + item.phone + ')'
    return time ? `${who} 预约了明日 ${time} 场次` : `${who} 预约了明日场次`
  })
)

/** 拉取明日预约动态：需登录，未登录/失效时静默失败，不影响首页展示 */
async function loadNotice() {
  if (!userStore.loggedIn) {
    nextDayReservations.value = []
    return
  }
  try {
    const list = await getNextDayReservations()
    nextDayReservations.value = list
      .filter((item) => !!item.name)
      .sort((a, b) => (a.reservationTime || '').localeCompare(b.reservationTime || ''))
      .slice(0, 50)
  } catch (e) {
    nextDayReservations.value = []
  }
}

async function load() {
  loading.value = true
  try {
    activities.value = await getActivities()
  } catch (e) {
    // 展示后端返回的真实原因（如网关限流「操作过于频繁，请稍候再试」）
    showError(e, '加载失败')
  } finally {
    loading.value = false
    uni.stopPullDownRefresh()
  }
  loadNotice()
}

function onBook(id: number) {
  // 未登录先跳登录
  if (!userStore.loggedIn) {
    uni.navigateTo({ url: `/pages/login/index?redirect=/pages/detail/index?id=${id}` })
    return
  }
  uni.navigateTo({ url: `/pages/detail/index?id=${id}` })
}

onShow(() => {
  load()
})

onPullDownRefresh(() => {
  load()
})
</script>

<template>
  <view class="home">
    <view class="home__search">
      <view class="search-box">
        <text class="search-box__dot"></text>
        <text class="search-box__text">搜索活动名称</text>
      </view>
    </view>

    <view v-if="loading" class="home__loading">加载中...</view>
    <view v-else-if="activities.length" class="activity-list">
      <ActivityCard
        v-for="a in activities"
        :key="a.projectId"
        :activity="a"
        @book="onBook"
      />
    </view>
    <EmptyState v-else text="暂无活动" />

    <!-- 预约动态：明日已预约名单滚动展示（公开信息，姓名脱敏） -->
    <view v-if="noticeItems.length" class="notice">
      <view class="notice__badge">
        <text class="notice__dot"></text>
        <text>明日已约 {{ noticeItems.length }} 人</text>
      </view>
      <swiper
        v-if="noticeItems.length > 3"
        class="notice__swiper"
        vertical
        autoplay
        circular
        :interval="2600"
        :duration="480"
        :display-multiple-items="3"
      >
        <swiper-item v-for="(item, i) in noticeItems" :key="i">
          <view class="notice__item text-ellipsis">{{ item }}</view>
        </swiper-item>
      </swiper>
      <view v-else class="notice__list">
        <view v-for="(item, i) in noticeItems" :key="i" class="notice__item text-ellipsis">
          {{ item }}
        </view>
      </view>
    </view>
  </view>
</template>

<style scoped lang="scss">
.home {
  min-height: 100vh;
  padding-bottom: 40rpx;
}
.home__search {
  background: linear-gradient(180deg, #3b7dff 0%, #5b95ff 100%);
  padding: 16rpx 24rpx 24rpx;
  margin-bottom: 24rpx;
}

/* 预约动态滚动条（列表下方，浅色卡片样式，三行展示） */
.notice {
  display: flex;
  align-items: center;
  padding: 20rpx;
  margin: 24rpx 24rpx 0;
  background: #ffffff;
  border: 1rpx solid #e5e8ef;
  border-radius: 24rpx;
  box-shadow: 0 4rpx 16rpx rgba(59, 125, 255, 0.08);
  overflow: hidden;
}
.notice__badge {
  flex-shrink: 0;
  align-self: flex-start;
  display: flex;
  align-items: center;
  height: 44rpx;
  padding-right: 16rpx;
  margin-right: 16rpx;
  border-right: 1rpx solid #e5e8ef;
  font-size: 22rpx;
  font-weight: 600;
  color: #3b7dff;
}
.notice__dot {
  width: 10rpx;
  height: 10rpx;
  border-radius: 50%;
  background: #3b7dff;
  margin-right: 8rpx;
  animation: notice-pulse 1.6s ease-in-out infinite;
}
.notice__swiper {
  flex: 1;
  height: 132rpx;
}
.notice__list {
  flex: 1;
}
.notice__item {
  height: 44rpx;
  line-height: 44rpx;
  font-size: 22rpx;
  color: #4e5969;
}
@keyframes notice-pulse {
  0%,
  100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.4;
    transform: scale(0.7);
  }
}
.search-box {
  background: #fff;
  height: 72rpx;
  border-radius: 36rpx;
  display: flex;
  align-items: center;
  padding: 0 28rpx;
  gap: 16rpx;
}
.search-box__dot {
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
  background: #3b7dff;
}
.search-box__text {
  color: #c9cdd4;
  font-size: 26rpx;
}
.home__loading {
  text-align: center;
  color: #86909c;
  padding: 100rpx 0;
  font-size: 26rpx;
}
.activity-list {
  padding: 0 24rpx;
}
</style>
