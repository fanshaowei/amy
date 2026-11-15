<script setup lang="ts">
/**
 * 全站统一线性图标
 * ------------------------------------------------------------
 * 为什么不用 emoji / 图片 / iconfont：
 * - emoji 在 iOS、Android、开发者工具上造型与颜色完全不同，且无法跟随主题色；
 * - 小程序 WXSS 里 background-image 的 svg data-uri 在部分安卓机型不渲染；
 * - iconfont 需要引入字体文件，小程序本地 @font-face 路径限制多。
 * 这里用纯 CSS 几何绘制（view + 伪元素），线宽统一 .075em，
 * 容器 font-size 即图标尺寸，颜色继承 currentColor，任何机型表现一致。
 *
 * 用法：<AppIcon name="calendar" :size="28" color="#c8a35c" />
 */
import type { IconName } from '@/types'

withDefaults(
  defineProps<{
    name: IconName
    /** 图标边长，单位 rpx */
    size?: number
    /** 不传则继承父级文字色 */
    color?: string
  }>(),
  { size: 32, color: '' }
)
</script>

<template>
  <view
    class="ai"
    :class="`ai--${name}`"
    :style="{ fontSize: size + 'rpx', color: color || 'currentColor' }"
  >
    <view class="ai__p1"></view>
    <view class="ai__p2"></view>
    <view class="ai__p3"></view>
    <view class="ai__p4"></view>
  </view>
</template>

<style scoped lang="scss">
/* 1em × 1em 画布：所有部件用 em 定位，随 font-size 等比缩放 */
.ai {
  position: relative;
  display: inline-block;
  width: 1em;
  height: 1em;
  flex-shrink: 0;
  box-sizing: border-box;
  line-height: 1;
  vertical-align: middle;
}
.ai__p1,
.ai__p2,
.ai__p3,
.ai__p4 {
  position: absolute;
  display: none;
  box-sizing: border-box;
}

/* ---------- 定位 ---------- */
.ai--location {
  .ai__p1 {
    display: block;
    left: 50%;
    margin-left: -0.26em;
    top: 0.08em;
    width: 0.52em;
    height: 0.52em;
    border: 0.075em solid currentColor;
    border-radius: 50% 50% 50% 0;
    transform: rotate(-45deg);
  }
  .ai__p2 {
    display: block;
    left: 50%;
    margin-left: -0.085em;
    top: 0.26em;
    width: 0.17em;
    height: 0.17em;
    border-radius: 50%;
    background: currentColor;
  }
  .ai__p3 {
    display: block;
    left: 50%;
    margin-left: -0.24em;
    bottom: 0.06em;
    width: 0.48em;
    height: 0.075em;
    border-radius: 50%;
    background: currentColor;
    opacity: 0.28;
  }
}

/* ---------- 日历 ---------- */
.ai--calendar {
  .ai__p1 {
    display: block;
    left: 0.08em;
    right: 0.08em;
    top: 0.19em;
    bottom: 0.09em;
    border: 0.075em solid currentColor;
    border-radius: 0.13em;
  }
  .ai__p2 {
    display: block;
    left: 0.08em;
    right: 0.08em;
    top: 0.38em;
    height: 0.075em;
    background: currentColor;
  }
  .ai__p3,
  .ai__p4 {
    display: block;
    top: 0.1em;
    width: 0.075em;
    height: 0.17em;
    border-radius: 0.04em;
    background: currentColor;
  }
  .ai__p3 { left: 0.27em; }
  .ai__p4 { right: 0.27em; }
}

/* ---------- 时钟 ---------- */
.ai--clock {
  .ai__p1 {
    display: block;
    left: 0.07em;
    right: 0.07em;
    top: 0.07em;
    bottom: 0.07em;
    border: 0.075em solid currentColor;
    border-radius: 50%;
  }
  .ai__p2 {
    display: block;
    left: 50%;
    margin-left: -0.0375em;
    top: 0.26em;
    width: 0.075em;
    height: 0.24em;
    border-radius: 0.04em;
    background: currentColor;
  }
  .ai__p3 {
    display: block;
    left: 50%;
    top: 50%;
    margin-top: -0.0375em;
    width: 0.21em;
    height: 0.075em;
    border-radius: 0.04em;
    background: currentColor;
  }
  .ai__p4 {
    display: block;
    left: 50%;
    top: 50%;
    margin-left: -0.05em;
    margin-top: -0.05em;
    width: 0.1em;
    height: 0.1em;
    border-radius: 50%;
    background: currentColor;
  }
}

