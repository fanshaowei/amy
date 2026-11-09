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

/**
 * 预约须知为后台富文本编辑器产出的 HTML，直接交给 rich-text 渲染。
 * 处理两点：
 * 1. 空内容兜底（rich-text 传空串时小程序端会告警）；
 * 2. 图片可能超出弹窗宽度，统一注入 max-width 自适应样式（已有 style 的追加，没有的补上）。
 */
const nodes = computed(() => {
  const html = props.content || '<p></p>'
  return html
    .replace(/<img([^>]*?)style="([^"]*?)"/gi, '<img$1style="$2;max-width:100%;height:auto"')
    .replace(/<img(?![^>]*?style=)/gi, '<img style="max-width:100%;height:auto"')
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
        <rich-text class="notice__rich" :nodes="nodes"></rich-text>
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
/* 富文本内图片自适应宽度（rich-text 内联样式随节点下发，这里兜底容器表现） */
.notice__rich {
  display: block;
  width: 100%;
  word-break: break-all;
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
