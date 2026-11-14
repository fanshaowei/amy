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
 * 预约须知为后台富文本编辑器（Word 粘贴样式）产出的 HTML，交给 rich-text 渲染。
 * 内容区是浅色「纸张」背景（见 .notice），后台原本的深色文字无需改写即可读，
 * 所以这里**不要**给富文本注入/覆盖任何文字颜色，保留后台配色原样。
 * 只处理两点：
 * 1. 空内容兜底（rich-text 传空串时小程序端会告警）；
 * 2. 图片可能超出弹窗宽度，统一注入 max-width 自适应样式。
 */
const nodes = computed(() => {
  const html = props.content || '<p></p>'
  const wrapped = '<div style="line-height:1.85">' + html + '</div>'
  return wrapped
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
  @include glass-mask;
  z-index: 999;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
}
.sheet {
  @include art-strong();
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
  /* 标题留在深靛托盘上，显式用浅色 */
  color: $art-text-strong;
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
  color: $art-text-dim;
  font-size: 40rpx;
}
/* 浅色「纸张」：后台 Word 粘贴的深色文字（含「入馆方式」正文的
 * color: rgb(65,65,65)）本来就是按白底排的，放回浅底即可正常阅读，
 * 不需要也不应该改写富文本内容。深靛只保留在外层托盘、标题与按钮上。 */
.notice {
  flex: 1;
  min-height: 0;
  max-height: 48vh;
  box-sizing: border-box;
  padding: 28rpx 26rpx;
  border-radius: 24rpx;
  background-color: #fffdf8;
  background-image: linear-gradient(160deg, #ffffff 0%, #f6f3ea 100%);
  box-shadow: inset 0 2rpx 12rpx rgba(38, 52, 100, 0.07);
  font-size: 26rpx;
  color: $uni-text-color;
  line-height: 1.85;
}
/* 富文本内图片自适应宽度（rich-text 内联样式随节点下发，这里兜底容器表现） */
.notice__rich {
  display: block;
  width: 100%;
  word-break: break-all;
}
.notice__hint {
  color: $uni-text-color-grey;
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
  @include art-gold-button();
  
}
</style>
