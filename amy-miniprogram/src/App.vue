<script setup lang="ts">
import { onLaunch, onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'

onLaunch(() => {
  // 启动时恢复登录态
  const userStore = useUserStore()
  userStore.restore()
  console.log('App Launch')
})

onShow(() => {
  // 从后台回到前台时重新对齐 storage：开发者工具/系统直接清空数据缓存时，
  // 内存里的 token 会与实际登录态不一致，导致「看似已登录，进子页面又跳登录」。
  useUserStore().restore()
})
</script>

<style lang="scss">
/* ============================================================
 * 全局背景层：太阳宫氛围
 * ------------------------------------------------------------
 * 意象：洱海水光（同心涟漪）、日轮暖金、孔雀青、扎染靛紫、胭脂点。
 * 全部用 CSS 渐变实现，不依赖图片、不依赖 backdrop-filter，
 * 因此所有机型都能看到完整氛围；玻璃卡片再浮在这层之上做模糊。
 * 注意：radial-gradient 一律用「起止两段」的传统色标写法，
 * 不用双位置色标，避免旧版 WXSS 解析失败。
 * ============================================================ */
page {
  min-height: 100vh;
  background-color: #eef4fb;
  background-image:
    /* 右上角：日轮外的水光涟漪，三层同心细环 */
    radial-gradient(circle at 86% 10%, rgba(255, 255, 255, 0) 0rpx, rgba(255, 255, 255, 0) 74rpx, rgba(255, 255, 255, 0.5) 76rpx, rgba(255, 255, 255, 0) 79rpx),
    radial-gradient(circle at 86% 10%, rgba(255, 255, 255, 0) 0rpx, rgba(255, 255, 255, 0) 122rpx, rgba(255, 255, 255, 0.36) 124rpx, rgba(255, 255, 255, 0) 127rpx),
    radial-gradient(circle at 86% 10%, rgba(255, 255, 255, 0) 0rpx, rgba(255, 255, 255, 0) 170rpx, rgba(255, 255, 255, 0.24) 172rpx, rgba(255, 255, 255, 0) 175rpx),
    /* 日轮暖金光晕 */
    radial-gradient(circle at 88% 7%, rgba(242, 202, 126, 0.48) 0%, rgba(242, 202, 126, 0) 42%),
    /* 孔雀青 / 靛蓝 / 胭脂 / 紫 的弥散光斑 */
    radial-gradient(circle at 4% 9%, rgba(31, 158, 154, 0.3) 0%, rgba(31, 158, 154, 0) 46%),
    radial-gradient(circle at 96% 42%, rgba(216, 120, 158, 0.22) 0%, rgba(216, 120, 158, 0) 44%),
    radial-gradient(circle at 8% 54%, rgba(94, 124, 226, 0.32) 0%, rgba(94, 124, 226, 0) 48%),
    radial-gradient(circle at 44% 100%, rgba(140, 110, 230, 0.2) 0%, rgba(140, 110, 230, 0) 52%),
    radial-gradient(circle at 2% 92%, rgba(56, 189, 180, 0.2) 0%, rgba(56, 189, 180, 0) 44%),
    linear-gradient(165deg, #f7f9ff 0%, #eaf3fb 46%, #f6efff 100%);
  background-repeat: no-repeat;
  background-size: 100% 100%;
  background-attachment: fixed;
  color: $uni-text-color;
  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Helvetica Neue',
    'Microsoft YaHei', sans-serif;
  font-size: 28rpx;
  line-height: 1.6;
}

/* 原生输入组件在玻璃上不要自带白底。
 * 全站内容卡已统一为深靛玻璃，输入文字用暖白；浅底的搜索框自行覆盖。 */
input,
textarea {
  background: transparent;
  color: $art-text-strong;
}
/* 微信小程序的 placeholder 颜色只能通过 placeholder-class 指定，
 * 全局统一挂这个类，深靛卡片上的占位文字才不会是默认深灰。 */
.art-ph {
  color: rgba(255, 255, 255, 0.42);
}

/* ============================================================
 * 可复用的玻璃工具类（模板里直接写 class 即可）
 * ============================================================ */
.glass-card {
  @include glass-surface();
}
.glass-card--flat {
  @include glass-surface(0.5, 14px, 24rpx);
}
.glass-panel {
  @include glass-strong(40rpx);
}
.glass-inset {
  @include glass-inset();
}
.glass-chip {
  @include glass-chip();
  border-radius: 24rpx;
}
.glass-btn {
  @include brand-button();
  color: #fff;
  border-radius: 56rpx;
}
.glass-divider {
  border-top: 1rpx solid $glass-divider;
}

/* ============================================================
 * 艺术化通用类
 * ============================================================ */

/* 圆形玻璃图标底座：配合 AppIcon 使用 */
.glass-well {
  @include art-icon-well();
  width: 56rpx;
  height: 56rpx;
}
.glass-well--sm {
  @include art-icon-well();
  width: 44rpx;
  height: 44rpx;
}

/* 描金小字 / 编号 */
.art-gold {
  color: #b8893f;
}
.art-serif-num {
  font-family: 'DIN Alternate', 'Baskerville', 'Times New Roman', serif;
  font-weight: 700;
  letter-spacing: 1rpx;
}

/* 入场动效：uni.scss 的 anim-up mixin 引用这里的 keyframes */
@keyframes art-up {
  from {
    opacity: 0;
    transform: translateY(26rpx);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
.anim-up {
  @include anim-up(0s);
}
.anim-up--1 {
  @include anim-up(0.08s);
}
.anim-up--2 {
  @include anim-up(0.16s);
}
.anim-up--3 {
  @include anim-up(0.24s);
}

/* 通用工具类 */
.flex {
  display: flex;
}
.flex-col {
  display: flex;
  flex-direction: column;
}
.flex-1 {
  flex: 1;
  min-width: 0;
}
.flex-center {
  display: flex;
  align-items: center;
  justify-content: center;
}
.items-center {
  align-items: center;
}
.justify-between {
  justify-content: space-between;
}
.text-ellipsis {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}
</style>
