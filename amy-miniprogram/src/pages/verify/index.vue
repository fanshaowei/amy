<script setup lang="ts">
import { ref, computed } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import {
  verifyUser,
  getFollowers,
  addFollower,
  updateFollower,
  deleteFollower,
} from '@/api/user'
import type { UserIdentityPayload } from '@/api/user'
import type { UserIdentity as UserIdentityRow } from '@/types'
import { cardTypeTitle } from '@/types'
import { isValidIdCard, isValidPhone } from '@/utils'
import { showError } from '@/utils/request'
import CardPicker from '@/components/CardPicker.vue'

const userStore = useUserStore()

/** 登录/实名完成后要回跳的页面（预约页带过来） */
const redirect = ref('')

/** 页面状态：verify=填写实名信息 manage=已实名（本人信息 + 随行人管理） */
const mode = ref<'verify' | 'manage'>('verify')
const loading = ref(false)

/* ===== 实名认证表单 ===== */
const form = ref({
  name: '',
  cardType: 1,
  phone: '',
  idNum: '',
})
const cardPickerShow = ref(false)
const cardPickerTarget = ref<'verify' | 'follower'>('verify')
const submitting = ref(false)

/* ===== 本人实名信息 & 随行人列表 ===== */
const myIdentity = ref<UserIdentityRow | null>(null)
const followers = ref<UserIdentityRow[]>([])

/* ===== 随行人编辑表单（弹层） ===== */
const followerFormShow = ref(false)
const followerForm = ref<UserIdentityPayload>({
  userIdentityId: undefined,
  name: '',
  idType: 1,
  idNum: '',
  phone: '',
})
const followerSubmitting = ref(false)

/* ===== 手机号 / 证件号 输入验证 ===== */
/** 实名认证表单的行内错误提示 */
const formErrors = ref({ phone: '', idNum: '' })
/** 随行人表单的行内错误提示 */
const followerErrors = ref({ phone: '', idNum: '' })

/** 实时过滤手机号输入：仅保留数字，最长 11 位 */
function keepDigits(v: string, max = 11): string {
  return (v || '').replace(/\D/g, '').slice(0, max)
}

/**
 * 实时过滤证件号输入：
 * 身份证仅保留数字与 X（自动转大写），最长 18 位；
 * 其他证件类型（护照/通行证等）去除空白，最长 30 位
 */
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

/**
 * 校验证件号：返回错误文案，空串表示通过。
 * 身份证按 GB 11643 校验位算法验证；其他证件做基础长度校验
 */
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

/* 实名认证表单输入处理 */
function onVerifyPhoneInput(e: any) {
  form.value.phone = keepDigits(e?.detail?.value ?? '')
  formErrors.value.phone = ''
}
function onVerifyPhoneBlur() {
  formErrors.value.phone = checkPhone(form.value.phone)
}
function onVerifyIdInput(e: any) {
  form.value.idNum = filterIdNum(e?.detail?.value ?? '', form.value.cardType)
  formErrors.value.idNum = ''
}
function onVerifyIdBlur() {
  formErrors.value.idNum = checkIdNum(form.value.idNum, form.value.cardType)
}

/* 随行人表单输入处理 */
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

/** 证件选择器当前选中值 */
const cardPickerSelected = computed(() =>
  cardPickerTarget.value === 'verify' ? form.value.cardType : followerForm.value.idType
)

function onCardSelect(id: number) {
  if (cardPickerTarget.value === 'verify') {
    form.value.cardType = id
    // 证件类型变了，原证件号的校验结论作废
    formErrors.value.idNum = checkIdNum(form.value.idNum, id)
  } else {
    followerForm.value.idType = id
    followerErrors.value.idNum = checkIdNum(followerForm.value.idNum, id)
  }
  cardPickerShow.value = false
}

/** 拉取本人信息 + 身份记录列表，并根据实名状态切换视图 */
async function refresh() {
  loading.value = true
  try {
    await userStore.fetchProfile()
    const list = await getFollowers()
    myIdentity.value = list.find((item) => !item.isFollower) || null
    followers.value = list.filter((item) => item.isFollower)
    // 预填手机号，减少输入
    if (mode.value === 'verify' && !form.value.phone && userStore.phone) {
      form.value.phone = userStore.phone
    }
    mode.value = userStore.user?.isVerified ? 'manage' : 'verify'
  } catch (e) {
    showError(e, '加载失败')
  } finally {
    loading.value = false
  }
}

/* ===== 实名认证提交 ===== */
function validateIdentity(
  p: { name: string; idType: number; idNum: string; phone: string },
  errors: { phone: string; idNum: string }
): boolean {
  if (!p.name.trim()) {
    uni.showToast({ title: '请输入姓名', icon: 'none' })
    return false
  }
  errors.phone = checkPhone(p.phone)
  errors.idNum = checkIdNum(p.idNum, p.idType)
  const firstError = errors.phone || errors.idNum
  if (firstError) {
    uni.showToast({ title: firstError, icon: 'none' })
    return false
  }
  return true
}

