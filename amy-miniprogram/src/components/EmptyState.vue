<script setup lang="ts">
import type { IconName } from '@/types'
import AppIcon from '@/components/AppIcon.vue'

/**
 * 空状态：默认使用统一的线性艺术图标（孔雀翎羽）。
 * 保留 icon 属性仅为兼容历史调用（传了 emoji 时按原样显示）。
 */
withDefaults(
  defineProps<{
    name?: IconName
    text?: string
    icon?: string
  }>(),
  { name: 'feather', text: '暂无数据', icon: '' }
)
</script>

<template>
  <view class="empty">
    <view class="empty__halo"></view>
    <view class="empty__icon">
      <AppIcon v-if="!icon" :name="name" :size="76" color="#f0cf94" />
      <text v-else class="empty__emoji">{{ icon }}</text>
    </view>
    <text class="empty__text">{{ text }}</text>
    <view class="empty__rule"></view>
  </view>
</template>

<style scoped lang="scss">
.empty {
  position: relative;
  @include art-card(0.9, 18px, 32rpx);
  margin: 32rpx 24rpx;
  padding: 72rpx 48rpx 80rpx;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  overflow: hidden;
}
.empty__halo {
  position: absolute;
  top: 20rpx;
  width: 300rpx;
  height: 300rpx;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(240, 207, 148, 0.14) 0%, rgba(240, 207, 148, 0) 70%);
}
.empty__icon {
  position: relative;
  width: 176rpx;
  height: 176rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  @include art-chip();
  border-radius: 50%;
  margin-bottom: 30rpx;
}
.empty__emoji {
  font-size: 76rpx;
  line-height: 1;
  opacity: 0.7;
}
.empty__text {
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.66);
  letter-spacing: 1rpx;
}
.empty__rule {
  @include art-rule(64rpx);
  margin-top: 24rpx;
  opacity: 0.8;
}
</style>
