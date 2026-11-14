<script setup lang="ts">
import ArtEmblem from './ArtEmblem.vue'

/**
 * 页面级艺术页头：把首页 Hero 的深靛渐变、描金细线、孔雀翎纹章、水波弧
 * 缩成一条统一的品牌带，供首页之外的所有页面共用，保证进任意页面都是同一套语言。
 */
withDefaults(
  defineProps<{
    title: string
    eyebrow?: string
    subtitle?: string
    compact?: boolean
    /** 页面根容器自带左右 padding 时置 true，让页头与下方卡片左右对齐 */
    flush?: boolean
  }>(),
  {
    eyebrow: 'SUN PALACE · ART SPACE',
    subtitle: '',
    compact: false,
    flush: false,
  }
)
</script>

<template>
  <view class="ph" :class="{ 'ph--compact': compact, 'ph--flush': flush }">
    <view class="ph__corner"></view>
    <view class="ph__wave"></view>
    <view class="ph__body">
      <view class="ph__eyebrow">
        <view class="ph__rule"></view>
        <text>{{ eyebrow }}</text>
      </view>
      <view class="ph__title">{{ title }}</view>
      <view v-if="subtitle" class="ph__sub">{{ subtitle }}</view>
    </view>
    <view class="ph__emblem"><ArtEmblem :size="132" /></view>
  </view>
</template>

<style scoped lang="scss">
.ph {
  position: relative;
  margin: 16rpx 24rpx 0;
  height: 216rpx;
  border-radius: 32rpx;
  overflow: hidden;
  box-sizing: border-box;
  padding: 30rpx 28rpx;
  background-color: rgba(36, 58, 114, 0.86);
  background-image: linear-gradient(
    118deg,
    rgba(32, 51, 106, 0.96) 0%,
    rgba(30, 92, 130, 0.9) 54%,
    rgba(31, 138, 132, 0.8) 100%
  );
  -webkit-backdrop-filter: blur(20px) saturate(170%);
  backdrop-filter: blur(20px) saturate(170%);
  box-shadow: 0 18rpx 46rpx rgba(20, 36, 82, 0.26),
    inset 0 1rpx 0 rgba(240, 207, 148, 0.2);
}
.ph--compact {
  height: 184rpx;
}
.ph--flush {
  margin-left: 0;
  margin-right: 0;
}
.ph__corner {
  position: absolute;
  right: -70rpx;
  top: -96rpx;
  width: 250rpx;
  height: 250rpx;
  border-radius: 50%;
  border: 1rpx solid rgba(240, 207, 148, 0.26);
}
.ph__wave {
  position: absolute;
  left: -30%;
  right: -30%;
  bottom: -220rpx;
  height: 280rpx;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.08);
  box-shadow: 0 -28rpx 0 rgba(255, 255, 255, 0.045);
}
.ph__body {
  position: relative;
  z-index: 2;
  width: 72%;
}
.ph__eyebrow {
  display: flex;
  align-items: center;
  gap: 12rpx;
  font-size: 17rpx;
  letter-spacing: 2rpx;
  color: rgba(240, 207, 148, 0.94);
  margin-bottom: 12rpx;
}
.ph__rule {
  @include art-rule(36rpx);
}
.ph__title {
  font-size: 38rpx;
  font-weight: 700;
  line-height: 1.24;
  letter-spacing: 2rpx;
  color: #fffaf0;
  text-shadow: 0 2rpx 12rpx rgba(12, 28, 70, 0.3);
}
.ph__sub {
  margin-top: 8rpx;
  font-size: 21rpx;
  letter-spacing: 1rpx;
  color: rgba(255, 255, 255, 0.72);
  /* 组件样式隔离，不能依赖 App.vue 的全局 text-ellipsis，这里内聚一份 */
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.ph__emblem {
  position: absolute;
  right: 2rpx;
  top: 40rpx;
  z-index: 1;
  opacity: 0.92;
}
.ph--compact .ph__emblem {
  top: 26rpx;
}
</style>
