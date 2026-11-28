<script setup lang="ts">
import { computed } from 'vue'
import type { ReserveRecord } from '@/types'
import { RESERVE_STATUS } from '@/types'
import StatusBadge from './StatusBadge.vue'

const props = defineProps<{ record: ReserveRecord }>()
const emit = defineEmits<{ (e: 'click', orderNum: string): void }>()

/** status_msg 与状态中文文案相同（或为空）时不重复显示 */
const showStatusMsg = computed(() => {
  const msg = (props.record.statusMsg || '').trim()
  if (!msg) return false
  const statusText = RESERVE_STATUS[props.record.reservationStatus]?.text || ''
  return msg !== statusText && (msg != '已核销' && msg != '已取消' && msg != '已过期' && msg != '已核销')
})
</script>

<template>
  <view class="record-card" @tap="emit('click', record.reservationNum)">
    <view class="record-card__top">
      <view class="record-card__name">{{ record.projectName }}</view>
      <StatusBadge :status="record.reservationStatus" />
    </view>
    <view v-if="showStatusMsg" class="record-card__status-msg">{{ record.statusMsg }}</view>
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
  @include art-card(0.6, 18px, 32rpx);
  margin: $art-gap 24rpx 0;
  padding: 32rpx;
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
  color: $art-text-strong;
  flex: 1;
  line-height: 1.4;
}
.record-card__status-msg {
  font-size: 24rpx;
  color: #ff9c9c;
  line-height: 1.5;
  margin-top: 12rpx;
}
.record-card__time {
  font-size: 26rpx;
  color: $art-text-strong;
  margin-top: 20rpx;
  font-weight: 500;
}
.record-card__info {
  font-size: 24rpx;
  color: $art-text-dim;
  line-height: 1.9;
  margin-top: 12rpx;
}
.record-card__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 24rpx;
  margin-top: 24rpx;
  border-top: 1px solid $art-divider;
  color: $art-accent;
  font-size: 26rpx;
}
.record-card__footer .arrow {
  color: $art-text-dim;
  font-size: 32rpx;
}
</style>
