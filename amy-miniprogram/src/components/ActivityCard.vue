<script setup lang="ts">
import { computed } from 'vue'
import type { Activity } from '@/types'
import { hmsToHm } from '@/utils'

const props = defineProps<{ activity: Activity }>()
const emit = defineEmits<{ (e: 'book', id: number): void }>()

/** 可预约日期区间（后端直接下发开始/结束日期） */
const dateRange = computed(() => {
  const { reservationStartDate, reservationEndDate } = props.activity
  if (!reservationStartDate || !reservationEndDate) return ''
  return `${reservationStartDate} ~ ${reservationEndDate}`
})

/** 每日开放时段 */
const timeRange = computed(() =>
  `${hmsToHm(props.activity.reservationStartTime)} ~ ${hmsToHm(props.activity.reservationEndTime)}`
)

/** 号源总数与剩余号数均由后端下发 */
const totalNum = computed(() => props.activity.totalReservationCount || 0)
const remainNum = computed(() => Math.max(0, props.activity.remainingReservationCount || 0))
const bookedNum = computed(() => Math.max(0, totalNum.value - remainNum.value))

const bookedPercent = computed(() => {
  if (!totalNum.value) return 0
  return Math.round((bookedNum.value / totalNum.value) * 100)
})

/** 号源状态由后端判定：1充足 2紧张 3已满 */
const remainState = computed<'plenty' | 'tight' | 'soldout'>(() => {
  if (props.activity.reservationStatus === 3 || remainNum.value <= 0) return 'soldout'
  if (props.activity.reservationStatus === 2) return 'tight'
  return 'plenty'
})

const remainStateLabel = computed(() => {
  if (remainState.value === 'soldout') return '已约满'
  if (remainState.value === 'tight') return '号源紧张'
  return '充足号源'
})
</script>

<template>
  <view class="activity-card">
    <view class="activity-card__pic">
      <image class="activity-card__img" :src="activity.coverArtUrl" mode="aspectFill" />
      <view class="activity-card__tag">📍 艺术空间</view>
      <view class="activity-card__badge" :class="`badge--${remainState}`">
        {{ remainStateLabel }}
      </view>
    </view>
    <view class="activity-card__body">
      <view class="activity-card__title text-ellipsis">{{ activity.projectName }}</view>

      <view class="activity-card__meta">
        <view class="meta-row">
          <text class="meta-icon">📅</text>
          <text class="meta-text">{{ dateRange }}</text>
        </view>
        <view class="meta-row">
          <text class="meta-icon">⏰</text>
          <text class="meta-text">{{ timeRange }}</text>
        </view>
      </view>

      <!-- 预约统计 -->
      <view class="stats" :class="`stats--${remainState}`">
        <view class="stats__header">
          <view class="stats__remain">
            <text class="stats__remain-label">剩余号数</text>
            <view class="stats__remain-value">
              <text class="stats__remain-num">{{ remainNum }}</text>
              <text class="stats__remain-total">/ {{ totalNum }}</text>
            </view>
          </view>
          <view class="stats__booked">
            <text class="stats__booked-num">{{ bookedPercent }}%</text>
            <text class="stats__booked-label">已预约</text>
          </view>
        </view>
        <view class="stats__bar">
          <view class="stats__bar-fill" :style="{ width: bookedPercent + '%' }"></view>
        </view>
      </view>

      <view class="activity-card__footer">
        <view class="activity-card__deadline">
          <text class="deadline-label">截止</text>
          <text class="deadline-time">{{ hmsToHm(activity.cutOffTime) }}</text>
        </view>
        <view
          class="activity-card__btn"
          :class="{ 'activity-card__btn--disabled': remainState === 'soldout' }"
          @tap="remainState !== 'soldout' && emit('book', activity.projectId)"
        >
          {{ remainState === 'soldout' ? '已约满' : '立即预约' }}
        </view>
      </view>
    </view>
  </view>
</template>

<style scoped lang="scss">
.activity-card {
  background: #fff;
  border-radius: 32rpx;
  overflow: hidden;
  margin-bottom: 28rpx;
  box-shadow: 0 4rpx 24rpx rgba(31, 35, 41, 0.04);
}
.activity-card__pic {
  position: relative;
  height: 360rpx;
}
.activity-card__img {
  width: 100%;
  height: 100%;
}
.activity-card__tag {
  position: absolute;
  top: 24rpx;
  left: 24rpx;
  background: rgba(255, 255, 255, 0.92);
  padding: 8rpx 20rpx;
  border-radius: 24rpx;
  font-size: 22rpx;
  color: #3b7dff;
  font-weight: 600;
  backdrop-filter: blur(8rpx);
}
.activity-card__badge {
  position: absolute;
  top: 24rpx;
  right: 24rpx;
  padding: 8rpx 18rpx;
  border-radius: 24rpx;
  font-size: 22rpx;
  font-weight: 600;
  backdrop-filter: blur(8rpx);
}
.badge--plenty {
  background: rgba(7, 193, 96, 0.92);
  color: #fff;
}
.badge--tight {
  background: rgba(255, 125, 0, 0.92);
  color: #fff;
}
.badge--soldout {
  background: rgba(134, 144, 156, 0.92);
  color: #fff;
}
.activity-card__body {
  padding: 28rpx 32rpx 24rpx;
}
.activity-card__title {
  font-size: 32rpx;
  font-weight: 600;
  color: #1f2329;
  margin-bottom: 20rpx;
  line-height: 1.4;
}

