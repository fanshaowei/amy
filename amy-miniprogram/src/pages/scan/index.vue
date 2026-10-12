<script setup lang="ts">
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import type { ReserveRecord } from '@/types'
import { RESERVE_STATUS } from '@/types'
import { queryOrder, confirm } from '@/api/scan'
import { useUserStore } from '@/stores/user'
import { hmsToHm } from '@/utils'

const userStore = useUserStore()

const view = ref<'scan' | 'result'>('scan')
const record = ref<ReserveRecord | null>(null)
const confirming = ref(false)

/** 预约状态（后端：0待核销 1已完成 2已过期 3已取消） */
const statusInfo = computed(() => RESERVE_STATUS[record.value?.reservationStatus ?? 2])
/** 是否已完成核销 */
const verified = computed(() => record.value?.reservationStatus === 1)

/** 入场日期 */
const entryDate = computed(() => (record.value?.reservationTime || '').slice(0, 10))
/** 入场时间 */
const entryTime = computed(() => hmsToHm((record.value?.reservationTime || '').slice(11)))

function doScan() {
  uni.scanCode({
    scanType: ['qrCode'],
    success: async (res) => {
      await handleOrderCode(res.result)
    },
    fail: () => {
      // 取消扫码
    },
  })
}

/** 按预约单号查询（二维码内容即预约单号） */
async function handleOrderCode(orderNum: string) {
  const num = (orderNum || '').trim()
  if (!num) return
  uni.showLoading({ title: '查询中' })
  try {
    record.value = await queryOrder(num)
    view.value = 'result'
  } catch (e: any) {
    uni.showToast({ title: e.message || '查询失败', icon: 'none' })
  } finally {
    uni.hideLoading()
  }
}

async function onConfirm() {
  if (!record.value) return
  confirming.value = true
  try {
    await confirm(record.value.reservationNum)
    uni.showToast({ title: '核销成功', icon: 'success' })
    record.value = await queryOrder(record.value.reservationNum)
  } catch (e: any) {
    uni.showToast({ title: e.message || '核销失败', icon: 'none' })
  } finally {
    confirming.value = false
  }
}

function rescan() {
  view.value = 'scan'
  record.value = null
}

onShow(() => {
  // 核销功能需登录（后端按 token 鉴权并校验核销员身份）
  if (!userStore.loggedIn) {
    uni.navigateTo({ url: '/pages/login/index' })
    return
  }
  // 回到扫码页时重置
  if (view.value === 'result') view.value = 'scan'
})
</script>

<template>
  <view class="scan">
    <!-- 扫码界面 -->
    <view v-if="view === 'scan'" class="scan__view">
      <view class="scan__box">
        <view class="scan__line" />
      </view>
      <view class="scan__tip">将二维码放入框内，自动扫描核销</view>
      <view class="scan__actions">
        <view class="btn-primary" @tap="doScan">手动扫码</view>
      </view>
    </view>

    <!-- 扫码结果 -->
    <view v-else-if="record" class="scan__result">
      <view class="result-card">
        <view class="result-card__status">
          <view class="big" :class="`text--${statusInfo.type}`">{{ statusInfo.text }}</view>
          <view class="meta">单号：{{ record.reservationNum }}</view>
        </view>
        <view class="result-card__detail">
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
          <view class="row">
            <text class="k">预约人姓名</text>
            <text class="v">{{ record.name }}</text>
          </view>
          <view class="row">
            <text class="k">预约人证件</text>
            <text class="v">{{ record.idType }}</text>
          </view>
          <view class="row">
            <text class="k">预约人手机号</text>
            <text class="v">{{ record.phone }}</text>
          </view>
          <view v-if="record.compUsers.length" class="row">
            <text class="k">随行人员</text>
            <text class="v">{{ record.compUsers.map((p) => p.name).join('、') }}</text>
          </view>
        </view>
      </view>

      <view class="confirm-actions">
        <view v-if="verified" class="btn-done">已核销</view>
        <view v-else class="btn-confirm" :class="{ disabled: confirming }" @tap="onConfirm">
          {{ confirming ? '核销中...' : '确认核销' }}
        </view>
        <view class="btn-rescan" @tap="rescan">重新扫码</view>
      </view>
    </view>
  </view>
</template>

<style scoped lang="scss">
.scan {
  min-height: 100vh;
}
/* 扫码界面 */
.scan__view {
  padding: 64rpx 48rpx;
}
.scan__box {
  width: 100%;
  max-width: 560rpx;
  aspect-ratio: 1;
  margin: 0 auto;
  position: relative;
  border: 4rpx solid #3b7dff;
  border-radius: 32rpx;
  background: #1f2329;
  overflow: hidden;
}
.scan__box::before,
.scan__box::after {
  content: '';
  position: absolute;
  width: 64rpx;
  height: 64rpx;
  border: 8rpx solid #fff;
}
.scan__box::before {
  top: 0;
  left: 0;
  border-right: 0;
  border-bottom: 0;
  border-top-left-radius: 24rpx;
}
.scan__box::after {
  bottom: 0;
  right: 0;
  border-left: 0;
  border-top: 0;
  border-bottom-right-radius: 24rpx;
}
.scan__line {
  position: absolute;
  left: 24rpx;
  right: 24rpx;
  top: 24rpx;
  height: 4rpx;
  background: linear-gradient(90deg, transparent, #3b7dff, transparent);
  box-shadow: 0 0 16rpx #3b7dff;
  animation: scan 1.6s linear infinite;
}
@keyframes scan {
  0% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(480rpx);
  }
  100% {
    transform: translateY(0);
  }
}
.scan__tip {
  text-align: center;
  color: #86909c;
  font-size: 26rpx;
  margin-top: 32rpx;
}
.scan__actions {
  margin-top: 64rpx;
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}
.btn-primary {
  background: linear-gradient(90deg, #3b7dff, #5b95ff);
  color: #fff;
  padding: 26rpx;
  border-radius: 56rpx;
  text-align: center;
  font-size: 30rpx;
  font-weight: 500;
  box-shadow: 0 8rpx 32rpx rgba(59, 125, 255, 0.35);
}
.btn-outline {
  border: 1px solid #3b7dff;
  color: #3b7dff;
  padding: 26rpx;
  border-radius: 56rpx;
  text-align: center;
  font-size: 30rpx;
}
/* 结果界面 */
.scan__result {
  padding: 24rpx;
}
.result-card {
  background: #fff;
  border-radius: 40rpx;
  overflow: hidden;
  box-shadow: 0 8rpx 40rpx rgba(31, 35, 41, 0.06);
}
.result-card__status {
  padding: 48rpx 32rpx 32rpx;
  text-align: center;
}
.big {
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
.meta {
  font-size: 24rpx;
  color: #86909c;
  margin-top: 12rpx;
}
.result-card__detail {
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
.confirm-actions {
  margin-top: 40rpx;
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}
.btn-confirm {
  background: linear-gradient(90deg, #3b7dff, #5b95ff);
  color: #fff;
  padding: 26rpx;
  border-radius: 56rpx;
  text-align: center;
  font-size: 30rpx;
  font-weight: 500;
  box-shadow: 0 8rpx 32rpx rgba(59, 125, 255, 0.35);
}
.btn-confirm.disabled {
  opacity: 0.6;
}
.btn-done {
  background: #f2f3f5;
  color: #c9cdd4;
  padding: 26rpx;
  border-radius: 56rpx;
  text-align: center;
  font-size: 30rpx;
}
.btn-rescan {
  text-align: center;
  color: #86909c;
  font-size: 28rpx;
  padding: 20rpx;
}
</style>
