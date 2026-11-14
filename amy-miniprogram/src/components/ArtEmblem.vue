<script setup lang="ts">
/**
 * 太阳宫艺术纹章
 * ------------------------------------------------------------
 * 用纯 CSS 几何层叠出「日轮 + 孔雀翎羽 + 水光星点」的意象：
 * - 金色日轮与同心光环 → 太阳宫 / 日月同辉
 * - 三支孔雀翎羽 → 孔雀舞的民族艺术语汇
 * - 青蓝渐变与胭脂点 → 洱海水光与扎染
 * 不引入任何图片与名人肖像，避免素材版权与包体积问题，
 * 也保证在不支持 backdrop-filter 的安卓 WebView 上完整可见。
 *
 * 用法：<ArtEmblem :size="220" />
 */
withDefaults(defineProps<{ size?: number }>(), { size: 240 })
</script>

<template>
  <view class="em" :style="{ fontSize: size + 'rpx' }">
    <!-- 柔光底层 -->
    <view class="em__aura"></view>
    <!-- 旋转的双层日环 -->
    <view class="em__ring em__ring--outer"></view>
    <view class="em__ring em__ring--inner"></view>
    <!-- 日轮 -->
    <view class="em__sun"></view>
    <view class="em__moon"></view>
    <!-- 三支孔雀翎羽 -->
    <view class="em__feather em__feather--l">
      <view class="em__vane"></view>
      <view class="em__eye"></view>
      <view class="em__core"></view>
      <view class="em__stem"></view>
    </view>
    <view class="em__feather em__feather--c">
      <view class="em__vane"></view>
      <view class="em__eye"></view>
      <view class="em__core"></view>
      <view class="em__stem"></view>
    </view>
    <view class="em__feather em__feather--r">
      <view class="em__vane"></view>
      <view class="em__eye"></view>
      <view class="em__core"></view>
      <view class="em__stem"></view>
    </view>
    <!-- 水光星点 -->
    <view class="em__spark em__spark--1"></view>
    <view class="em__spark em__spark--2"></view>
    <view class="em__spark em__spark--3"></view>
  </view>
</template>

<style scoped lang="scss">
.em {
  position: relative;
  width: 1em;
  height: 1em;
  flex-shrink: 0;
  animation: em-float 7s ease-in-out infinite;
}

/* 青紫柔光：让纹章在毛玻璃上有「浮起来」的体积 */
.em__aura {
  position: absolute;
  left: -0.1em;
  right: -0.1em;
  top: -0.1em;
  bottom: -0.1em;
  border-radius: 50%;
  background:
    radial-gradient(circle at 62% 30%, rgba(246, 220, 163, 0.62) 0%, rgba(246, 220, 163, 0) 46%),
    radial-gradient(circle at 32% 72%, rgba(31, 158, 154, 0.4) 0%, rgba(31, 158, 154, 0) 52%),
    radial-gradient(circle at 50% 50%, rgba(255, 255, 255, 0.5) 0%, rgba(255, 255, 255, 0) 70%);
}

.em__ring {
  position: absolute;
  border-radius: 50%;
}
.em__ring--outer {
  left: 0.04em;
  right: 0.04em;
  top: 0.04em;
  bottom: 0.04em;
  border: 0.012em solid rgba(200, 163, 92, 0.62);
  animation: em-spin 32s linear infinite;
}
.em__ring--outer::after {
  content: '';
  position: absolute;
  left: 50%;
  top: -0.026em;
  width: 0.052em;
  height: 0.052em;
  margin-left: -0.026em;
  border-radius: 50%;
  background: #d9ad5e;
  box-shadow: 0 0 0.06em rgba(217, 173, 94, 0.8);
}
.em__ring--inner {
  left: 0.14em;
  right: 0.14em;
  top: 0.14em;
  bottom: 0.14em;
  border: 0.012em dashed rgba(255, 255, 255, 0.82);
  animation: em-spin-reverse 24s linear infinite;
}

