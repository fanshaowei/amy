<script setup lang="ts">
import type { ReserveRecord } from '@/types'
import StatusBadge from './StatusBadge.vue'

defineProps<{ record: ReserveRecord }>()
const emit = defineEmits<{ (e: 'click', orderNum: string): void }>()
</script>

<template>
  <view class="record-card" @tap="emit('click', record.reservationNum)">
    <view class="record-card__top">
      <view class="record-card__name">{{ record.projectName }}</view>
      <StatusBadge :status="record.reservationStatus" />
    </view>
    <view class="record-card__time">预约时间：{{ record.reservationTime }}</view>
    <view class="record-card__info">
      <view>单号：{{ record.reservationNum }}</view>
      <view>预约人：{{ record.name }}</view>
      <view>预约人数：{{ record.guestsNum }}</view>
    </view>
    <view class="record-card__footer">
      <text>查看预约详情</text>
      <text class="arrow">›</text>
    </view>
  </view>
</template>

<style scoped lang="scss">
.record-card {
  background: #fff;
  margin: 24rpx;
  border-radius: 32rpx;
  padding: 32rpx;
  box-shadow: 0 4rpx 24rpx rgba(31, 35, 41, 0.04);
}
.record-card__top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24rpx;
}
.record-card__name {
  font-size: 32rpx;
  font-weight: 600;
  color: #1f2329;
  flex: 1;
  line-height: 1.4;
}
.record-card__time {
  font-size: 26rpx;
  color: #1f2329;
  margin-top: 20rpx;
  font-weight: 500;
}
.record-card__info {
  font-size: 24rpx;
  color: #86909c;
  line-height: 1.9;
  margin-top: 12rpx;
}
.record-card__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 24rpx;
  margin-top: 24rpx;
  border-top: 1px solid #f2f3f5;
  color: #3b7dff;
  font-size: 26rpx;
}
.record-card__footer .arrow {
  color: #c9cdd4;
  font-size: 32rpx;
}
</style>
