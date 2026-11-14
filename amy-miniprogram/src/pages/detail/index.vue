<script setup lang="ts">
import { ref, computed } from 'vue'
import { onLoad, onUnload } from '@dcloudio/uni-app'
import type { Activity, TimeSlot, UserIdentity as UserIdentityRow } from '@/types'
import { cardTypeTitle } from '@/types'
import { getActivityDetail, getSlots } from '@/api/activity'
import { getFollowers, addFollower } from '@/api/user'
import type { UserIdentityPayload } from '@/api/user'
import { useUserStore } from '@/stores/user'
import { useReserveStore } from '@/stores/reserve'
import { hmsToHm, isValidIdCard, isValidPhone } from '@/utils'
import CountdownBar from '@/components/CountdownBar.vue'
import NoticeModal from '@/components/NoticeModal.vue'
import DatePickerModal from '@/components/DatePickerModal.vue'
import CardPicker from '@/components/CardPicker.vue'

const userStore = useUserStore()
const reserveStore = useReserveStore()

const activityId = ref(0)
const activity = ref<Activity | null>(null)

/* 弹窗状态 */
const noticeShow = ref(false)
const datePickerShow = ref(false)
const followerPickerShow = ref(false)

/* 时间选择 */
const dates = ref<string[]>([])
const slots = ref<TimeSlot[]>([])
const selectedDate = ref('')
const selectedTime = ref('')

/* 主预约人信息（由实名信息自动带出，不可编辑） */
const bookerName = ref('')
const bookerTel = ref('')
const bookerCardType = ref(1)
const bookerIdCard = ref('')

/* 随行人：全部已保存的随行人 + 本次带入的随行人 */
const allFollowers = ref<UserIdentityRow[]>([])
const companions = ref<UserIdentityRow[]>([])
/** 随行人选择弹窗中勾选的记录ID */
const checkedFollowerIds = ref<number[]>([])

const submitting = ref(false)

/** 可携带随行人数（后端 Projects.travelerCount） */
const maxCompanions = computed(() => activity.value?.travelerCount || 0)

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

/**
 * 拉取实名信息与随行人列表并自动带出：
 *  - 预约人信息来自 /user/followers/get 中 isFollower=false 的身份记录（/user/info 仅用于实名状态判断与兜底）
 *  - 随行人来自 /user/followers/get 中 isFollower=true 的记录，超过可携带人数时弹窗让用户勾选
 */
