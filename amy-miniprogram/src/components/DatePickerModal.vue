<script setup lang="ts">
import type { TimeSlot } from '@/types'
import { hmsToHm } from '@/utils'

defineProps<{
  show: boolean
  dates: string[]
  slots: TimeSlot[]
  selectedDate: string
  selectedTime: string
  dwellTime?: number
}>()

const emit = defineEmits<{
  (e: 'pick-date', date: string): void
  (e: 'pick-time', time: string): void
  (e: 'confirm'): void
  (e: 'close'): void
}>()
</script>

<template>
  <view v-if="show" class="modal-mask" @tap="emit('close')">
    <view class="sheet" @tap.stop>
      <view class="sheet__title">
        <text>选择预约日期</text>
        <text class="sheet__close" @tap="emit('close')">×</text>
      </view>

      <view class="block">
        <view class="block__hd">选择日期</view>
        <view class="date-pills">
          <view
            v-for="d in dates"
            :key="d"
            class="date-pill"
            :class="{ selected: selectedDate === d }"
            @tap="emit('pick-date', d)"
          >
            {{ d }}
          </view>
        </view>
      </view>

      <view class="block">
        <view class="block__hd">选择时段</view>
        <view class="time-grid">
          <view
            v-for="s in slots"
            :key="s.time"
            class="time-cell"
            :class="{ full: s.full, closed: s.closed, selected: selectedTime === s.time }"
            @tap="!s.full && emit('pick-time', s.time)"
          >
            <view class="time-cell__t">{{ hmsToHm(s.time) }}</view>
            <view class="time-cell__sub">(参观{{ dwellTime || 30 }}分钟)</view>
            <view class="time-cell__state">{{ s.closed ? '不可预约' : s.full ? '约满' : `余${s.remain}号` }}</view>
            <view v-if="selectedTime === s.time" class="time-cell__check">✓</view>
          </view>
        </view>
      </view>

      <view class="sheet__actions">
        <view class="btn btn--cancel" @tap="emit('close')">取消</view>
        <view class="btn btn--primary" @tap="emit('confirm')">确定</view>
      </view>
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
  max-height: 82vh;
  overflow-y: auto;
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
  color: $art-text-dim;
  font-size: 40rpx;
}
.block {
  margin-bottom: 28rpx;
}
.block__hd {
  font-size: 26rpx;
  color: $art-text-strong;
  font-weight: 600;
  margin-bottom: 20rpx;
}
.date-pills {
  display: flex;
  flex-wrap: wrap;
  gap: 20rpx;
}
.date-pill {
  padding: 16rpx 28rpx;
  @include art-inset(16rpx);
  font-size: 26rpx;
  color: $art-text-strong;
}
.date-pill.selected {
  @include art-gold-button(135deg);
  
  border-color: transparent;
}
.time-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20rpx;
}
.time-cell {
  padding: 24rpx 12rpx;
  border-radius: 24rpx;
  @include art-inset(24rpx);
  background: rgba(240, 207, 148, 0.08);
  text-align: center;
  position: relative;
}
.time-cell__t {
  font-size: 32rpx;
  font-weight: 600;
  color: $art-accent;
}
.time-cell__sub {
  font-size: 22rpx;
  color: $art-accent;
  opacity: 0.7;
  margin-top: 4rpx;
}
.time-cell__state {
  font-size: 22rpx;
  color: $art-text-dim;
  margin-top: 4rpx;
}
.time-cell.full {
  background: rgba(255, 255, 255, 0.06);
}
.time-cell.full .time-cell__t,
.time-cell.full .time-cell__sub {
  color: $art-text-dim;
}
.time-cell.full .time-cell__state {
  color: $art-text-dim;
}
/* 不可预约：与「约满」同样是置灰不可点，用虚线边框区分成因 */
.time-cell.closed {
  background: rgba(255, 255, 255, 0.05);
  border: 1px dashed rgba(255, 255, 255, 0.22);
}
.time-cell.closed .time-cell__t,
.time-cell.closed .time-cell__sub {
  color: $art-text-dim;
}
.time-cell.closed .time-cell__state {
  color: $art-text-dim;
}
.time-cell.selected {
  @include art-gold-button(135deg);
}
.time-cell.selected .time-cell__t,
.time-cell.selected .time-cell__sub,
.time-cell.selected .time-cell__state {
  color: $art-on-gold;
  opacity: 1;
}
.time-cell__check {
  position: absolute;
  top: 8rpx;
  right: 16rpx;
  color: $art-on-gold;
  font-size: 28rpx;
}
.sheet__actions {
  display: flex;
  gap: 20rpx;
  margin-top: 12rpx;
}
.btn {
  flex: 1;
  padding: 26rpx;
  border-radius: 56rpx;
  text-align: center;
  font-size: 30rpx;
}
.btn--cancel {
  @include art-inset(56rpx);
  background: rgba(255, 255, 255, 0.07);
  color: $art-text-strong;
}
.btn--primary {
  @include art-gold-button();
}
</style>
