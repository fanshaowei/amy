<script setup lang="ts">
import PageHero from '@/components/PageHero.vue'
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import type { ReserveRecord } from '@/types'
import { RESERVE_STATUS } from '@/types'
import { queryOrder, confirm } from '@/api/scan'
import { useUserStore } from '@/stores/user'
import { hmsToHm } from '@/utils'
import { showError } from '@/utils/request'

const userStore = useUserStore()

const view = ref<'scan' | 'result'>('scan')
const record = ref<ReserveRecord | null>(null)
const confirming = ref(false)
/** 本次扫码解析出的核销码（旧格式二维码为空，核销时兜底用查询返回的） */
const scannedVerifyCode = ref('')

/** 预约状态（后端：-1预约失败 0待核销 1已完成 2已过期 3已取消 4预约中） */
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
      const parsed = parseQrContent(res.result)
      scannedVerifyCode.value = parsed.verifyCode
      await handleOrderCode(parsed.orderNum)
    },
    fail: () => {
      // 取消扫码
    },
  })
}

/**
 * 解析二维码内容：
 *  - 新格式：JSON {"orderNum":"...","verifyCode":"..."}
 *  - 兼容旧格式：内容即预约单号（verifyCode 从查询结果取）
 */
function parseQrContent(raw: string): { orderNum: string; verifyCode: string } {
  const content = (raw || '').trim()
  try {
    const obj = JSON.parse(content)
    if (obj && typeof obj.orderNum === 'string' && obj.orderNum) {
      return { orderNum: obj.orderNum, verifyCode: typeof obj.verifyCode === 'string' ? obj.verifyCode : '' }
    }
  } catch (e) {
    // 非 JSON：按旧格式处理
  }
  return { orderNum: content, verifyCode: '' }
}

/** 扫码后按预约单号查询（二维码内容携带预约单号） */
async function handleOrderCode(orderNum: string) {
  const num = (orderNum || '').trim()
  if (!num) return
  uni.showLoading({ title: '查询中' })
  try {
    record.value = await queryOrder(num)
    view.value = 'result'
  } catch (e) {
    showError(e, '查询失败')
  } finally {
    uni.hideLoading()
  }
}

async function onConfirm() {
  if (!record.value) return
  confirming.value = true
  try {
    // 优先用扫码得到的核销码，兜底用查询返回的
    const code = scannedVerifyCode.value || record.value.verifyCode
    await confirm(record.value.reservationNum, code)
    uni.showToast({ title: '核销成功', icon: 'success' })
    record.value = await queryOrder(record.value.reservationNum)
  } catch (e) {
    showError(e, '核销失败')
  } finally {
    confirming.value = false
  }
}

function rescan() {
  view.value = 'scan'
  record.value = null
  scannedVerifyCode.value = ''
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
    <PageHero title="核销" eyebrow="VERIFICATION" subtitle="扫描入场二维码完成核销" compact />
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
  padding-bottom: $art-page-bottom;
}
/* 扫码界面 */
.scan__view {
  padding: $art-gap 24rpx 0;
}
.scan__box {
  width: 100%;
  max-width: 560rpx;
  aspect-ratio: 1;
  margin: 0 auto;
  position: relative;
  border: 4rpx solid $art-accent;
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
  border: 8rpx solid $art-accent;
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
  background: linear-gradient(90deg, transparent, $art-accent, transparent);
  box-shadow: 0 0 16rpx rgba(240, 207, 148, 0.7);
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
  color: $uni-text-color-grey;
  font-size: 26rpx;
  margin-top: 32rpx;
}
.scan__actions {
  margin-top: $art-gap-lg;
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}
.btn-primary {
  @include art-gold-button();
  
  padding: 26rpx;
  border-radius: 56rpx;
  text-align: center;
  font-size: 30rpx;
  font-weight: 500;
}
.btn-outline {
  @include art-chip();
  border: 1rpx solid rgba(240, 207, 148, 0.5);
  color: $art-accent;
  padding: 26rpx;
  border-radius: 56rpx;
  text-align: center;
  font-size: 30rpx;
}
/* 结果界面 */
.scan__result {
  padding: $art-gap 24rpx 0;
}
.result-card {
  @include art-card(0.66, 22px, 40rpx);
  overflow: hidden;
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
  color: $art-text-dim;
}
.text--done {
  color: #7fe0a8;
}
.text--pending {
  color: #ffc47a;
}
.text--cancel {
  color: #ff9c9c;
}
.text--fail {
  color: #ff9c9c;
}
.text--booking {
  color: $art-accent;
}
.meta {
  font-size: 24rpx;
  color: $art-text-dim;
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
  border-bottom: 1px solid $art-divider;
}
.row:last-child {
  border-bottom: 0;
}
.row .k {
  color: $art-text;
}
.row .v {
  color: $art-text-strong;
  font-weight: 500;
}
.confirm-actions {
  margin-top: $art-gap-lg;
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}
.btn-confirm {
  @include art-gold-button();
  
  padding: 26rpx;
  border-radius: 56rpx;
  text-align: center;
  font-size: 30rpx;
  font-weight: 500;
}
.btn-confirm.disabled {
  opacity: 0.6;
}
.btn-done {
  @include art-chip();
  border-radius: 56rpx;
  color: $art-text-dim;
  padding: 26rpx;
  border-radius: 56rpx;
  text-align: center;
  font-size: 30rpx;
}
.btn-rescan {
  text-align: center;
  color: $uni-text-color-grey;
  font-size: 28rpx;
  padding: 20rpx;
}
</style>
