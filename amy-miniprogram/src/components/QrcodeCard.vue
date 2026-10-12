<script setup lang="ts">
import { computed } from 'vue'
import type { ReserveRecord } from '@/types'
import { generateQRMatrix } from '@/utils/qrcode'

const props = defineProps<{ record: ReserveRecord }>()

const matrix = computed(() => generateQRMatrix(props.record.reservationNum))

/** 渲染为展开的一维数组，供 grid 布局使用 */
const cells = computed(() => {
  const arr: { dark: boolean }[] = []
  for (let r = 0; r < matrix.value.size; r++) {
    for (let c = 0; c < matrix.value.size; c++) {
      arr.push({ dark: matrix.value.data[r][c] })
    }
  }
  return arr
})

const gridStyle = computed(() => ({
  gridTemplateColumns: `repeat(${matrix.value.size}, 1fr)`,
}))
</script>

<template>
  <view class="qr-card">
    <view class="qr-card__code">{{ record.reservationNum }}</view>
    <view class="qr-grid" :style="gridStyle">
      <view
        v-for="(c, i) in cells"
        :key="i"
        class="qr-cell"
        :class="{ dark: c.dark }"
      />
    </view>
    <view class="qr-card__label">入场核验码</view>
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
  color: #86909c;
  margin-bottom: 24rpx;
  letter-spacing: 1rpx;
}
.qr-grid {
  display: grid;
  width: 400rpx;
  height: 400rpx;
  padding: 16rpx;
  background: #fff;
  border: 1px solid #e5e6eb;
  border-radius: 16rpx;
  gap: 0;
}
.qr-cell {
  background: #fff;
}
.qr-cell.dark {
  background: #1f2329;
}
.qr-card__label {
  margin-top: 20rpx;
  font-size: 26rpx;
  color: #86909c;
}
</style>
