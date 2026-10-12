<script setup lang="ts">
import { ref } from 'vue'
import { onShow, onPullDownRefresh } from '@dcloudio/uni-app'
import type { Activity } from '@/types'
import { getActivities } from '@/api/activity'
import { useUserStore } from '@/stores/user'
import ActivityCard from '@/components/ActivityCard.vue'
import EmptyState from '@/components/EmptyState.vue'

const userStore = useUserStore()
const activities = ref<Activity[]>([])
const loading = ref(true)

async function load() {
  loading.value = true
  try {
    activities.value = await getActivities()
  } catch (e) {
    uni.showToast({ title: '加载失败', icon: 'none' })
  } finally {
    loading.value = false
    uni.stopPullDownRefresh()
  }
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