async function onSubmitVerify() {
  const p = {
    name: form.value.name.trim(),
    idType: form.value.cardType,
    idNum: form.value.idNum.trim(),
    phone: form.value.phone.trim(),
  }
  if (!validateIdentity(p, formErrors.value)) return
  submitting.value = true
  try {
    await verifyUser(p)
    uni.showToast({ title: '实名认证成功', icon: 'success' })
    if (redirect.value) {
      // 从预约页跳来：实名完成后直接回去（预约页会重新拉取实名信息）
      setTimeout(() => uni.redirectTo({ url: redirect.value }), 800)
      return
    }
    await refresh()
  } catch (e) {
    showError(e, '实名认证失败')
  } finally {
    submitting.value = false
  }
}

/* ===== 随行人管理 ===== */
function openAddFollower() {
  followerForm.value = { userIdentityId: undefined, name: '', idType: 1, idNum: '', phone: '' }
  followerErrors.value = { phone: '', idNum: '' }
  followerFormShow.value = true
}

function openEditFollower(item: UserIdentityRow) {
  followerForm.value = {
    userIdentityId: item.userIdentityId,
    name: item.name,
    idType: item.idType,
    idNum: item.idNum,
    phone: item.phone,
  }
  followerErrors.value = { phone: '', idNum: '' }
  followerFormShow.value = true
}

function openFollowerCardPicker() {
  cardPickerTarget.value = 'follower'
  cardPickerShow.value = true
}

async function onSubmitFollower() {
  const p = {
    userIdentityId: followerForm.value.userIdentityId,
    name: followerForm.value.name.trim(),
    idType: followerForm.value.idType,
    idNum: followerForm.value.idNum.trim(),
    phone: followerForm.value.phone.trim(),
    isFollower: true,
  }
  if (!validateIdentity(p, followerErrors.value)) return
  followerSubmitting.value = true
  try {
    const list = p.userIdentityId ? await updateFollower(p) : await addFollower(p)
    followers.value = list.filter((item) => item.isFollower)
    followerFormShow.value = false
    uni.showToast({ title: p.userIdentityId ? '修改成功' : '添加成功', icon: 'success' })
  } catch (e) {
    showError(e, '保存失败')
  } finally {
    followerSubmitting.value = false
  }
}

function onDeleteFollower(item: UserIdentityRow) {
  uni.showModal({
    title: '删除随行人',
    content: `确定删除随行人「${item.name}」吗？`,
    confirmColor: '#f53f3f',
    success: async (res) => {
      if (!res.confirm) return
      try {
        const list = await deleteFollower(item.userIdentityId)
        followers.value = list.filter((i) => i.isFollower)
        uni.showToast({ title: '已删除', icon: 'success' })
      } catch (e) {
        showError(e, '删除失败')
      }
    },
  })
}

onLoad((query) => {
  redirect.value = (query?.redirect as string) || ''
  if (!userStore.loggedIn) {
    uni.redirectTo({ url: '/pages/login/index?redirect=/pages/verify/index' })
    return
  }
})

onShow(() => {
  refresh()
})
</script>

