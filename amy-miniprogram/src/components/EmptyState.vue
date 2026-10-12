<script setup lang="ts">
defineProps<{
  icon?: string
  text?: string
}>()
</script>

<template>
  <view class="empty">
    <view class="empty__icon">{{ icon || '📋' }}</view>
    <text class="empty__text">{{ text || '暂无数据' }}</text>
  </view>
</template>

<style scoped lang="scss">
.empty {
  padding: 160rpx 64rpx;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.empty__icon {
  font-size: 120rpx;
  opacity: 0.4;
  margin-bottom: 24rpx;
}
.empty__text {
  font-size: 26rpx;
  color: #86909c;
}
</style>
