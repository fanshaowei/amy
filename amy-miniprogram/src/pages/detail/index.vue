<script setup lang="ts">
import { ref, computed } from 'vue'
import { onLoad, onUnload } from '@dcloudio/uni-app'
import type { Activity, TimeSlot } from '@/types'
import { cardTypeTitle } from '@/types'
import { getActivityDetail, getSlots } from '@/api/activity'
import { useUserStore } from '@/stores/user'
import { useReserveStore } from '@/stores/reserve'
import { hmsToHm } from '@/utils'
import CountdownBar from '@/components/CountdownBar.vue'
import NoticeModal from '@/components/NoticeModal.vue'
import DatePickerModal from '@/components/DatePickerModal.vue'
import CardPicker from '@/components/CardPicker.vue'

/** 随行人表单（不含本人） */
interface CompanionForm {
  name: string
  /** 证件类型 1身份证 2护照 3港澳通行证 4台湾通行证 5军官证 */
  cardType: number
  idNum: string
}

const userStore = useUserStore()
const reserveStore = useReserveStore()

const activityId = ref(0)
const activity = ref<Activity | null>(null)

/* 弹窗状态 */
const noticeShow = ref(false)
const datePickerShow = ref(false)
const cardPickerShow = ref(false)
const cardPickerTarget = ref<'booker' | number>('booker')

/* 时间选择 */
const dates = ref<string[]>([])
const slots = ref<TimeSlot[]>([])
const selectedDate = ref('')
const selectedTime = ref('')

/* 主预约人表单 */
const bookerName = ref('')
const bookerTel = ref('')
const bookerCardType = ref(1)
const bookerIdCard = ref('')

/* 随行人 */
const companions = ref<CompanionForm[]>([])

const submitting = ref(false)

/** 可携带随行人数（后端 Projects.travelerCount） */
const maxCompanions = computed(() => activity.value?.travelerCount || 0)

/** 证件选择器当前选中的值 */
const cardPickerSelected = computed(() => {
  if (cardPickerTarget.value === 'booker') return bookerCardType.value
  const idx = cardPickerTarget.value as number
  return companions.value[idx]?.cardType || 1
})

const dateLabel = computed(() =>
  selectedDate.value && selectedTime.value
    ? `${selectedDate.value} ${hmsToHm(selectedTime.value)}`
    : '点击选择预约日期'
)

const dateLabelClass = computed(() => ({
  placeholder: !selectedDate.value || !selectedTime.value,
}))

async function loadActivity(id: number) {
  try {
    activity.value = await getActivityDetail(id)
    // 可预约日期由后端「项目预约信息」接口下发
    dates.value = activity.value.reservationDates || []
  } catch (e) {
    uni.showToast({ title: '加载失败', icon: 'none' })
  }
}

async function loadSlots() {
  if (!selectedDate.value) return
  try {
    slots.value = await getSlots(activityId.value, selectedDate.value)
  } catch (e) {
    uni.showToast({ title: '时段加载失败', icon: 'none' })
  }
}

function openNotice() {
  noticeShow.value = true
}

function onNoticeConfirm() {
  noticeShow.value = false
  datePickerShow.value = true
}

function onPickDate(date: string) {
  selectedDate.value = date
  selectedTime.value = ''
  loadSlots()
}

function onPickTime(time: string) {
  selectedTime.value = time
}

function onDateConfirm() {
  if (!selectedDate.value) {
    uni.showToast({ title: '请选择日期', icon: 'none' })
    return
  }
  if (!selectedTime.value) {
    uni.showToast({ title: '请选择时段', icon: 'none' })
    return
  }
  datePickerShow.value = false
}

function openCardPicker(target: 'booker' | number) {
  cardPickerTarget.value = target
  cardPickerShow.value = true
}

function onCardSelect(id: number) {
  if (cardPickerTarget.value === 'booker') {
    bookerCardType.value = id
  } else {
    const idx = cardPickerTarget.value as number
    if (companions.value[idx]) {
      companions.value[idx].cardType = id
    }
  }
  cardPickerShow.value = false
}

function addCompanion() {
  if (companions.value.length >= maxCompanions.value) {
    uni.showToast({ title: `最多${maxCompanions.value}位随行人`, icon: 'none' })
    return
  }
  companions.value.push({
    name: '',
    cardType: 1,
    idNum: '',
  })
}

function removeCompanion(idx: number) {
  companions.value.splice(idx, 1)
}