/* ---------- 搜索 ---------- */
.ai--search {
  .ai__p1 {
    display: block;
    left: 0.09em;
    top: 0.08em;
    width: 0.56em;
    height: 0.56em;
    border: 0.075em solid currentColor;
    border-radius: 50%;
  }
  .ai__p2 {
    display: block;
    left: 0.6em;
    top: 0.62em;
    width: 0.3em;
    height: 0.075em;
    border-radius: 0.04em;
    background: currentColor;
    transform: rotate(45deg);
    transform-origin: left center;
  }
}

/* ---------- 票券 ---------- */
.ai--ticket {
  .ai__p1 {
    display: block;
    left: 0.06em;
    right: 0.06em;
    top: 0.18em;
    bottom: 0.18em;
    border: 0.075em solid currentColor;
    border-radius: 0.12em;
  }
  .ai__p2 {
    display: block;
    left: 62%;
    top: 0.24em;
    bottom: 0.24em;
    border-left: 0.055em dashed currentColor;
    opacity: 0.7;
  }
  .ai__p3,
  .ai__p4 {
    display: block;
    left: 0.17em;
    height: 0.06em;
    border-radius: 0.03em;
    background: currentColor;
  }
  .ai__p3 { top: 0.38em; width: 0.28em; }
  .ai__p4 { top: 0.56em; width: 0.22em; opacity: 0.55; }
}

/* ---------- 铃铛 ---------- */
.ai--bell {
  .ai__p1 {
    display: block;
    left: 0.23em;
    right: 0.23em;
    top: 0.16em;
    bottom: 0.28em;
    border: 0.075em solid currentColor;
    border-bottom: none;
    border-radius: 0.3em 0.3em 0.08em 0.08em;
  }
  .ai__p2 {
    display: block;
    left: 0.17em;
    right: 0.17em;
    top: 0.7em;
    height: 0.075em;
    border-radius: 0.04em;
    background: currentColor;
  }
  .ai__p3 {
    display: block;
    left: 50%;
    margin-left: -0.075em;
    top: 0.76em;
    width: 0.15em;
    height: 0.13em;
    border-radius: 0 0 0.075em 0.075em;
    background: currentColor;
  }
  .ai__p4 {
    display: block;
    left: 50%;
    margin-left: -0.06em;
    top: 0.08em;
    width: 0.12em;
    height: 0.12em;
    border-radius: 50%;
    background: currentColor;
  }
}

/* ---------- 用户 ---------- */
.ai--user {
  .ai__p1 {
    display: block;
    left: 50%;
    margin-left: -0.17em;
    top: 0.09em;
    width: 0.34em;
    height: 0.34em;
    border: 0.075em solid currentColor;
    border-radius: 50%;
  }
  .ai__p2 {
    display: block;
    left: 0.13em;
    right: 0.13em;
    bottom: 0.07em;
    height: 0.36em;
    border: 0.075em solid currentColor;
    border-bottom: none;
    border-radius: 0.4em 0.4em 0 0;
  }
}

/* ---------- 认证盾 ---------- */
.ai--shield {
  .ai__p1 {
    display: block;
    left: 0.16em;
    right: 0.16em;
    top: 0.08em;
    bottom: 0.1em;
    border: 0.075em solid currentColor;
    border-radius: 0.16em 0.16em 0.38em 0.38em;
  }
  .ai__p2 {
    display: block;
    left: 0.32em;
    top: 0.38em;
    width: 0.18em;
    height: 0.1em;
    border-left: 0.07em solid currentColor;
    border-bottom: 0.07em solid currentColor;
    transform: rotate(-45deg);
  }
}

/* ---------- 列表 ---------- */
.ai--list {
  .ai__p1,
  .ai__p2,
  .ai__p3 {
    display: block;
    left: 0.32em;
    right: 0.08em;
    height: 0.07em;
    border-radius: 0.04em;
    background: currentColor;
  }
  .ai__p1 { top: 0.24em; }
  .ai__p2 { top: 0.47em; }
  .ai__p3 { top: 0.7em; }
  .ai__p4 {
    display: block;
    left: 0.11em;
    top: 0.22em;
    width: 0.1em;
    height: 0.1em;
    border-radius: 50%;
    background: currentColor;
    box-shadow: 0 0.23em 0 currentColor, 0 0.46em 0 currentColor;
  }
}

