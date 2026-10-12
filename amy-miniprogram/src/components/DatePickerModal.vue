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
            :class="{ full: s.full, selected: selectedTime === s.time }"
            @tap="!s.full && emit('pick-time', s.time)"
          >
            <view class="time-cell__t">{{ hmsToHm(s.time) }}</view>
            <view class="time-cell__sub">(参观{{ dwellTime || 30 }}分钟)</view>
            <view class="time-cell__state">{{ s.full ? '约满' : `余${s.remain}号` }}</view>
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
  color: #86909c;
  font-size: 40rpx;
}
.block {
  margin-bottom: 28rpx;
}
.block__hd {
  font-size: 26rpx;
  color: #1f2329;
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
  background: #fff;
  border: 1px solid #e5e6eb;
  border-radius: 16rpx;
  font-size: 26rpx;
  color: #1f2329;
}
.date-pill.selected {
  background: #3b7dff;
  color: #fff;
  border-color: #3b7dff;
}
.time-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20rpx;
}
.time-cell {
  padding: 24rpx 12rpx;
  border-radius: 24rpx;
  background: #e8f3ff;
  text-align: center;
  position: relative;
}
.time-cell__t {
  font-size: 32rpx;
  font-weight: 600;
  color: #3b7dff;
}
.time-cell__sub {
  font-size: 22rpx;
  color: #3b7dff;
  opacity: 0.7;
  margin-top: 4rpx;
}
.time-cell__state {
  font-size: 22rpx;
  color: #86909c;
  margin-top: 4rpx;
}
.time-cell.full {
  background: #f7f8fa;
}
.time-cell.full .time-cell__t,
.time-cell.full .time-cell__sub {
  color: #c9cdd4;
}
.time-cell.full .time-cell__state {
  color: #c9cdd4;
}
.time-cell.selected {
  background: #3b7dff;
}
.time-cell.selected .time-cell__t,
.time-cell.selected .time-cell__sub,
.time-cell.selected .time-cell__state {
  color: #fff;
  opacity: 1;
}
.time-cell__check {
  position: absolute;
  top: 8rpx;
  right: 16rpx;
  color: #fff;
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
  background: #fff;
  border: 1px solid #e5e6eb;
  color: #1f2329;
}
.btn--primary {
  background: #00b96b;
  color: #fff;
}
</style>
