<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  show: boolean
  title?: string
  content: string
}>()

const emit = defineEmits<{
  (e: 'confirm'): void
  (e: 'close'): void
}>()

/** 简单解析须知文本，支持 ## 标题 */
const blocks = computed(() => {
  return props.content
    .split('\n')
    .filter((l) => l.trim() !== '')
    .map((line) => {
      if (line.startsWith('## ')) {
        return { type: 'h', text: line.replace('## ', '') }
      }
      return { type: 'p', text: line }
    })
})
</script>

<template>
  <view v-if="show" class="modal-mask" @tap="emit('close')">
    <view class="sheet" @tap.stop>
      <view class="sheet__title">
        <text>{{ title || '预约须知' }}</text>
        <text class="sheet__close" @tap="emit('close')">×</text>
      </view>
      <scroll-view class="notice" scroll-y>
        <template v-for="(b, i) in blocks" :key="i">
          <view v-if="b.type === 'h'" class="notice__h">{{ b.text }}</view>
          <view v-else class="notice__p">{{ b.text }}</view>
        </template>
        <view class="notice__hint">— 请仔细阅读以上须知 —</view>
      </scroll-view>
      <view class="sheet__btn" @tap="emit('confirm')">我已阅读</view>
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
  max-height: 80vh;
  display: flex;
  flex-direction: column;
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
.notice {
  flex: 1;
  min-height: 0;
  max-height: 48vh;
  font-size: 26rpx;
  color: #1f2329;
  line-height: 1.85;
}
.notice__h {
  font-size: 28rpx;
  font-weight: 600;
  margin: 20rpx 0 10rpx;
  color: #1f2329;
}
.notice__p {
  margin-bottom: 8rpx;
}
.notice__hint {
  color: #86909c;
  font-size: 22rpx;
  text-align: center;
  margin: 16rpx 0 8rpx;
}
.sheet__btn {
  margin-top: 24rpx;
  padding: 26rpx;
  border-radius: 56rpx;
  text-align: center;
  font-size: 30rpx;
  font-weight: 500;
  background: linear-gradient(90deg, #3b7dff, #5b95ff);
  color: #fff;
  box-shadow: 0 8rpx 24rpx rgba(59, 125, 255, 0.35);
}
</style>