/* ---------- 右箭头（线 + 尖） ---------- */
.ai--arrow {
  .ai__p1 {
    display: block;
    left: 0.12em;
    right: 0.3em;
    top: 50%;
    margin-top: -0.0375em;
    height: 0.075em;
    border-radius: 0.04em;
    background: currentColor;
  }
  .ai__p2 {
    display: block;
    right: 0.14em;
    top: 0.32em;
    width: 0.22em;
    height: 0.22em;
    border-top: 0.075em solid currentColor;
    border-right: 0.075em solid currentColor;
    transform: rotate(45deg);
  }
}

/* ---------- 右尖括号 ---------- */
.ai--next {
  .ai__p1 {
    display: block;
    left: 0.36em;
    top: 0.22em;
    width: 0.28em;
    height: 0.28em;
    border-top: 0.075em solid currentColor;
    border-right: 0.075em solid currentColor;
    border-radius: 0.03em;
    transform: rotate(45deg);
  }
}

/* ---------- 星芒 ---------- */
.ai--sparkle {
  .ai__p1,
  .ai__p2 {
    display: block;
    left: 50%;
    top: 50%;
    width: 0.3em;
    height: 1em;
    margin-left: -0.15em;
    margin-top: -0.5em;
    border-radius: 50%;
    background: currentColor;
    transform: scaleX(0.42);
  }
  .ai__p2 { transform: rotate(90deg) scaleX(0.42); }
  .ai__p3 {
    display: block;
    left: 50%;
    top: 50%;
    width: 0.16em;
    height: 0.16em;
    margin-left: -0.08em;
    margin-top: -0.08em;
    border-radius: 50%;
    background: currentColor;
  }
}

/* ---------- 扫码 ---------- */
.ai--scan {
  .ai__p1 {
    display: block;
    left: 0.12em;
    right: 0.12em;
    top: 0.14em;
    bottom: 0.14em;
    border: 0.065em solid currentColor;
    border-radius: 0.1em;
    opacity: 0.42;
  }
  .ai__p2 {
    display: block;
    left: 0.22em;
    right: 0.22em;
    top: 50%;
    margin-top: -0.04em;
    height: 0.08em;
    border-radius: 0.04em;
    background: currentColor;
  }
  .ai__p3,
  .ai__p4 {
    display: block;
    width: 0.22em;
    height: 0.22em;
    border: 0.075em solid currentColor;
  }
  .ai__p3 {
    left: 0.06em;
    top: 0.08em;
    border-right: none;
    border-bottom: none;
    border-radius: 0.08em 0 0 0;
  }
  .ai__p4 {
    right: 0.06em;
    bottom: 0.08em;
    border-left: none;
    border-top: none;
    border-radius: 0 0 0.08em 0;
  }
}

/* ---------- 信息 ---------- */
.ai--info {
  .ai__p1 {
    display: block;
    left: 0.08em;
    right: 0.08em;
    top: 0.08em;
    bottom: 0.08em;
    border: 0.075em solid currentColor;
    border-radius: 50%;
  }
  .ai__p2 {
    display: block;
    left: 50%;
    margin-left: -0.045em;
    top: 0.24em;
    width: 0.09em;
    height: 0.09em;
    border-radius: 50%;
    background: currentColor;
  }
  .ai__p3 {
    display: block;
    left: 50%;
    margin-left: -0.045em;
    top: 0.4em;
    width: 0.09em;
    height: 0.3em;
    border-radius: 0.05em;
    background: currentColor;
  }
}

/* ---------- 清除 / 垃圾桶 ---------- */
.ai--trash {
  .ai__p1 {
    display: block;
    left: 0.23em;
    right: 0.23em;
    top: 0.3em;
    bottom: 0.11em;
    border: 0.075em solid currentColor;
    border-top: none;
    border-radius: 0 0 0.1em 0.1em;
  }
  .ai__p2 {
    display: block;
    left: 0.15em;
    right: 0.15em;
    top: 0.23em;
    height: 0.075em;
    border-radius: 0.04em;
    background: currentColor;
  }
  .ai__p3 {
    display: block;
    left: 0.38em;
    right: 0.38em;
    top: 0.11em;
    height: 0.12em;
    border: 0.06em solid currentColor;
    border-bottom: none;
    border-radius: 0.06em 0.06em 0 0;
  }
  .ai__p4 {
    display: block;
    left: 50%;
    margin-left: -0.03em;
    top: 0.42em;
    width: 0.06em;
    height: 0.24em;
    border-radius: 0.03em;
    background: currentColor;
    opacity: 0.7;
  }
}

