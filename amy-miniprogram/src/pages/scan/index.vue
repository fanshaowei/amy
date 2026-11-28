<script setup lang="ts">
import PageHero from '@/components/PageHero.vue'
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import type { ReserveRecord } from '@/types'
import { RESERVE_STATUS } from '@/types'
import { queryOrder, confirm } from '@/api/scan'
import { hmsToHm } from '@/utils'
import { showError } from '@/utils/request'
import { requireLogin } from '@/utils/authGuard'

const view = ref<'scan' | 'result'>('scan')
const record = ref<ReserveRecord | null>(null)
const confirming = ref(false)
/** 本次扫码解析出的核销码（旧格式二维码为空，核销时兜底用查询返回的） */
const scannedVerifyCode = ref('')
/** 原生扫码相机是否已调起：防止连点重复打开相机 */
const scanning = ref(false)
/** 扫码结果查询中：防止上一次查询结果被后一次覆盖 */
const querying = ref(false)

/** 预约状态（后端：-1预约失败 0待核销 1已核销 2已过期 3已取消 4预约中） */
const statusInfo = computed(() => RESERVE_STATUS[record.value?.reservationStatus ?? 2])
/** 是否已核销核销 */
const verified = computed(() => record.value?.reservationStatus === 1)

/** 入场日期 */
const entryDate = computed(() => (record.value?.reservationTime || '').slice(0, 10))
/** 入场时间 */
const entryTime = computed(() => hmsToHm((record.value?.reservationTime || '').slice(11)))

/** 预约人信息（本人 + 随行人员），与预约详情页同为逐人简表结构 */
const people = computed(() => {
  const r = record.value
  if (!r) return [] as { name: string; idNum: string; phone?: string }[]
  return [
    { name: r.name, idNum: r.idNum, phone: r.phone },
    // 后端 compUsers 可能缺省，兜底避免展开 undefined 报错
    ...(r.compUsers || []).map((p) => ({ name: p.name, idNum: p.idNum, phone: p.phone })),
  ]
})

/**
 * 调起扫码。
 *
 * ⚠️ 真机（尤其 iPhone）会打开微信原生相机页：本页 onHide，相机关闭回来又 onShow，
 * 且 onShow 的派发时机不保证早于扫码结果查询完成，部分机型还会连续触发两次。
 * 因此本页【不能】在 onShow 里重置 view —— 否则刚查出来的结果页会被这次 onShow
 * 立刻打回扫码页，表现就是「结果一闪而过」。开发者工具是模拟扫码、不打开原生相机页，
 * 所以只在真机复现。结果页仅由「重新扫码」按钮主动重置。
 */
