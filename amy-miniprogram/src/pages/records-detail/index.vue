<script setup lang="ts">
import PageHero from '@/components/PageHero.vue'
import { ref, computed } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import type { ReserveRecord } from '@/types'
import { RESERVE_STATUS } from '@/types'
import { getRecordDetail, cancelReserve } from '@/api/reserve'
import QrcodeCard from '@/components/QrcodeCard.vue'
import { hmsToHm } from '@/utils'
import { showError } from '@/utils/request'
import { requireLogin } from '@/utils/authGuard'

const record = ref<ReserveRecord | null>(null)
const orderNum = ref('')
/** 首屏只加载一次，避免 onShow 重复请求 */
let initialLoaded = false

/** 预约状态（后端：-1预约失败 0待核销 1已核销 2已过期 3已取消 4预约中） */
const statusInfo = computed(() => RESERVE_STATUS[record.value?.reservationStatus ?? 2])

/** status_msg 与状态中文文案相同（或为空）时不重复显示 */
const showStatusMsg = computed(() => {
  const msg = (record.value?.statusMsg || '').trim()
  if (!msg) return false
  return msg !== (statusInfo.value?.text || '')
})

/** 预约人信息（本人 + 随行人员），统一成简表结构便于逐行排版 */
const people = computed(() => {
  const r = record.value
  if (!r) return [] as { name: string; idNum: string; phone?: string }[]
  return [
    { name: r.name, idNum: r.idNum, phone: r.phone },
    // 后端 compUsers 可能缺省，兜底避免展开 undefined 报错
    ...(r.compUsers || []).map((p) => ({ name: p.name, idNum: p.idNum, phone: p.phone })),
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
  } catch (e) {
    showError(e, '加载失败')
  }
}

/** 是否可取消：仅「待核销」状态 */
const canCancel = computed(() => record.value?.reservationStatus === 0)
const cancelling = ref(false)

/** 取消预约：二次确认后调用后端取消接口，成功后刷新详情 */
function onCancel() {
  if (!record.value || cancelling.value) return
  uni.showModal({
    title: '取消预约',
    content: '确定要取消该预约吗？取消后不可恢复。',
    confirmText: '取消预约',
    confirmColor: '#f53f3f',
    success: async (res) => {
      if (!res.confirm) return
      cancelling.value = true
      try {
        await cancelReserve(record.value!.reservationNum)
        uni.showToast({ title: '已取消预约', icon: 'success' })
        load(record.value!.reservationNum)
      } catch (e) {
        showError(e, '取消失败')
      } finally {
        cancelling.value = false
      }
    },
  })
}

onLoad((query) => {
  orderNum.value = query?.orderNum || ''
})

onShow(() => {
  // 未登录 → 授权登录页；从登录页放弃授权返回 → 统一回首页
  if (!requireLogin()) return
  if (initialLoaded) return
  initialLoaded = true
  load(orderNum.value)
})
</script>

<template>
  <view v-if="record" class="result">
    <PageHero title="预约详情" eyebrow="RESERVATION DETAIL" subtitle="入场核验码与预约信息" compact flush />
    <view class="result__card">
      <view class="result__status">
        <view class="result__status-big" :class="`text--${statusInfo.type}`">
          {{ statusInfo.text }}
        </view>
        <view class="result__status-meta">单号：{{ record.reservationNum }}</view>
        <view class="result__status-msg" v-if="showStatusMsg&&record.statusMsg!='已完成' && record.statusMsg!='已取消' && record.statusMsg!='已过期'">{{ record.statusMsg }}</view>
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
        <!-- 每人独立成块：姓名+角色一行，证件号/手机号各一行，避免长号码被挤断 -->
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

      <!-- 取消预约：仅「待核销」状态展示 -->
      <view v-if="canCancel" class="result__actions">
        <view class="cancel-btn" :class="{ disabled: cancelling }" @tap="onCancel">
          {{ cancelling ? '取消中...' : '取消预约' }}
        </view>
      </view>
    </view>
  </view>
</template>

<style scoped lang="scss">
.result {
  min-height: 100vh;
  padding: 0 24rpx $art-page-bottom;
}
.result__card {
  @include art-card(0.66, 22px, 40rpx);
  
  margin-top: $art-gap;overflow: hidden;
  position: relative;
}
.result__card::before,
.result__card::after {
  content: '';
  position: absolute;
  width: 48rpx;
  height: 48rpx;
  background: rgba(240, 207, 148, 0.16);
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
.result__status-msg {
  font-size: 24rpx;
  color: #ff9c9c;
  margin-top: 8rpx;
}
.result__status-meta {
  font-size: 24rpx;
  color: $art-text-dim;
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
  border-top: 2rpx dashed $art-divider;
}
.result__detail {
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
  flex-shrink: 0;
  margin-right: 32rpx;
}
.row .v {
  color: $art-text-strong;
  font-weight: 500;
  flex: 1;
  text-align: right;
  word-break: break-all;
}
/* 预约人 / 随行人：长证件号与手机号各占一行，不再挤在同一行被折断 */
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
.result__actions {
  padding: 8rpx 32rpx 40rpx;
}
.cancel-btn {
  height: 80rpx;
  line-height: 80rpx;
  text-align: center;
  font-size: 28rpx;
  color: #ff9c9c;
  @include art-chip();
  border: 1rpx solid rgba(245, 63, 63, 0.5);
  border-radius: 48rpx;
}
.cancel-btn.disabled {
  opacity: 0.6;
}
</style>
