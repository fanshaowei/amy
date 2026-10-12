<script setup lang="ts">
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import type { ReserveRecord } from '@/types'
import { RESERVE_STATUS } from '@/types'
import { getRecordDetail } from '@/api/reserve'
import QrcodeCard from '@/components/QrcodeCard.vue'
import { hmsToHm } from '@/utils'

const record = ref<ReserveRecord | null>(null)

/** 预约状态（后端：0待核销 1已完成 2已过期 3已取消） */
const statusInfo = computed(() => RESERVE_STATUS[record.value?.reservationStatus ?? 2])

/** 预约人信息（本人 + 随行人员） */
const people = computed(() => {
  if (!record.value) return []
  return [
    {
      name: record.value.name,
      idType: record.value.idType,
      idNum: record.value.idNum,
      phone: record.value.phone,
    },
    ...record.value.compUsers,
  ]
})

/** 入场日期 */
const entryDate = computed(() => (record.value?.reservationTime || '').slice(0, 10))
/** 入场时间 */
const entryTime = computed(() => hmsToHm((record.value?.reservationTime || '').slice(11)))

async function load(orderNum: string) {
  if (!orderNum) {
    uni.showToast({ title: '缺少预约单号', icon: 'none' })
    return
  }
  try {
    record.value = await getRecordDetail(orderNum)
  } catch (e: any) {
    uni.showToast({ title: e.message || '加载失败', icon: 'none' })
  }
}

onLoad((query) => {
  load(query?.orderNum || '')
})
</script>

<template>
  <view v-if="record" class="result">
    <view class="result__card">
      <view class="result__status">
        <view class="result__status-big" :class="`text--${statusInfo.type}`">
          {{ statusInfo.text }}
        </view>
        <view class="result__status-meta">单号：{{ record.reservationNum }}</view>
      </view>

      <view class="result__qr">
        <QrcodeCard :record="record" />
      </view>

      <view class="result__detail">
        <view class="row">
          <text class="k">活动名称</text>
          <text class="v">{{ record.projectName }}</text>
        </view>
        <view class="row">
          <text class="k">预约人数</text>
          <text class="v">{{ record.guestsNum }}人</text>
        </view>
        <view class="row">
          <text class="k">入场日期</text>
          <text class="v">{{ entryDate }}</text>
        </view>
        <view class="row">
          <text class="k">入场时间</text>
          <text class="v">{{ entryTime }}</text>
        </view>
        <view v-for="(p, i) in people" :key="i" class="row">
          <text class="k">{{ i === 0 ? '预约人' : '随行人' }}</text>
          <text class="v">{{ p.name }} · {{ p.idType }} · {{ p.phone || '-' }}</text>
        </view>
      </view>
    </view>
  </view>
</template>

<style scoped lang="scss">
.result {
  min-height: 100vh;
  padding: 32rpx 24rpx;
}
.result__card {
  background: #fff;
  border-radius: 40rpx;
  overflow: hidden;
  position: relative;
  box-shadow: 0 8rpx 40rpx rgba(31, 35, 41, 0.06);
}
.result__card::before,
.result__card::after {
  content: '';
  position: absolute;
  width: 48rpx;
  height: 48rpx;
  background: #f4f6fa;
  border-radius: 50%;
  top: 220rpx;
  z-index: 2;
}
.result__card::before {
  left: -24rpx;
}
.result__card::after {
  right: -24rpx;
}
.result__status {
  padding: 48rpx 32rpx 32rpx;
  text-align: center;
}
.result__status-big {
  font-size: 44rpx;
  font-weight: 700;
}
.text--expired {
  color: #86909c;
}
.text--done {
  color: #00b96b;
}
.text--pending {
  color: #ff9500;
}
.text--cancel {
  color: #f53f3f;
}
.result__status-meta {
  font-size: 24rpx;
  color: #86909c;
  margin-top: 12rpx;
}
.result__qr {
  padding: 16rpx 32rpx 32rpx;
  position: relative;
}
.result__qr::before {
  content: '';
  position: absolute;
  top: 0;
  left: 32rpx;
  right: 32rpx;
  border-top: 2rpx dashed #e5e6eb;
}
.result__detail {
  padding: 16rpx 32rpx 40rpx;
}
.row {
  display: flex;
  justify-content: space-between;
  padding: 20rpx 0;
  font-size: 28rpx;
  border-bottom: 1px solid #f2f3f5;
}
.row:last-child {
  border-bottom: 0;
}
.row .k {
  color: #4e5969;
}
.row .v {
  color: #1f2329;
  font-weight: 500;
}
</style>
