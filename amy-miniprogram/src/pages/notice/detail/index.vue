<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import type { NoticeDetail } from '@/types'
import { getNoticeDetail } from '@/api/notice'
import { showError } from '@/utils/request'
import PageHero from '@/components/PageHero.vue'

/** 公告详情：rich-text 渲染后台富文本（sys_notice.notice_content，HTML） */
const detail = ref<NoticeDetail | null>(null)
const loading = ref(true)
const failed = ref(false)

async function load(id: number) {
  loading.value = true
  failed.value = false
  try {
    detail.value = await getNoticeDetail(id)
    if (detail.value?.noticeTitle) {
      uni.setNavigationBarTitle({ title: detail.value.noticeTitle.slice(0, 16) })
    }
  } catch (e) {
    failed.value = true
    showError(e, '公告加载失败')
  } finally {
    loading.value = false
  }
}

onLoad((query) => {
  const id = Number(query?.id || 0)
  if (!id) {
    failed.value = true
    loading.value = false
    return
  }
  load(id)
})
</script>

<template>
  <view class="notice-detail">
    <PageHero title="公告详情" eyebrow="NOTICE · SUN PALACE" compact flush />

    <view v-if="loading" class="notice-detail__loading">
      <view class="skeleton-line skeleton-line--title"></view>
      <view class="skeleton-line"></view>
      <view class="skeleton-line"></view>
      <view class="skeleton-line skeleton-line--short"></view>
    </view>

    <view v-else-if="detail" class="notice-detail__card">
      <view class="notice-detail__head">
        <text class="notice-detail__title">{{ detail.noticeTitle }}</text>
        <view class="notice-detail__meta">
          <text>{{ detail.noticeType === '1' ? '通知' : '公告' }}</text>
          <text class="notice-detail__dot">·</text>
          <text>{{ detail.createTime || '' }}</text>
        </view>
      </view>
      <view class="notice-detail__divider"></view>
      <rich-text class="notice-detail__content" :nodes="detail.noticeContent || '（公告内容为空）'"></rich-text>
    </view>

    <view v-else class="notice-detail__card notice-detail__card--empty">
      <text>公告不存在或已关闭</text>
    </view>
  </view>
</template>

<style scoped lang="scss">
.notice-detail {
  min-height: 100vh;
  padding: 0 24rpx $art-page-bottom;
}
.notice-detail__loading {
  @include art-card(0.62, 16px, 32rpx);
  /* 父容器已有 24rpx 内边距，此处不再叠加左右外边距：
     与首页卡片、列表页卡片及 flush 后的 PageHero 同宽 */
  margin: $art-gap 0 0;
  /* 左右 28rpx 与 PageHero 的 28rpx 内边距一致，卡片内文字与页头标题左对齐 */
  padding: 40rpx 28rpx;
}
.skeleton-line {
  height: 24rpx;
  border-radius: 10rpx;
  background: rgba(255, 255, 255, 0.1);
  margin-bottom: 20rpx;
  animation: skeleton-flow 1.6s linear infinite;
}
.skeleton-line--title {
  height: 34rpx;
  width: 60%;
  margin-top: 6rpx;
}
.skeleton-line--short {
  width: 40%;
  margin-bottom: 0;
}
.notice-detail__card {
  @include art-card(0.62, 16px, 32rpx);
  /* 父容器已有 24rpx 内边距，此处不再叠加左右外边距：
     与首页卡片、列表页卡片及 flush 后的 PageHero 同宽 */
  margin: $art-gap 0 0;
  /* 左右 28rpx 与 PageHero 的 28rpx 内边距一致，卡片内文字与页头标题左对齐 */
  padding: 40rpx 28rpx;
  color: $art-text;
}
.notice-detail__card--empty {
  text-align: center;
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.6);
}
.notice-detail__title {
  display: block;
  font-size: 34rpx;
  font-weight: 700;
  line-height: 1.5;
  color: $art-text-strong;
}
.notice-detail__meta {
  display: flex;
  align-items: center;
  margin-top: 16rpx;
  font-size: 22rpx;
  color: $uni-text-color-grey;
}
.notice-detail__dot {
  margin: 0 10rpx;
}
.notice-detail__divider {
  height: 1rpx;
  margin: 26rpx 0;
  background: linear-gradient(90deg, $art-gradient-gold, rgba(200, 163, 92, 0.04));
}
.notice-detail__content {
  font-size: 28rpx;
  line-height: 1.9;
  word-break: break-all;
}
</style>