/* 日轮：暖金球，位于上方偏右，与下方孔雀翎形成「日月同辉」 */
.em__sun {
  position: absolute;
  left: 0.46em;
  top: 0.18em;
  width: 0.34em;
  height: 0.34em;
  border-radius: 50%;
  background: radial-gradient(circle at 34% 30%, #fff4d2 0%, #f0c87e 42%, #d79f4c 100%);
  box-shadow: 0 0 0.14em rgba(233, 190, 116, 0.72), 0 0.02em 0.08em rgba(160, 110, 50, 0.18);
}
/* 半透明月牙：叠在日轮左侧，用玻璃白勾一道弧 */
.em__moon {
  position: absolute;
  left: 0.36em;
  top: 0.2em;
  width: 0.3em;
  height: 0.3em;
  border-radius: 50%;
  border-left: 0.022em solid rgba(255, 255, 255, 0.82);
  transform: rotate(-24deg);
}

/* ---------- 孔雀翎羽 ---------- */
.em__feather {
  position: absolute;
  left: 50%;
  bottom: 0.06em;
  width: 0.3em;
  height: 0.66em;
  margin-left: -0.15em;
  transform-origin: bottom center;
}
.em__feather--l { transform: rotate(-34deg); animation: em-sway-l 6s ease-in-out infinite; }
.em__feather--c { transform: rotate(0deg); animation: em-sway-c 6.6s ease-in-out infinite; }
.em__feather--r { transform: rotate(34deg); animation: em-sway-r 6.3s ease-in-out infinite; }

/* 羽片：青蓝渐变的水滴形 */
.em__vane {
  position: absolute;
  left: 0.005em;
  right: 0.005em;
  top: 0;
  height: 0.48em;
  border-radius: 50% 50% 48% 48% / 62% 62% 38% 38%;
  border: 0.012em solid rgba(255, 255, 255, 0.72);
  background: linear-gradient(155deg, #58cfc0 0%, #2b9db6 42%, #3b5fc9 100%);
  box-shadow: 0 0.02em 0.08em rgba(30, 80, 140, 0.24), inset 0 0.02em 0.06em rgba(255, 255, 255, 0.4);
}
/* 翎眼：金环 + 深蓝瞳 */
.em__eye {
  position: absolute;
  left: 0.045em;
  right: 0.045em;
  top: 0.12em;
  height: 0.25em;
  border-radius: 50%;
  border: 0.018em solid rgba(240, 206, 139, 0.92);
  background: radial-gradient(circle at 42% 34%, #7ad9cf 0%, #2e74a8 55%, #29407f 100%);
}
.em__core {
  position: absolute;
  left: 50%;
  top: 0.215em;
  width: 0.075em;
  height: 0.075em;
  margin-left: -0.0375em;
  border-radius: 50%;
  background: #1e2f66;
  box-shadow: 0 0 0.04em rgba(255, 255, 255, 0.65);
}
/* 羽轴 */
.em__stem {
  position: absolute;
  left: 50%;
  top: 0.44em;
  bottom: 0;
  width: 0.018em;
  margin-left: -0.009em;
  border-radius: 0.02em;
  background: linear-gradient(180deg, rgba(214, 174, 102, 0.9), rgba(214, 174, 102, 0.16));
}

/* ---------- 水光星点 ---------- */
.em__spark {
  position: absolute;
  width: 0.05em;
  height: 0.05em;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 0 0.06em rgba(255, 255, 255, 0.9);
  animation: em-twinkle 3.2s ease-in-out infinite;
}
.em__spark--1 { left: 0.14em; top: 0.3em; }
.em__spark--2 { right: 0.1em; top: 0.58em; animation-delay: 1.1s; }
.em__spark--3 { left: 0.24em; bottom: 0.16em; animation-delay: 2s; width: 0.034em; height: 0.034em; }

@keyframes em-spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}
@keyframes em-spin-reverse {
  0% { transform: rotate(360deg); }
  100% { transform: rotate(0deg); }
}
@keyframes em-float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-0.035em); }
}
@keyframes em-sway-l {
  0%, 100% { transform: rotate(-34deg); }
  50% { transform: rotate(-30deg); }
}
@keyframes em-sway-c {
  0%, 100% { transform: rotate(0deg); }
  50% { transform: rotate(3deg); }
}
@keyframes em-sway-r {
  0%, 100% { transform: rotate(34deg); }
  50% { transform: rotate(30deg); }
}
@keyframes em-twinkle {
  0%, 100% { opacity: 0.35; transform: scale(0.7); }
  50% { opacity: 1; transform: scale(1.15); }
}
</style>
