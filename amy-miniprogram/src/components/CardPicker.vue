<script setup lang="ts">
import { CARD_TYPES } from '@/types'

defineProps<{
  show: boolean
  selected: number
}>()

const emit = defineEmits<{
  (e: 'select', id: number): void
  (e: 'close'): void
}>()
</script>

<template>
  <view v-if="show" class="modal-mask" @tap="emit('close')">
    <view class="sheet" @tap.stop>
      <view class="sheet__title">
        <text>选择证件类型</text>
        <text class="sheet__close" @tap="emit('close')">×</text>
      </view>
      <view class="list">
        <view
          v-for="c in CARD_TYPES"
          :key="c.id"
          class="item"
          :class="{ selected: selected === c.id }"
          @tap="emit('select', c.id)"
        >
          <text>{{ c.title }}</text>
          <text v-if="selected === c.id" class="check">✓</text>
        </view>
      </view>
    </view>
  </view>
</template>

<style scoped lang="scss">
.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.55);
  z-index: 999;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
}
.sheet {
  background: #fff;
  border-radius: 40rpx 40rpx 0 0;
  padding: 32rpx 32rpx calc(32rpx + env(safe-area-inset-bottom));
}
.sheet__title {
  text-align: center;
  font-size: 32rpx;
  font-weight: 600;
  padding: 8rpx 0 28rpx;
  position: relative;
}
.sheet__close {
  position: absolute;
  right: 0;
  top: 0;
  width: 56rpx;
  height: 56rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #86909c;
  font-size: 40rpx;
}
.list {
  padding: 0 8rpx;
}
.item {
  padding: 28rpx 16rpx;
  border-bottom: 1px solid #f2f3f5;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 28rpx;
}
.item:last-child {
  border-bottom: 0;
}
.item.selected {
  color: #3b7dff;
  font-weight: 600;
}
.check {
  color: #3b7dff;
  font-size: 32rpx;
}
</style>