<template>
  <view class="verify">
    <!-- 未实名：填写本人实名认证信息 -->
    <view v-if="mode === 'verify'" class="section">
      <view class="section__hd">实名认证信息</view>
      <view class="tip">根据场馆要求，预约前需完成本人实名认证</view>
      <view class="form-item">
        <text class="form-item__lbl"><text class="req">*</text>姓名</text>
        <input class="form-item__input" v-model="form.name" placeholder="请输入本人姓名" />
      </view>
      <view class="form-item" @tap="cardPickerTarget = 'verify'; cardPickerShow = true">
        <text class="form-item__lbl"><text class="req">*</text>证件类型</text>
        <text class="form-item__val">{{ cardTypeTitle(form.cardType) }}</text>
        <text class="arrow">›</text>
      </view>
      <view class="form-item">
        <text class="form-item__lbl"><text class="req">*</text>手机号码</text>
        <input
          class="form-item__input"
          :value="form.phone"
          type="number"
          maxlength="11"
          placeholder="请输入手机号码"
          @input="onVerifyPhoneInput"
          @blur="onVerifyPhoneBlur"
        />
      </view>
      <view class="field-err" v-if="formErrors.phone">{{ formErrors.phone }}</view>
      <view class="form-item">
        <text class="form-item__lbl"><text class="req">*</text>证件号码</text>
        <input
          class="form-item__input"
          :value="form.idNum"
          :maxlength="form.cardType === 1 ? 18 : 30"
          placeholder="请输入证件号码"
          @input="onVerifyIdInput"
          @blur="onVerifyIdBlur"
        />
      </view>
      <view class="field-err" v-if="formErrors.idNum">{{ formErrors.idNum }}</view>
      <view class="submit-btn" :class="{ disabled: submitting }" @tap="onSubmitVerify">
        {{ submitting ? '提交中...' : '提交认证' }}
      </view>
    </view>

    <!-- 已实名：本人信息 + 随行人管理 -->
    <view v-else>
      <view class="section">
        <view class="section__hd">本人实名信息</view>
        <view v-if="myIdentity" class="identity-card">
          <view class="identity-row"><text class="lbl">姓名</text><text>{{ myIdentity.name }}</text></view>
          <view class="identity-row"><text class="lbl">证件类型</text><text>{{ cardTypeTitle(myIdentity.idType) }}</text></view>
          <view class="identity-row"><text class="lbl">证件号码</text><text>{{ myIdentity.idNum }}</text></view>
          <view class="identity-row"><text class="lbl">手机号码</text><text>{{ myIdentity.phone }}</text></view>
        </view>
        <view v-else class="tip">已实名，但暂未获取到身份记录</view>
      </view>

      <view class="section">
        <view class="section__hd section__hd--row">
          <text>随行人管理</text>
          <text class="add" @tap="openAddFollower">＋ 添加随行人</text>
        </view>
        <view v-if="!followers.length" class="tip tip--empty">
          暂无随行人，添加后预约时可自动带出
        </view>
        <view v-for="item in followers" :key="item.userIdentityId" class="follower">
          <view class="follower__info">
            <view class="follower__name">
              {{ item.name }}
              <text class="follower__type">{{ cardTypeTitle(item.idType) }}</text>
            </view>
            <view class="follower__meta">{{ item.idNum }}</view>
            <view class="follower__meta">{{ item.phone }}</view>
          </view>
          <view class="follower__ops">
            <text class="op" @tap="openEditFollower(item)">修改</text>
            <text class="op op--danger" @tap="onDeleteFollower(item)">删除</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 随行人编辑弹层 -->
    <view v-if="followerFormShow" class="modal-mask" @tap="followerFormShow = false">
      <view class="sheet" @tap.stop>
        <view class="sheet__title">
          <text>{{ followerForm.userIdentityId ? '修改随行人' : '添加随行人' }}</text>
          <text class="sheet__close" @tap="followerFormShow = false">×</text>
        </view>
        <view class="sheet__body">
          <view class="form-item">
            <text class="form-item__lbl"><text class="req">*</text>姓名</text>
            <input class="form-item__input" v-model="followerForm.name" placeholder="请输入随行人姓名" />
          </view>
          <view class="form-item" @tap="openFollowerCardPicker">
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
          <view class="submit-btn" :class="{ disabled: followerSubmitting }" @tap="onSubmitFollower">
            {{ followerSubmitting ? '保存中...' : '保存' }}
          </view>
        </view>
      </view>
    </view>

    <!-- 证件类型选择 -->
    <CardPicker
      :show="cardPickerShow"
      :selected="cardPickerSelected"
      @select="onCardSelect"
      @close="cardPickerShow = false"
    />
  </view>
</template>

<style scoped lang="scss">
.verify {
  min-height: 100vh;
  padding-bottom: 60rpx;
}
.section {
  background: #fff;
  margin: 24rpx;
  border-radius: 24rpx;
  overflow: hidden;
  padding-bottom: 16rpx;
}
.section__hd {
  padding: 28rpx 32rpx 8rpx;
  font-size: 30rpx;
  font-weight: 600;
  color: #1f2329;
}
.section__hd--row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 16rpx;
}
.section__hd .add {
  font-size: 26rpx;
  color: #3b7dff;
  font-weight: 400;
}
.tip {
  padding: 0 32rpx 16rpx;
  font-size: 24rpx;
  color: #86909c;
}
.tip--empty {
  padding: 24rpx 32rpx 32rpx;
}
.form-item {
  padding: 28rpx 32rpx;
  border-top: 1px solid #f2f3f5;
  display: flex;
  align-items: center;
  gap: 24rpx;
  min-height: 80rpx;
}
.form-item__lbl {
  font-size: 28rpx;
  color: #1f2329;
  flex-shrink: 0;
  width: 160rpx;
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
.submit-btn {
  margin: 32rpx;
  background: linear-gradient(90deg, #3b7dff, #5b95ff);
  color: #fff;
  padding: 26rpx;
  border-radius: 56rpx;
  text-align: center;
  font-size: 30rpx;
  font-weight: 500;
  box-shadow: 0 8rpx 32rpx rgba(59, 125, 255, 0.35);
}
.submit-btn.disabled {
  opacity: 0.6;
}
.identity-card {
  padding: 8rpx 32rpx 24rpx;
}
.identity-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20rpx 0;
  border-bottom: 1px solid #f2f3f5;
  font-size: 28rpx;
  color: #1f2329;
}
.identity-row:last-child {
  border-bottom: 0;
}
.identity-row .lbl {
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
.follower__ops {
  display: flex;
  gap: 32rpx;
}
.op {
  font-size: 26rpx;
  color: #3b7dff;
}
.op--danger {
  color: #f53f3f;
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
  max-height: 80vh;
  overflow-y: auto;
  padding-bottom: env(safe-area-inset-bottom);
}
.sheet__title {
  text-align: center;
  font-size: 32rpx;
  font-weight: 600;
  padding: 32rpx 0 8rpx;
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
  padding-bottom: 16rpx;
}
</style>