function onTimeout() {
  uni.showToast({ title: '预约时间已到', icon: 'none' })
  setTimeout(() => {
    uni.switchTab({ url: '/pages/home/index' })
  }, 800)
}

async function onSubmit() {
  if (!selectedDate.value || !selectedTime.value) {
    uni.showToast({ title: '请选择预约日期', icon: 'none' })
    return
  }
  if (!bookerName.value.trim()) {
    uni.showToast({ title: '请输入预约人姓名', icon: 'none' })
    return
  }
  if (!/^1\d{10}$/.test(bookerTel.value)) {
    uni.showToast({ title: '请输入正确的手机号', icon: 'none' })
    return
  }
  if (!bookerIdCard.value.trim()) {
    uni.showToast({ title: '请输入证件号', icon: 'none' })
    return
  }
  for (let i = 0; i < companions.value.length; i++) {
    const c = companions.value[i]
    if (!c.name.trim()) {
      uni.showToast({ title: `请输入随行人${i + 1}姓名`, icon: 'none' })
      return
    }
    if (!c.idNum.trim()) {
      uni.showToast({ title: `请输入随行人${i + 1}证件号`, icon: 'none' })
      return
    }
  }

  submitting.value = true
  try {
    await reserveStore.create({
      projectId: activityId.value,
      reservationUserId: userStore.uid,
      date: selectedDate.value,
      time: selectedTime.value,
      name: bookerName.value.trim(),
      phone: bookerTel.value,
      cardType: bookerCardType.value,
      idNum: bookerIdCard.value.trim(),
      // 随行人按后端 UserIdentity 结构序列化，随行人默认使用预约人手机号
      companions: companions.value.map((c) => ({
        name: c.name.trim(),
        idType: c.cardType,
        idNum: c.idNum.trim(),
        phone: bookerTel.value,
      })),
    })
    uni.showToast({ title: '预约成功', icon: 'success' })
    setTimeout(() => {
      uni.switchTab({ url: '/pages/records/index' })
    }, 800)
  } catch (e: any) {
    uni.showToast({ title: e.message || '提交失败', icon: 'none' })
  } finally {
    submitting.value = false
  }
}

onLoad((query) => {
  const id = Number(query?.id || 0)
  activityId.value = id

  // 未登录拦截
  if (!userStore.loggedIn) {
    uni.navigateTo({ url: `/pages/login/index?redirect=/pages/detail/index?id=${id}` })
    return
  }

  loadActivity(id)
  // 进入后延迟弹出须知
  setTimeout(openNotice, 300)
})

onUnload(() => {
  // 倒计时组件内部会自行清理
})
</script>