async function loadIdentity() {
  try {
    const user = await userStore.fetchProfile()
    if (!user?.isVerified) {
      // 未实名 → 先去实名认证，完成后回跳本页
      uni.redirectTo({
        url: `/pages/verify/index?redirect=/pages/detail/index?id=${activityId.value}`,
      })
      return
    }
    bookerName.value = user.name || ''
    bookerTel.value = user.phone || ''
    bookerIdCard.value = user.idNum || ''
    bookerCardType.value = user.idType || 1

    const list = await getFollowers()
    allFollowers.value = list.filter((item) => item.isFollower)

    // 预约人信息以 followers/get 返回的 isFollower=false 身份记录为准（含证件类型），
    // /user/info 仅作为兜底
    const myIdentity = list.find((item) => !item.isFollower)
    if (myIdentity) {
      bookerName.value = myIdentity.name || bookerName.value
      bookerTel.value = myIdentity.phone || bookerTel.value
      bookerIdCard.value = myIdentity.idNum || bookerIdCard.value
      bookerCardType.value = myIdentity.idType || bookerCardType.value
    }

    const limit = maxCompanions.value
    companions.value = allFollowers.value.slice(0, limit)
    checkedFollowerIds.value = companions.value.map((c) => c.userIdentityId)
    // 保存的随行人超过可携带数量 → 让用户选择带入哪些
    if (limit > 0 && allFollowers.value.length > limit) {
      followerPickerShow.value = true
    }
  } catch (e: any) {
    uni.showToast({ title: e?.message || '实名信息加载失败', icon: 'none' })
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

/* ===== 随行人选择 ===== */
function openFollowerPicker() {
  checkedFollowerIds.value = companions.value.map((c) => c.userIdentityId)
  followerPickerShow.value = true
}

function onToggleFollower(item: UserIdentityRow) {
  const idx = checkedFollowerIds.value.indexOf(item.userIdentityId)
  if (idx >= 0) {
    checkedFollowerIds.value.splice(idx, 1)
    return
  }
  if (checkedFollowerIds.value.length >= maxCompanions.value) {
    uni.showToast({ title: `最多携带${maxCompanions.value}位随行人`, icon: 'none' })
    return
  }
  checkedFollowerIds.value.push(item.userIdentityId)
}

function onFollowerPickerConfirm() {
  companions.value = allFollowers.value.filter((item) =>
    checkedFollowerIds.value.includes(item.userIdentityId)
  )
  followerPickerShow.value = false
}

function removeCompanion(item: UserIdentityRow) {
  companions.value = companions.value.filter((c) => c.userIdentityId !== item.userIdentityId)
}

/* ===== 添加随行人弹层（本页直接添加，不再跳转实名认证页） ===== */
const followerFormShow = ref(false)
const followerForm = ref<UserIdentityPayload>({ name: '', idType: 1, idNum: '', phone: '' })
const followerErrors = ref({ phone: '', idNum: '' })
const followerSubmitting = ref(false)
const cardPickerShow = ref(false)

/** 仅保留数字，最长 11 位 */
function keepDigits(v: string, max = 11): string {
  return (v || '').replace(/\D/g, '').slice(0, max)
}

/** 证件号输入过滤：身份证仅数字与 X（转大写）18 位，其他证件去空白 30 位 */
function filterIdNum(v: string, idType: number): string {
  const s = v || ''
  return idType === 1
    ? s.toUpperCase().replace(/[^0-9X]/g, '').slice(0, 18)
    : s.replace(/\s/g, '').slice(0, 30)
}

/** 校验手机号：返回错误文案，空串表示通过 */
function checkPhone(phone: string): string {
  if (!phone) return '请输入手机号码'
  if (!isValidPhone(phone)) return '手机号格式不正确：应为 1 开头的 11 位数字'
  return ''
}

/** 校验证件号：身份证按校验位算法验证，其他证件做长度校验 */
function checkIdNum(idNum: string, idType: number): string {
  if (!idNum) return '请输入证件号码'
  if (idType === 1 && !isValidIdCard(idNum)) {
    return '身份证号格式不正确：18 位且校验位需相符'
  }
  if (idType !== 1 && idNum.length < 5) {
    return '证件号码过短，请核对后重新输入'
  }
  return ''
}

function openAddFollower() {
  followerForm.value = { name: '', idType: 1, idNum: '', phone: '' }
  followerErrors.value = { phone: '', idNum: '' }
  followerFormShow.value = true
}

function onFollowerPhoneInput(e: any) {
  followerForm.value.phone = keepDigits(e?.detail?.value ?? '')
  followerErrors.value.phone = ''
}
function onFollowerPhoneBlur() {
  followerErrors.value.phone = checkPhone(followerForm.value.phone)
}
function onFollowerIdInput(e: any) {
  followerForm.value.idNum = filterIdNum(e?.detail?.value ?? '', followerForm.value.idType)
  followerErrors.value.idNum = ''
}
function onFollowerIdBlur() {
  followerErrors.value.idNum = checkIdNum(followerForm.value.idNum, followerForm.value.idType)
}
function onFollowerCardSelect(id: number) {
  followerForm.value.idType = id
  followerErrors.value.idNum = checkIdNum(followerForm.value.idNum, id)
  cardPickerShow.value = false
}

/** 提交添加随行人：保存成功后直接带入当前随行人列表；超出活动可携带上限则提示且不带入 */
async function onSubmitNewFollower() {
  const p = {
    name: followerForm.value.name.trim(),
    idType: followerForm.value.idType,
    idNum: followerForm.value.idNum.trim(),
    phone: followerForm.value.phone.trim(),
    isFollower: true,
  }
  if (!p.name) {
    uni.showToast({ title: '请输入姓名', icon: 'none' })
    return
  }
  followerErrors.value.phone = checkPhone(p.phone)
  followerErrors.value.idNum = checkIdNum(p.idNum, p.idType)
  const firstError = followerErrors.value.phone || followerErrors.value.idNum
  if (firstError) {
    uni.showToast({ title: firstError, icon: 'none' })
    return
  }
  followerSubmitting.value = true
  try {
    const prevIds = new Set(allFollowers.value.map((i) => i.userIdentityId))
    const list = await addFollower(p)
    allFollowers.value = list.filter((item) => item.isFollower)
    followerFormShow.value = false

    const added = allFollowers.value.find(
      (i) => !prevIds.has(i.userIdentityId) && i.idNum === p.idNum
    )
    if (maxCompanions.value <= 0) {
      uni.showToast({ title: '该项目不可携带随行人，已保存至随行人列表', icon: 'none' })
      return
    }
    if (companions.value.length >= maxCompanions.value) {
      // 已选随行人达到活动上限：仅保存，不带入当前列表
      uni.showToast({
        title: `当前活动最多携带${maxCompanions.value}位随行人，已保存未带入`,
        icon: 'none',
      })
      return
    }
    if (added) {
      companions.value.push(added)
      checkedFollowerIds.value.push(added.userIdentityId)
      uni.showToast({ title: '添加成功', icon: 'success' })
    }
  } catch (e: any) {
    uni.showToast({ title: e?.message || '添加失败', icon: 'none' })
  } finally {
    followerSubmitting.value = false
  }
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
  if (!bookerName.value.trim() || !bookerIdCard.value.trim()) {
    uni.showToast({ title: '实名信息不完整，请先完成实名认证', icon: 'none' })
    return
  }
  if (!isValidPhone(bookerTel.value)) {
    uni.showToast({ title: '实名手机号不正确，请先到「实名认证」中修正', icon: 'none' })
    return
  }
  if (bookerCardType.value === 1 && !isValidIdCard(bookerIdCard.value)) {
    uni.showToast({ title: '实名证件号格式不正确，请先到「实名认证」中修正', icon: 'none' })
    return
  }
  for (let i = 0; i < companions.value.length; i++) {
    const c = companions.value[i]
    if (!c.name.trim() || !c.idNum.trim()) {
      uni.showToast({ title: `随行人${i + 1}信息不完整，请到「实名认证」中维护`, icon: 'none' })
      return
    }
    if (c.idType === 1 && !isValidIdCard(c.idNum)) {
      uni.showToast({ title: `随行人${i + 1}身份证号格式不正确`, icon: 'none' })
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
      // 随行人取自已保存的身份记录，对应后端 FollowerReq（name/idNum/phone）
      companions: companions.value.map((c) => ({
        name: c.name.trim(),
        idType: c.idType,
        idNum: c.idNum.trim(),
        phone: c.phone || bookerTel.value,
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
  loadIdentity()
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

    <!-- 预约人信息：由实名认证自动带出，不可编辑 -->
    <view class="section">
      <view class="section__hd section__hd--row">
        <text>预约人信息</text>
        <text class="auto-tag">已从实名信息带出</text>
      </view>
      <view class="form-item">
        <text class="form-item__lbl">预约人姓名</text>
        <text class="form-item__val">{{ bookerName || '-' }}</text>
      </view>
      <view class="form-item">
        <text class="form-item__lbl">预约人手机</text>
        <text class="form-item__val">{{ bookerTel || '-' }}</text>
      </view>
      <view class="form-item">
        <text class="form-item__lbl">证件类型</text>
        <text class="form-item__val">{{ cardTypeTitle(bookerCardType) }}</text>
      </view>
      <view class="form-item">
        <text class="form-item__lbl">预约人证件号</text>
        <text class="form-item__val">{{ bookerIdCard || '-' }}</text>
      </view>
    </view>

    <!-- 随行人：由已保存的随行人列表勾选带入 -->
    <view class="section">
      <view class="section__hd section__hd--row">
        <text>随行人信息</text>
        <text class="pick" @tap="openFollowerPicker">选择随行人</text>
      </view>
      <view v-if="!companions.length" class="tip tip--empty">
        {{ maxCompanions > 0 ? '暂未携带随行人，可从已保存的随行人中选择' : '该项目不可携带随行人' }}
      </view>
      <view v-for="c in companions" :key="c.userIdentityId" class="follower">
        <view class="follower__info">
          <view class="follower__name">
            {{ c.name }}
            <text class="follower__type">{{ cardTypeTitle(c.idType) }}</text>
          </view>
          <view class="follower__meta">{{ c.idNum }}</view>
        </view>
        <text class="follower__del" @tap="removeCompanion(c)">移除</text>
      </view>
      <view class="follower-tip">
        <text>已选 {{ companions.length }}/{{ maxCompanions }} 位随行人</text>
        <text class="link" @tap="openAddFollower">添加随行人 ›</text>
      </view>
    </view>

    <view class="submit-btn" :class="{ disabled: submitting }" @tap="onSubmit">
      {{ submitting ? '提交中...' : '提交' }}
    </view>

    <!-- 随行人选择弹窗 -->
    <view v-if="followerPickerShow" class="modal-mask" @tap="followerPickerShow = false">
      <view class="sheet" @tap.stop>
        <view class="sheet__title">
          <text>选择随行人（最多{{ maxCompanions }}位）</text>
          <text class="sheet__close" @tap="followerPickerShow = false">×</text>
        </view>
        <view class="sheet__body">
          <view
            v-for="item in allFollowers"
            :key="item.userIdentityId"
            class="pick-item"
            :class="{ selected: checkedFollowerIds.includes(item.userIdentityId) }"
            @tap="onToggleFollower(item)"
          >
            <view class="pick-item__info">
              <view class="pick-item__name">
                {{ item.name }}
                <text class="pick-item__type">{{ cardTypeTitle(item.idType) }}</text>
              </view>
              <view class="pick-item__meta">{{ item.idNum }}</view>
            </view>
            <text class="pick-item__check">{{ checkedFollowerIds.includes(item.userIdentityId) ? '✓' : '' }}</text>
          </view>
          <view class="sheet__confirm" @tap="onFollowerPickerConfirm">确定</view>
        </view>
      </view>
    </view>

    <!-- 添加随行人弹层：本页直接填写并保存，不跳转实名认证页 -->
    <view v-if="followerFormShow" class="modal-mask" @tap="followerFormShow = false">
      <view class="sheet" @tap.stop>
        <view class="sheet__title">
          <text>添加随行人</text>
          <text class="sheet__close" @tap="followerFormShow = false">×</text>
        </view>
        <view class="sheet__body">
          <view class="form-item">
            <text class="form-item__lbl"><text class="req">*</text>姓名</text>
            <input
              class="form-item__input"
              v-model="followerForm.name"
              placeholder="请输入随行人姓名"
            />
          </view>
          <view class="form-item" @tap="cardPickerShow = true">
            <text class="form-item__lbl"><text class="req">*</text>证件类型</text>
            <text class="form-item__val">{{ cardTypeTitle(followerForm.idType) }}</text>
            <text class="arrow">›</text>
          </view>
          <view class="form-item">
            <text class="form-item__lbl"><text class="req">*</text>手机号码</text>
            <input
              class="form-item__input"
              :value="followerForm.phone"
              type="number"
              maxlength="11"
              placeholder="请输入手机号码"
              @input="onFollowerPhoneInput"
              @blur="onFollowerPhoneBlur"
            />
          </view>
          <view class="field-err" v-if="followerErrors.phone">{{ followerErrors.phone }}</view>
          <view class="form-item">
            <text class="form-item__lbl"><text class="req">*</text>证件号码</text>
            <input
              class="form-item__input"
              :value="followerForm.idNum"
              :maxlength="followerForm.idType === 1 ? 18 : 30"
              placeholder="请输入证件号码"
              @input="onFollowerIdInput"
              @blur="onFollowerIdBlur"
            />
          </view>
          <view class="field-err" v-if="followerErrors.idNum">{{ followerErrors.idNum }}</view>
          <view class="sheet__confirm" :class="{ disabled: followerSubmitting }" @tap="onSubmitNewFollower">
            {{ followerSubmitting ? '保存中...' : '保存并使用' }}
          </view>
        </view>
      </view>
    </view>

    <!-- 证件类型选择 -->
    <CardPicker
      :show="cardPickerShow"
      :selected="followerForm.idType"
      @select="onFollowerCardSelect"
      @close="cardPickerShow = false"
    />

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
.auto-tag {
  font-size: 22rpx;
  font-weight: 400;
  color: #00b42a;
  background: rgba(0, 180, 42, 0.08);
  border-radius: 8rpx;
  padding: 4rpx 12rpx;
}
.section__hd .pick {
  font-size: 26rpx;
  color: #3b7dff;
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
.field-err {
  padding: 0 32rpx 16rpx;
  text-align: right;
  font-size: 24rpx;
  color: #f53f3f;
  line-height: 1.4;
}
.arrow {
  color: #c9cdd4;
  font-size: 28rpx;
}
.tip {
  padding: 24rpx 32rpx;
  font-size: 26rpx;
  color: #86909c;
}
.follower {
  display: flex;
  align-items: center;
  padding: 24rpx 32rpx;
  border-top: 1px solid #f2f3f5;
}
.follower__info {
  flex: 1;
  min-width: 0;
}
.follower__name {
  font-size: 30rpx;
  font-weight: 600;
  color: #1f2329;
}
.follower__type {
  font-size: 22rpx;
  font-weight: 400;
  color: #3b7dff;
  background: rgba(59, 125, 255, 0.08);
  border-radius: 8rpx;
  padding: 2rpx 12rpx;
  margin-left: 16rpx;
}
.follower__meta {
  font-size: 24rpx;
  color: #86909c;
  margin-top: 8rpx;
}
.follower__del {
  font-size: 26rpx;
  color: #f53f3f;
}
.follower-tip {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24rpx 32rpx 28rpx;
  font-size: 24rpx;
  color: #86909c;
  border-top: 1px solid #f2f3f5;
}
.follower-tip .link {
  color: #3b7dff;
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
.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.55);
  z-index: 999;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
}
.sheet {
  background: #fff;
  border-radius: 40rpx 40rpx 0 0;
  max-height: 75vh;
  overflow-y: auto;
  padding-bottom: env(safe-area-inset-bottom);
}
.sheet__title {
  text-align: center;
  font-size: 32rpx;
  font-weight: 600;
  padding: 32rpx 0 16rpx;
  position: relative;
}
.sheet__close {
  position: absolute;
  right: 32rpx;
  top: 32rpx;
  width: 56rpx;
  height: 56rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #86909c;
  font-size: 40rpx;
}
.sheet__body {
  padding: 0 32rpx calc(32rpx + env(safe-area-inset-bottom));
}
.pick-item {
  display: flex;
  align-items: center;
  padding: 24rpx 0;
  border-bottom: 1px solid #f2f3f5;
}
.pick-item__info {
  flex: 1;
  min-width: 0;
}
.pick-item__name {
  font-size: 30rpx;
  font-weight: 600;
  color: #1f2329;
}
.pick-item__type {
  font-size: 22rpx;
  font-weight: 400;
  color: #3b7dff;
  background: rgba(59, 125, 255, 0.08);
  border-radius: 8rpx;
  padding: 2rpx 12rpx;
  margin-left: 16rpx;
}
.pick-item__meta {
  font-size: 24rpx;
  color: #86909c;
  margin-top: 6rpx;
}
.pick-item__check {
  width: 48rpx;
  text-align: center;
  font-size: 34rpx;
  color: #3b7dff;
  font-weight: 600;
}
.pick-item.selected .pick-item__name {
  color: #3b7dff;
}
.sheet__confirm {
  margin-top: 32rpx;
  background: linear-gradient(90deg, #3b7dff, #5b95ff);
  color: #fff;
  padding: 24rpx;
  border-radius: 56rpx;
  text-align: center;
  font-size: 30rpx;
  font-weight: 500;
}
.sheet__confirm.disabled {
  opacity: 0.6;
}
</style>