/* ---------- 对勾 ---------- */
.ai--check {
  .ai__p1 {
    display: block;
    left: 0.24em;
    top: 0.4em;
    width: 0.24em;
    height: 0.13em;
    border-left: 0.08em solid currentColor;
    border-bottom: 0.08em solid currentColor;
    border-radius: 0.02em;
    transform: rotate(-45deg);
  }
}

/* ---------- 加号 / 关闭 ---------- */
.ai--plus,
.ai--close {
  .ai__p1 {
    display: block;
    left: 0.16em;
    right: 0.16em;
    top: 50%;
    margin-top: -0.0375em;
    height: 0.075em;
    border-radius: 0.04em;
    background: currentColor;
  }
  .ai__p2 {
    display: block;
    top: 0.16em;
    bottom: 0.16em;
    left: 50%;
    margin-left: -0.0375em;
    width: 0.075em;
    border-radius: 0.04em;
    background: currentColor;
  }
}
.ai--close { transform: rotate(45deg); }

/* ---------- 须知 / 文书 ---------- */
.ai--note {
  .ai__p1 {
    display: block;
    left: 0.17em;
    right: 0.17em;
    top: 0.07em;
    bottom: 0.07em;
    border: 0.075em solid currentColor;
    border-radius: 0.09em;
  }
  .ai__p2,
  .ai__p3,
  .ai__p4 {
    display: block;
    left: 0.29em;
    height: 0.06em;
    border-radius: 0.03em;
    background: currentColor;
  }
  .ai__p2 { top: 0.3em; right: 0.29em; }
  .ai__p3 { top: 0.48em; right: 0.29em; opacity: 0.75; }
  .ai__p4 { top: 0.66em; right: 0.44em; opacity: 0.5; }
}

/* ---------- 日轮（艺术元素） ---------- */
.ai--sun {
  .ai__p1 {
    display: block;
    left: 50%;
    top: 50%;
    width: 0.44em;
    height: 0.44em;
    margin-left: -0.22em;
    margin-top: -0.22em;
    border-radius: 50%;
    background: currentColor;
  }
  .ai__p2 {
    display: block;
    left: 0.04em;
    right: 0.04em;
    top: 0.04em;
    bottom: 0.04em;
    border: 0.07em dashed currentColor;
    border-radius: 50%;
    opacity: 0.72;
  }
}

/* ---------- 喇叭 ---------- */
.ai--horn {
  .ai__p1 {
    display: block;
    left: 0.1em;
    top: 0.38em;
    width: 0.15em;
    height: 0.24em;
    border-radius: 0.03em;
    background: currentColor;
  }
  .ai__p2 {
    display: block;
    left: 0.24em;
    top: 0.3em;
    width: 0;
    height: 0;
    border-top: 0.2em solid transparent;
    border-bottom: 0.2em solid transparent;
    border-right: 0.26em solid currentColor;
  }
  .ai__p3 {
    display: block;
    left: 0.56em;
    top: 0.3em;
    width: 0.26em;
    height: 0.4em;
    border: 0.07em solid currentColor;
    border-left: none;
    border-radius: 0 0.2em 0.2em 0;
  }
  .ai__p4 {
    display: block;
    left: 0.56em;
    top: 0.4em;
    width: 0.13em;
    height: 0.2em;
    border: 0.06em solid currentColor;
    border-left: none;
    border-radius: 0 0.1em 0.1em 0;
    opacity: 0.65;
  }
}

/* ---------- 孔雀翎眼（艺术元素） ---------- */
.ai--feather {
  .ai__p1 {
    display: block;
    left: 50%;
    margin-left: -0.23em;
    top: 0.03em;
    width: 0.46em;
    height: 0.66em;
    border: 0.065em solid currentColor;
    border-radius: 50% 50% 50% 50% / 58% 58% 42% 42%;
  }
  .ai__p2 {
    display: block;
    left: 50%;
    margin-left: -0.13em;
    top: 0.19em;
    width: 0.26em;
    height: 0.34em;
    border-radius: 50%;
    background: currentColor;
    opacity: 0.42;
  }
  .ai__p3 {
    display: block;
    left: 50%;
    margin-left: -0.075em;
    top: 0.29em;
    width: 0.15em;
    height: 0.15em;
    border-radius: 50%;
    background: currentColor;
  }
  .ai__p4 {
    display: block;
    left: 50%;
    margin-left: -0.035em;
    top: 0.67em;
    width: 0.07em;
    height: 0.28em;
    border-radius: 0.04em;
    background: currentColor;
    opacity: 0.65;
  }
}
</style>