<template>
  <view class="detail">
    <CountdownBar :seconds="activity?.reservationStaySecond || 180" @timeout="onTimeout" />

    <view v-if="activity" class="detail__hero">
      <view class="detail__title">{{ activity.projectName }}</view>
      <view class="detail__desc">{{ activity.desc }}</view>
    </view>

    <view class="section">
      <view class="section__hd">时间选择</view>
      <view class="form-item" @tap="datePickerShow = true">
        <text class="form-item__lbl"><text class="req">*</text>预约日期</text>
        <text class="form-item__val" :class="dateLabelClass">{{ dateLabel }}</text>
        <text class="arrow">›</text>
      </view>
    </view>

    <view class="section">
      <view class="section__hd">基本信息</view>
      <view class="form-item">
        <text class="form-item__lbl"><text class="req">*</text>预约人姓名</text>
        <input class="form-item__input" v-model="bookerName" placeholder="请输入预约人姓名" />
      </view>
      <view class="form-item">
        <text class="form-item__lbl"><text class="req">*</text>预约人手机</text>
        <input class="form-item__input" v-model="bookerTel" type="number" maxlength="11" placeholder="请输入预约人手机号" />
      </view>
      <view class="form-item" @tap="openCardPicker('booker')">
        <text class="form-item__lbl"><text class="req">*</text>证件类型</text>
        <text class="form-item__val">{{ cardTypeTitle(bookerCardType) }}</text>
        <text class="arrow">›</text>
      </view>
      <view class="form-item">
        <text class="form-item__lbl"><text class="req">*</text>预约人证件号</text>
        <input class="form-item__input" v-model="bookerIdCard" placeholder="请输入预约人证件号" />
      </view>
    </view>

    <!-- 随行人 -->
    <view v-for="(c, idx) in companions" :key="idx" class="section">
      <view class="section__hd section__hd--row">
        <text>随行人{{ idx + 1 }}</text>
        <text class="del" @tap="removeCompanion(idx)">删除</text>
      </view>
      <view class="form-item">
        <text class="form-item__lbl"><text class="req">*</text>姓名</text>
        <input class="form-item__input" v-model="c.name" placeholder="请输入随行人姓名" />
      </view>
      <view class="form-item" @tap="openCardPicker(idx)">
        <text class="form-item__lbl"><text class="req">*</text>证件类型</text>
        <text class="form-item__val">{{ cardTypeTitle(c.cardType) }}</text>
        <text class="arrow">›</text>
      </view>
      <view class="form-item">
        <text class="form-item__lbl"><text class="req">*</text>证件号</text>
        <input class="form-item__input" v-model="c.idNum" placeholder="请输入随行人证件号" />
      </view>
    </view>

    <view v-if="companions.length < maxCompanions" class="add-btn" @tap="addCompanion">
      ⊕ 添加随行人 ({{ companions.length }}/{{ maxCompanions }})
    </view>

    <view class="submit-btn" :class="{ disabled: submitting }" @tap="onSubmit">
      {{ submitting ? '提交中...' : '提交' }}
    </view>

    <!-- 弹窗 -->
    <NoticeModal
      :show="noticeShow"
      :content="activity?.reservationNotes || ''"
      @confirm="onNoticeConfirm"
      @close="noticeShow = false"
    />
    <DatePickerModal
      :show="datePickerShow"
      :dates="dates"
      :slots="slots"
      :selected-date="selectedDate"
      :selected-time="selectedTime"
      :dwell-time="activity?.intervalMinutes"
      @pick-date="onPickDate"
      @pick-time="onPickTime"
      @confirm="onDateConfirm"
      @close="datePickerShow = false"
    />
    <CardPicker
      :show="cardPickerShow"
      :selected="cardPickerSelected"
      @select="onCardSelect"
      @close="cardPickerShow = false"
    />
  </view>
</template>

<style scoped lang="scss">
.detail {
  min-height: 100vh;
  padding-bottom: 60rpx;
}
.detail__hero {
  background: linear-gradient(180deg, #3b7dff, #5b95ff);
  padding: 32rpx;
  color: #fff;
}
.detail__title {
  font-size: 40rpx;
  font-weight: 600;
  margin-bottom: 16rpx;
}
.detail__desc {
  font-size: 26rpx;
  opacity: 0.9;
  line-height: 1.7;
}
.section {
  background: #fff;
  margin: 24rpx;
  border-radius: 24rpx;
  overflow: hidden;
}
.section__hd {
  padding: 28rpx 32rpx 20rpx;
  font-size: 30rpx;
  font-weight: 600;
  color: #1f2329;
}
.section__hd--row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.section__hd .del {
  font-size: 26rpx;
  color: #f53f3f;
  font-weight: 400;
}
.form-item {
  padding: 28rpx 32rpx;
  border-top: 1px solid #f2f3f5;
  display: flex;
  align-items: center;
  gap: 24rpx;
  min-height: 100rpx;
}
.form-item__lbl {
  font-size: 28rpx;
  color: #1f2329;
  flex-shrink: 0;
}
.req {
  color: #f53f3f;
  margin-right: 4rpx;
}
.form-item__val {
  flex: 1;
  text-align: right;
  font-size: 28rpx;
  color: #1f2329;
}
.form-item__val.placeholder {
  color: #c9cdd4;
}
.form-item__input {
  flex: 1;
  text-align: right;
  font-size: 28rpx;
  color: #1f2329;
}
.arrow {
  color: #c9cdd4;
  font-size: 28rpx;
}
.add-btn {
  margin: 24rpx;
  padding: 20rpx;
  border: 1px dashed #c9cdd4;
  border-radius: 48rpx;
  text-align: center;
  color: #3b7dff;
  font-size: 26rpx;
}
.submit-btn {
  margin: 32rpx 24rpx 40rpx;
  background: linear-gradient(90deg, #3b7dff, #5b95ff);
  color: #fff;
  padding: 28rpx;
  border-radius: 56rpx;
  text-align: center;
  font-size: 32rpx;
  font-weight: 500;
  box-shadow: 0 8rpx 32rpx rgba(59, 125, 255, 0.35);
}
.submit-btn.disabled {
  opacity: 0.6;
}
</style>
