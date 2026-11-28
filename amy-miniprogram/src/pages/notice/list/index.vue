<script setup lang="ts">
import { ref } from 'vue'
import { onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import type { NoticeItem } from '@/types'
import { getNotices } from '@/api/notice'
import { showError } from '@/utils/request'
import PageHero from '@/components/PageHero.vue'
import EmptyState from '@/components/EmptyState.vue'
import AppIcon from '@/components/AppIcon.vue'

/** 公告列表：最多拉 50 条，列表项点击进详情 */
const list = ref<NoticeItem[]>([])
const loading = ref(true)

const TYPE_TEXT: Record<string, string> = { '1': '通知', '2': '公告' }

async function load() {
  loading.value = true
  try {
    list.value = await getNotices(50)
  } catch (e) {
    showError(e, '公告加载失败')
  } finally {
    loading.value = false
    uni.stopPullDownRefresh()
  }
}

function onItemTap(item: NoticeItem) {
  uni.navigateTo({ url: `/pages/notice/detail/index?id=${item.noticeId}` })
}

onShow(() => {
  load()
})

onPullDownRefresh(() => {
  load()
})
</script>

<template>
  <view class="notice-page">
    <PageHero title="通知公告" eyebrow="NOTICES · SUN PALACE" subtitle="公告与通知" compact flush />

    <view v-if="loading" class="notice-page__skeletons">
      <view v-for="i in 3" :key="i" class="skeleton-item">
        <view class="skeleton-item__bar"></view>
        <view class="skeleton-item__sub"></view>
      </view>
    </view>

    <template v-else-if="list.length">
      <view
        v-for="(item, i) in list"
        :key="item.noticeId"
        class="notice-item anim-up"
        :style="{ animationDelay: i * 0.06 + 's' }"
        @tap="onItemTap(item)"
      >
        <view class="notice-item__main">
          <view class="notice-item__tag">{{ TYPE_TEXT[item.noticeType] || '公告' }}</view>
          <text class="notice-item__title text-ellipsis">{{ item.noticeTitle }}</text>
        </view>
        <view class="notice-item__meta">
          <text>{{ (item.createTime || '').slice(0, 10) }}</text>
          <AppIcon name="next" :size="22" color="#9aa8c8" />
        </view>
      </view>
    </template>

    <EmptyState v-else name="note" text="暂无公告" />
  </view>
</template>

<style scoped lang="scss">
.notice-page {
  min-height: 100vh;
  padding: 0 24rpx $art-page-bottom;
}
@keyframes skeleton-flow {
  0% { background-position: 100% 0; }
  100% { background-position: -100% 0; }
}
.notice-page__skeletons {
  padding-top: $art-gap;
}
.skeleton-item {
  @include art-card(0.62, 16px, 32rpx);
  padding: 36rpx;
  margin-bottom: 24rpx;
}
.skeleton-item__bar {
  height: 32rpx;
  width: 70%;
  border-radius: 10rpx;
  background: rgba(255, 255, 255, 0.1);
  animation: skeleton-flow 1.6s linear infinite;
}
.skeleton-item__sub {
  height: 22rpx;
  width: 40%;
  margin-top: 20rpx;
  border-radius: 10rpx;
  background: rgba(255, 255, 255, 0.07);
}
.notice-item {
  @include art-card(0.62, 16px, 32rpx);
  padding: 30rpx 32rpx;
  /* 父容器已有 24rpx 内边距，此处不再叠加左右外边距：
     与首页 Hero / 活动卡片、以及 flush 后的 PageHero 保持同一宽度 */
  margin-top: 24rpx;
}
.notice-item__main {
  display: flex;
  align-items: center;
  gap: 14rpx;
  min-width: 0;
}
.notice-item__tag {
  flex-shrink: 0;
  padding: 4rpx 14rpx;
  border-radius: 18rpx;
  font-size: 20rpx;
  font-weight: 600;
  color: #f0cf94;
  background: rgba(240, 207, 148, 0.14);
}
.notice-item__title {
  flex: 1;
  min-width: 0;
  font-size: 28rpx;
  font-weight: 600;
  color: $art-text-strong;
}
.notice-item__meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 14rpx;
  font-size: 22rpx;
  color: $uni-text-color-grey;
}
</style>