/* 元信息 */
.activity-card__meta {
  display: flex;
  flex-direction: column;
  gap: 10rpx;
  margin-bottom: 24rpx;
}
.meta-row {
  display: flex;
  align-items: center;
  font-size: 24rpx;
  color: #4e5969;
}
.meta-icon {
  font-size: 24rpx;
  margin-right: 8rpx;
  opacity: 0.7;
}
.meta-text {
  font-size: 24rpx;
}

/* 统计区 */
.stats {
  position: relative;
  background: linear-gradient(135deg, #f7faff 0%, #eef3ff 100%);
  border-radius: 20rpx;
  padding: 22rpx 24rpx 24rpx;
  margin-bottom: 24rpx;
  border: 1rpx solid rgba(59, 125, 255, 0.08);
}
.stats--tight {
  background: linear-gradient(135deg, #fff7e6 0%, #ffe7c2 100%);
  border-color: rgba(255, 125, 0, 0.15);
}
.stats--soldout {
  background: linear-gradient(135deg, #f5f6f7 0%, #e8eaed 100%);
  border-color: rgba(134, 144, 156, 0.12);
}

.stats__header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 16rpx;
}

.stats__remain-label,
.stats__booked-label {
  display: block;
  font-size: 22rpx;
  color: #86909c;
  margin-bottom: 4rpx;
}

.stats__remain-value {
  display: flex;
  align-items: baseline;
}
.stats__remain-num {
  font-size: 52rpx;
  font-weight: 700;
  color: #3b7dff;
  line-height: 1;
  font-family: 'DIN Alternate', 'Helvetica Neue', sans-serif;
  letter-spacing: -1rpx;
}
.stats--tight .stats__remain-num {
  color: #ff7d00;
}
.stats--soldout .stats__remain-num {
  color: #86909c;
}
.stats__remain-total {
  font-size: 24rpx;
  color: #86909c;
  margin-left: 6rpx;
  font-weight: 500;
}

.stats__booked {
  text-align: right;
}
.stats__booked-num {
  display: block;
  font-size: 32rpx;
  font-weight: 700;
  color: #1f2329;
  line-height: 1;
  font-family: 'DIN Alternate', 'Helvetica Neue', sans-serif;
}
.stats__booked-label {
  margin: 4rpx 0 0;
  color: #86909c;
}

/* 进度条 */
.stats__bar {
  height: 12rpx;
  background: rgba(255, 255, 255, 0.7);
  border-radius: 6rpx;
  overflow: hidden;
  position: relative;
  box-shadow: inset 0 1rpx 2rpx rgba(31, 35, 41, 0.06);
}
.stats__bar-fill {
  height: 100%;
  background: linear-gradient(90deg, #3b7dff 0%, #5b95ff 100%);
  border-radius: 6rpx;
  transition: width 0.6s cubic-bezier(0.16, 1, 0.3, 1);
  box-shadow: 0 2rpx 6rpx rgba(59, 125, 255, 0.4);
  position: relative;
}
.stats__bar-fill::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, transparent 0%, rgba(255, 255, 255, 0.35) 50%, transparent 100%);
  animation: shimmer 2.4s linear infinite;
}
@keyframes shimmer {
  0% { transform: translateX(-100%); }
  100% { transform: translateX(100%); }
}
.stats--tight .stats__bar-fill {
  background: linear-gradient(90deg, #ff7d00 0%, #ffa940 100%);
  box-shadow: 0 2rpx 6rpx rgba(255, 125, 0, 0.4);
}
.stats--soldout .stats__bar-fill {
  background: linear-gradient(90deg, #c9cdd4 0%, #86909c 100%);
  box-shadow: none;
}

/* 底部操作区 */
.activity-card__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 4rpx;
}
.activity-card__deadline {
  display: flex;
  align-items: baseline;
  gap: 8rpx;
}
.deadline-label {
  font-size: 24rpx;
  color: #86909c;
}
.deadline-time {
  font-size: 30rpx;
  font-weight: 700;
  color: #1f2329;
  font-family: 'DIN Alternate', 'Helvetica Neue', sans-serif;
}

.activity-card__btn {
  background: linear-gradient(90deg, #3b7dff, #5b95ff);
  color: #fff;
  padding: 18rpx 48rpx;
  border-radius: 48rpx;
  font-size: 28rpx;
  font-weight: 600;
  box-shadow: 0 8rpx 24rpx rgba(59, 125, 255, 0.35);
  letter-spacing: 1rpx;
}
.activity-card__btn--disabled {
  background: #c9cdd4;
  box-shadow: none;
}
</style>