<script setup lang="ts">
import { computed } from 'vue'
import type { ReserveRecord } from '@/types'
import { generateQRMatrix } from '@/utils/qrcode'
import { formatCountdown } from '@/utils/verifyCodeCountdown'

const props = defineProps<{
  record: ReserveRecord
  /** 核销码剩余有效秒数；不传表示该订单不启用倒计时 */
  countdown?: number
  /** 是否正在获取新核销码 */
  refreshing?: boolean
}>()

/**
 * 二维码失效状态：预约失败(-1)/已过期(2)/已取消(3)/预约中(4)
 * 这些状态下后端不允许核销，真实核销码不下发展示，渲染坏码占位。
 */
const BROKEN_STATUS = [-1, 2, 3, 4]
const broken = computed(() => BROKEN_STATUS.includes(props.record.reservationStatus))

/**
 * 二维码内容：JSON 携带预约单号 + 核销码，
 * 核销端扫码后拆分调用 /reservation/verify/{orderNum}/{verifyCode}
 */
const qrContent = computed(() =>
  JSON.stringify({
    orderNum: props.record.reservationNum,
    verifyCode: props.record.verifyCode,
  })
)

const matrix = computed(() => generateQRMatrix(qrContent.value))

/** 正常二维码：渲染为展开的一维数组，供 grid 布局使用 */
const cells = computed(() => {
  const arr: { dark: boolean }[] = []
  for (let r = 0; r < matrix.value.size; r++) {
    for (let c = 0; c < matrix.value.size; c++) {
      arr.push({ dark: matrix.value.data[r][c] })
    }
  }
  return arr
})

/**
 * 坏码占位：固定种子的伪随机黑白块（每次渲染一致，看起来像一张扫不动的二维码），
 * 叠加中央遮罩提示，确保不可能被误扫。
 */
const BROKEN_SIZE = 25
const brokenCells = computed(() => {
  const arr: { dark: boolean }[] = []
  let seed = 20261121
  for (let i = 0; i < BROKEN_SIZE * BROKEN_SIZE; i++) {
    seed = (seed * 1103515245 + 12345) >>> 0
    arr.push({ dark: ((seed >>> 16) & 1) === 1 })
  }
  return arr
})

const size = computed(() => (broken.value ? BROKEN_SIZE : matrix.value.size))
const displayCells = computed(() => (broken.value ? brokenCells.value : cells.value))

const gridStyle = computed(() => ({
  gridTemplateColumns: `repeat(${size.value}, 1fr)`,
}))
</script>

<template>
  <view class="qr-card">
    <!-- <view class="qr-card__code">{{ record.reservationNum }}</view> -->
    <view class="qr-grid" :style="gridStyle">
      <view
        v-for="(c, i) in displayCells"
        :key="i"
        class="qr-cell"
        :class="{ dark: c.dark }"
      />
      <!-- 失效遮罩：盖住伪随机块，明确告知不可用 -->
      <view v-if="broken" class="qr-broken">
        <view class="qr-broken__badge">
          <view class="qr-broken__x">
            <view class="qr-broken__x-line qr-broken__x-line--a" />
            <view class="qr-broken__x-line qr-broken__x-line--b" />
          </view>
        </view>
        <view class="qr-broken__text">{{props.record.reservationStatus == 4 ? '预约中，请稍候':'二维码已失效'}}</view>
      </view>
    </view>
    <view class="qr-card__label">{{ broken ? '当前状态无法出示入场核验码' : '入场核验码' }}</view>
    <!-- 倒计时由页面调度（只在打开详情页期间生效）；失效态不展示 -->
    <view v-if="!broken && countdown !== undefined" class="qr-card__countdown">
      <text v-if="refreshing" class="qr-card__countdown-hint">正在更新核验码…</text>
      <text v-else class="qr-card__countdown-hint">
        核验码 <text class="qr-card__countdown-time">{{ formatCountdown(countdown) }}</text> 后自动更新
      </text>
    </view>
  </view>
</template>

<style scoped lang="scss">
.qr-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 24rpx;
}
.qr-card__code {
  font-size: 24rpx;
  color: $art-text-dim;
  margin-bottom: 24rpx;
  letter-spacing: 1rpx;
}
.qr-grid {
  display: grid;
  width: 400rpx;
  height: 400rpx;
  padding: 16rpx;
  background: #fff;
  border: 1px solid rgba(240, 207, 148, 0.28);
  border-radius: 16rpx;
  gap: 0;
  position: relative;
}
.qr-cell {
  background: #fff;
}
.qr-cell.dark {
  background: #1f2329;
}
/* 失效态：整体压暗 + 中央禁用徽标 */
.qr-broken {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16rpx;
  background: rgba(255, 255, 255, 0.55);
}
.qr-broken__badge {
  width: 96rpx;
  height: 96rpx;
  border-radius: 50%;
  background: #fdecec;
  border: 2rpx solid rgba(245, 63, 63, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
}
.qr-broken__x {
  position: relative;
  width: 40rpx;
  height: 40rpx;
}
.qr-broken__x-line {
  position: absolute;
  top: 50%;
  left: 0;
  width: 100%;
  height: 4rpx;
  border-radius: 2rpx;
  background: #f53f3f;
}
.qr-broken__x-line--a {
  transform: rotate(45deg);
}
.qr-broken__x-line--b {
  transform: rotate(-45deg);
}
.qr-broken__text {
  font-size: 24rpx;
  color: #f53f3f;
  font-weight: 600;
  background: rgba(255, 255, 255, 0.9);
  padding: 4rpx 16rpx;
  border-radius: 8rpx;
}
.qr-card__label {
  margin-top: 20rpx;
  font-size: 26rpx;
  color: $art-text-dim;
}
.qr-card__countdown {
  margin-top: 12rpx;
  padding: 8rpx 24rpx;
  border-radius: 24rpx;
  background: rgba(240, 207, 148, 0.12);
}
.qr-card__countdown-hint {
  font-size: 22rpx;
  color: $art-text-dim;
}
.qr-card__countdown-time {
  font-size: 26rpx;
  font-weight: 600;
  color: $art-accent;
}
</style>
