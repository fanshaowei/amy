<script setup lang="ts">
import { computed } from 'vue'
import type { Activity } from '@/types'
import { hmsToHm } from '@/utils'
import AppIcon from '@/components/AppIcon.vue'

const props = withDefaults(defineProps<{ activity: Activity; index?: number }>(), { index: 0 })
const emit = defineEmits<{ (e: 'book', id: number): void }>()

/** 卡片序号：01 / 02 …用衬线数字做艺术化编号 */
const serial = computed(() => String(props.index + 1).padStart(2, '0'))

/** 可预约日期区间（后端直接下发开始/结束日期） */
const dateRange = computed(() => {
  const { reservationStartDate, reservationEndDate } = props.activity
  if (!reservationStartDate || !reservationEndDate) return ''
  if(reservationStartDate == reservationEndDate) return `${reservationStartDate}`
  return `${reservationStartDate} ~ ${reservationEndDate}`
})

/** 每日开放时段 */
const timeRange = computed(() =>
  hmsToHm(props.activity.reservationStartTime) == hmsToHm(props.activity.reservationEndTime) ? `${hmsToHm(props.activity.reservationStartTime)}`:
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
  <view class="activity-card" :class="`activity-card--${remainState}`">
    <!-- 封面即海报底图，深色渐变从下往上压出可读的文字区 -->
    <image class="activity-card__img" :src="activity.coverArtUrl" mode="aspectFill" />
    <view class="activity-card__shade"></view>

    <view class="activity-card__inner">
      <view class="activity-card__top">
        <view class="activity-card__serial">
          <text class="activity-card__serial-num art-serif-num">{{ serial }}</text>
          <text class="activity-card__serial-en">PROJECT</text>
        </view>
        <view class="activity-card__badge" :class="`badge--${remainState}`">
          <text class="activity-card__badge-dot"></text>
          <text>{{ remainStateLabel }}</text>
        </view>
      </view>

      <view class="activity-card__main">
        <view class="activity-card__eyebrow">
          <AppIcon name="location" :size="20" color="#f0cf94" />
          <text>太阳宫 · 艺术空间</text>
        </view>
        <view class="activity-card__title text-ellipsis">{{ activity.projectName }}</view>

        <view class="activity-card__meta">
          <view class="meta-row">
            <AppIcon name="calendar" :size="24" color="#f0cf94" />
            <text>{{ dateRange }}</text>
          </view>
          <view class="meta-row">
            <AppIcon name="clock" :size="24" color="#8ce0d2" />
            <text>{{ timeRange }}</text>
          </view>
        </view>

        <view class="activity-card__rule"></view>

        <view class="activity-card__bottom">
          <view class="stats">
            <view class="stats__line">
              <text class="stats__num">{{ remainNum }}</text>
              <text class="stats__total">/ {{ totalNum }} 余号</text>
              <text class="stats__pct">{{ bookedPercent }}% 已约</text>
            </view>
            <text class="stats__deadline">
              预约截止 {{ hmsToHm(activity.cutOffTime) }}
            </text>
          </view>
          <view
            class="activity-card__btn"
            :class="{ 'activity-card__btn--disabled': remainState === 'soldout' }"
            @tap="remainState !== 'soldout' && emit('book', activity.projectId)"
          >
            <text>{{ remainState === 'soldout' ? '已约满' : '立即预约' }}</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 底部一条金→青的号源进度，取代原来带白边的内嵌统计块 -->
    <view class="activity-card__progress">
      <view class="activity-card__progress-fill" :style="{ width: bookedPercent + '%' }"></view>
    </view>
  </view>
</template>

<style scoped lang="scss">
.activity-card {
  position: relative;
  overflow: hidden;
  border-radius: 32rpx;
  margin-bottom: 32rpx;
  background-color: #14264c;
  box-shadow: 0 20rpx 52rpx rgba(18, 32, 70, 0.26);
}
.activity-card__img {
  position: absolute;
  left: 0;
  right: 0;
  top: 0;
  bottom: 0;
  width: 100%;
  height: 100%;
}
/* 顶部保留画面，底部压深到接近实色，保证白字可读 */
.activity-card__shade {
  position: absolute;
  left: 0;
  right: 0;
  top: 0;
  bottom: 0;
  background-image: linear-gradient(
    180deg,
    rgba(12, 24, 52, 0.44) 0%,
    rgba(12, 24, 52, 0.08) 24%,
    rgba(12, 24, 52, 0.72) 56%,
    rgba(10, 22, 48, 0.97) 100%
  );
}
.activity-card--soldout .activity-card__shade {
  background-color: rgba(28, 34, 52, 0.32);
}

.activity-card__inner {
  position: relative;
  z-index: 2;
  box-sizing: border-box;
  min-height: 480rpx;
  padding: 26rpx 30rpx 34rpx;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

/* ---------- 顶部：编号 + 状态 ---------- */
.activity-card__top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}
.activity-card__serial {
  display: flex;
  align-items: baseline;
  gap: 10rpx;
  text-shadow: 0 2rpx 12rpx rgba(8, 18, 42, 0.5);
}
.activity-card__serial-num {
  font-size: 44rpx;
  line-height: 1;
  color: #f0cf94;
}
.activity-card__serial-en {
  font-size: 17rpx;
  letter-spacing: 3rpx;
  color: rgba(240, 207, 148, 0.82);
}
.activity-card__badge {
  display: flex;
  align-items: center;
  gap: 8rpx;
  padding: 8rpx 18rpx;
  border-radius: 24rpx;
  font-size: 21rpx;
  font-weight: 600;
  color: #fff;
  background: rgba(10, 22, 48, 0.5);
  -webkit-backdrop-filter: blur(10px) saturate(160%);
  backdrop-filter: blur(10px) saturate(160%);
}
.activity-card__badge-dot {
  width: 10rpx;
  height: 10rpx;
  border-radius: 50%;
  background: #6ee7b7;
}
.badge--tight .activity-card__badge-dot { background: #ffb35c; }
.badge--soldout .activity-card__badge-dot { background: #b9c0d0; }

/* ---------- 主信息 ---------- */
.activity-card__main {
  min-width: 0;
}
.activity-card__eyebrow {
  display: flex;
  align-items: center;
  gap: 8rpx;
  font-size: 20rpx;
  letter-spacing: 2rpx;
  color: #f0cf94;
  margin-bottom: 10rpx;
}
.activity-card__title {
  /* 自定义组件内不能使用 App.vue 的全局 text-ellipsis，这里内聚一份 */
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  font-size: 40rpx;
  font-weight: 700;
  letter-spacing: 2rpx;
  line-height: 1.3;
  color: #fffaf0;
  text-shadow: 0 2rpx 14rpx rgba(8, 18, 42, 0.4);
  margin-bottom: 16rpx;
}
.activity-card__meta {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}
.meta-row {
  display: flex;
  align-items: center;
  gap: 10rpx;
  font-size: 23rpx;
  color: rgba(255, 255, 255, 0.76);
}
.activity-card__rule {
  height: 1rpx;
  margin: 22rpx 0 20rpx;
  background: linear-gradient(90deg, rgba(240, 207, 148, 0.72), rgba(240, 207, 148, 0.04));
}

/* ---------- 底部：号源 + 按钮 ---------- */
.activity-card__bottom {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20rpx;
}
.stats {
  min-width: 0;
}
.stats__line {
  display: flex;
  align-items: baseline;
  gap: 8rpx;
}
.stats__num {
  font-size: 48rpx;
  font-weight: 700;
  line-height: 1;
  color: #fffaf0;
  font-family: 'DIN Alternate', 'Helvetica Neue', sans-serif;
}
.activity-card--tight .stats__num { color: #ffc98a; }
.activity-card--soldout .stats__num { color: rgba(255, 255, 255, 0.62); }
.stats__total {
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.66);
}
.stats__pct {
  font-size: 20rpx;
  color: #8ce0d2;
  margin-left: 6rpx;
}
.stats__deadline {
  display: block;
  margin-top: 8rpx;
  font-size: 21rpx;
  color: rgba(255, 255, 255, 0.56);
}

.activity-card__btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10rpx;
  flex-shrink: 0;
  padding: 18rpx 34rpx;
  border-radius: 48rpx;
  font-size: 27rpx;
  font-weight: 600;
  letter-spacing: 1rpx;
  @include art-gold-button();
}
.activity-card__btn--disabled {
  background: rgba(255, 255, 255, 0.16);
  color: rgba(255, 255, 255, 0.72);
  box-shadow: none;
}

/* ---------- 底部号源进度 ---------- */
.activity-card__progress {
  position: absolute;
  z-index: 3;
  left: 0;
  right: 0;
  bottom: 0;
  height: 6rpx;
  background: rgba(255, 255, 255, 0.12);
}
.activity-card__progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #f0cf94 0%, #8ce0d2 100%);
  transition: width 0.6s cubic-bezier(0.16, 1, 0.3, 1);
}
.activity-card--soldout .activity-card__progress-fill {
  background: rgba(255, 255, 255, 0.4);
}
</style>