function doScan() {
  if (!requireLogin()) return
  if (scanning.value || querying.value) return
  scanning.value = true
  uni.scanCode({
    scanType: ['qrCode'],
    success: (res) => {
      scanning.value = false
      const parsed = parseQrContent(res.result)
      scannedVerifyCode.value = parsed.verifyCode
      void handleOrderCode(parsed.orderNum)
    },
    fail: () => {
      scanning.value = false
      // 用户取消扫码：保持扫码页，不提示
    },
    complete: () => {
      // 兜底复位：个别基础库可能不派发 complete
      scanning.value = false
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
  if (!num || querying.value) return
  querying.value = true
  uni.showLoading({ title: '查询中' })
  try {
    const detail = await queryOrder(num)
    record.value = detail
    view.value = 'result'
  } catch (e) {
    // 查询失败保持在扫码页，提示后可重新扫码
    showError(e, '查询失败')
  } finally {
    querying.value = false
    uni.hideLoading()
  }
}

async function onConfirm() {
  if (!record.value || confirming.value) return
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

/**
 * 登录守卫放 onShow：未登录跳授权登录页，从登录页放弃授权返回时统一回首页。
 *
 * ⚠️ 这里只做登录态判断，【绝不】重置 view 或 record：
 * 真机 scanCode 关闭相机会额外触发 onShow，若像旧代码那样把 result 改回 scan，
 * 刚查出来的结果页就会一闪而过（见 doScan 注释）。守卫在已登录时是无副作用的纯判断。
 */
onShow(() => {
  requireLogin()
})
</script>

<template>
  <view class="scan">
    <PageHero title="核销" eyebrow="VERIFICATION" subtitle="扫描入场二维码完成核销" compact />
    <!-- 扫码界面 -->
    <view v-if="view === 'scan'" class="scan__view">
      <view class="scan__guide">
        <view class="scan__guide-title">操作说明</view>
        <view class="scan__step">
          <view class="scan__step-no">1</view>
          <view class="scan__step-text">请客户出示预约记录二维码，或指导客户在小程序中点击「预约记录」查看二维码</view>
        </view>
        <view class="scan__step">
          <view class="scan__step-no">2</view>
          <view class="scan__step-text">点击下方的「手动扫码」按钮</view>
        </view>
        <view class="scan__step">
          <view class="scan__step-no">3</view>
          <view class="scan__step-text">将镜头对准客户的二维码进行扫描</view>
        </view>
        <view class="scan__step">
          <view class="scan__step-no">4</view>
          <view class="scan__step-text">扫描正确会弹出订单信息，确认无误后点击「确认核销」即可完成核销操作</view>
        </view>
      </view>
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
          <!-- 每人独立成块：姓名+角色一行，证件号/手机号各一行，与预约详情页一致 -->
          <view v-for="(p, i) in people" :key="i" class="person">
            <view class="person__hd">
              <text class="person__name">{{ p.name || '-' }}</text>
              <text class="person__tag" :class="{ 'person__tag--main': i === 0 }">
                {{ i === 0 ? '预约人' : '随行人 ' + i }}
              </text>
            </view>
            <view class="person__line">
              <text class="person__k">证件号码</text>
              <text class="person__v">{{ p.idNum || '-' }}</text>
            </view>
            <view class="person__line">
              <text class="person__k">手机号码</text>
              <text class="person__v">{{ p.phone || '-' }}</text>
            </view>
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
.scan__guide {
  @include art-card(0.66, 22px, 40rpx);
  padding: 40rpx 32rpx;
}
.scan__guide-title {
  font-size: 30rpx;
  font-weight: 600;
  color: $art-text-strong;
  margin-bottom: 24rpx;
}
.scan__step {
  display: flex;
  align-items: flex-start;
  gap: 20rpx;
  padding: 16rpx 0;
}
.scan__step-no {
  flex-shrink: 0;
  width: 44rpx;
  height: 44rpx;
  border-radius: 50%;
  background: rgba(240, 207, 148, 0.18);
  color: $art-accent;
  font-size: 26rpx;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
}
.scan__step-text {
  flex: 1;
  font-size: 27rpx;
  line-height: 1.7;
  color: $art-text;
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
/* 预约人 / 随行人：长证件号与手机号各占一行，不再挤在同一行被折断（与预约详情页一致） */
.person {
  padding: 20rpx 0 16rpx;
  border-bottom: 1px solid $art-divider;
}
.person:last-child {
  border-bottom: 0;
}
.person__hd {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-bottom: 8rpx;
}
.person__name {
  color: $art-text-strong;
  font-size: 30rpx;
  font-weight: 600;
}
.person__tag {
  @include art-chip();
  padding: 4rpx 16rpx;
  border-radius: 24rpx;
  font-size: 22rpx;
  color: $art-text;
}
.person__tag--main {
  background-color: $art-accent;
  color: $art-on-gold;
  box-shadow: none;
  font-weight: 600;
}
.person__line {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  padding: 6rpx 0;
  font-size: 26rpx;
}
.person__k {
  color: $art-text-dim;
  flex-shrink: 0;
  margin-right: 32rpx;
}
.person__v {
  flex: 1;
  min-width: 0;
  color: $art-text-strong;
  font-weight: 500;
  text-align: right;
  word-break: break-all;
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
