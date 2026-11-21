<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow, onPullDownRefresh } from '@dcloudio/uni-app'
import type { Activity, NextDayReservation, NoticeItem } from '@/types'
import { getActivities } from '@/api/activity'
import { getNextDayReservations } from '@/api/reserve'
import { getNotices } from '@/api/notice'
import { useUserStore } from '@/stores/user'
import { maskName } from '@/utils'
import { showError } from '@/utils/request'
import ActivityCard from '@/components/ActivityCard.vue'
import EmptyState from '@/components/EmptyState.vue'
import AppIcon from '@/components/AppIcon.vue'
import ArtEmblem from '@/components/ArtEmblem.vue'

const userStore = useUserStore()
const activities = ref<Activity[]>([])
const loading = ref(true)
const keyword = ref('')

/* 首页「预约动态」：当前生效放号日期下的已预约名单（公开信息，姓名脱敏） */
const nextDayReservations = ref<NextDayReservation[]>([])

/** 顶部日期胶囊：本地时区，不依赖后端 */
const todayLabel = computed(() => {
  const d = new Date()
  const week = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][d.getDay()]
  return `${d.getMonth() + 1}月${d.getDate()}日 · ${week}`
})

/** 搜索框是真过滤，不再是装饰：按项目名称模糊匹配 */
const filteredActivities = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return activities.value
  return activities.value.filter((a) => (a.projectName || '').toLowerCase().includes(kw))
})

function clearKeyword() {
  keyword.value = ''
}

const noticeItems = computed(() =>
  nextDayReservations.value.map((item) => {
    const time = (item.reservationTime || '').slice(11, 16)
    const day = (item.reservationTime || '').slice(5, 10)
    const who = maskName(item.name) + '(' + item.phone + ')'
    return time ? `${who} 预约了${day} ${time} 场次` : `${who} 预约了${day}场次`
  })
)

/** 拉取预约动态（日期由后端生效放号库存决定）：需登录，未登录/失效时静默失败，不影响首页展示 */
async function loadNotice() {
  if (!userStore.loggedIn) {
    nextDayReservations.value = []
    return
  }
  try {
    const list = await getNextDayReservations()
    nextDayReservations.value = list
      .filter((item) => !!item.name)
      .sort((a, b) => (a.reservationTime || '').localeCompare(b.reservationTime || ''))
      .slice(0, 50)
  } catch (e) {
    nextDayReservations.value = []
  }
}

async function load() {
  loading.value = true
  try {
    activities.value = await getActivities()
  } catch (e) {
    // 展示后端返回的真实原因（如网关限流「操作过于频繁，请稍候再试」）
    showError(e, '加载失败')
  } finally {
    loading.value = false
    uni.stopPullDownRefresh()
  }
  loadNotice()
  loadAnnouncements()
}

/* ---------- 通知公告：Hero 下方细条，单条滚动展示 ---------- */
const announcements = ref<NoticeItem[]>([])

/** 拉取通知公告：公开接口，未登录也可看；失败静默，不影响首页 */
async function loadAnnouncements() {
  try {
    announcements.value = await getNotices(5)
  } catch (e) {
    announcements.value = []
  }
}

/** 点击公告标题 → 公告列表页 */
function goNoticeList() {
  uni.navigateTo({ url: '/pages/notice/list/index' })
}

/** 点击预约：先在首页完成登录/实名校验，通过后才进预约详情页 */
async function onBook(id: number) {
  // 未登录 → 登录页（登录成功后回跳详情页）
  if (!userStore.loggedIn) {
    uni.navigateTo({
      url: `/pages/login/index?redirect=${encodeURIComponent(`/pages/home/index`)}`,
    })
    return
  }
  // 已登录 → 校验实名状态（静默刷新，缓存兜底；失败不阻断，详情页仍会兜底校验）
  try {
    const user = (await userStore.fetchProfile(true)) || userStore.user
    if (!user?.isVerified) {
      // 未实名不自动跳转，先让用户确认
      uni.showModal({
        title: '需要实名认证',
        content: '预约前需先完成实名认证，认证完成将自动返回继续预约',
        confirmText: '去认证',
        cancelText: '取消',
        success: (res) => {
          if (res.confirm) {
            uni.navigateTo({
              url: `/pages/verify/index?redirect=${encodeURIComponent(`/pages/home/index`)}`,
            })
          }
        },
      })
      return
    }
  } catch (e) {
    // 忽略，交由详情页兜底
  }
  uni.navigateTo({ url: `/pages/detail/index?id=${id}` })
}

/* ---------- 快捷入口 ---------- */
function goRecords() {
  uni.switchTab({ url: '/pages/records/index' })
}
function goVerify() {
  uni.navigateTo({ url: '/pages/verify/index' })
}
function goAbout() {
  uni.navigateTo({ url: '/pages/about/index' })
}
function goProfile() {
  uni.navigateTo({ url: userStore.loggedIn ? '/pages/edit/index' : '/pages/login/index' })
}

onShow(() => {
  load()
})

onPullDownRefresh(() => {
  load()
})
</script>

<template>
  <view class="home">
    <!-- ============ 品牌 Hero：深靛玻璃 + 日轮 + 孔雀翎羽 ============ -->
    <view class="hero anim-up">
      <view class="hero__wave"></view>
      <view class="hero__body">
        <view class="hero__eyebrow">
          <view class="hero__rule"></view>
          <text>SUN PALACE · ART SPACE</text>
        </view>
        <view class="hero__title">太阳宫<text class="hero__title-accent">艺术空间</text></view>
        <view class="hero__slogan">孔雀 · 月光 · 洱海</view>
        <view class="hero__chips">
          <view class="hero__chip">
            <AppIcon name="calendar" :size="22" color="#f0cf94" />
            <text>{{ todayLabel }}</text>
          </view>
          <view class="hero__chip">
            <text class="hero__live-dot"></text>
            <text>开放预约中</text>
          </view>
        </view>
      </view>
      <!-- ============ 通知公告条：紧贴 Hero 下方，单条滚动 ============ -->
    <view v-if="announcements.length" class="announce anim-up notic">
      <image class="announce__icon" src="/static/公告.png" mode="aspectFit" />
      <text class="announce__label">通知公告：</text>
      <swiper
        class="announce__swiper"
        vertical
        autoplay
        circular
        :interval="3200"
        :duration="480"
      >
        <swiper-item v-for="n in announcements" :key="n.noticeId">
          <view class="announce__item" @tap="goNoticeList">
            <text class="announce__text text-ellipsis">{{ n.noticeTitle }}</text>
            <text class="announce__date">{{ (n.createTime || '').slice(5, 10) }}</text>
          </view>
        </swiper-item>
      </swiper>
    </view>

    <view v-else class="announce anim-up notic">
      <image class="announce__icon" src="/static/公告.png" mode="aspectFit" />
      <text class="announce__label">通知公告：</text>
      <view class="announce__item" @tap="goNoticeList">
        <text class="announce__text text-ellipsis">暂无公告</text>
        <text class="announce__date"> </text>
      </view>
    </view>
      <view class="hero__emblem"><ArtEmblem :size="196" /></view>
      <view class="hero__corner"></view>
      
    </view>

    

    <!-- ============ 搜索 ============ -->
    <view class="home__search anim-up--1">
      <view class="search-box">
        <AppIcon name="search" :size="30" color="#8a93b2" />
        <input
          class="search-box__input"
          v-model="keyword"
          type="text"
          confirm-type="search"
          placeholder="搜索活动名称"
          placeholder-class="search-box__ph"
        />
        <view v-if="keyword" class="search-box__clear" @tap="clearKeyword">
          <AppIcon name="close" :size="24" color="#8a93b2" />
        </view>
      </view>
    </view>

    <!-- ============ 快捷入口 ============ -->
    <!-- <view class="quick anim-up--1">
      <view class="quick__item" @tap="goRecords">
        <view class="quick__icon quick__icon--gold">
          <AppIcon name="ticket" :size="36" color="#f0cf94" />
        </view>
        <text class="quick__label">我的预约</text>
        <text class="quick__en">RECORDS</text>
      </view>
      <view class="quick__item" @tap="goVerify">
        <view class="quick__icon quick__icon--jade">
          <AppIcon name="shield" :size="36" color="#8ce0d2" />
        </view>
        <text class="quick__label">实名认证</text>
        <text class="quick__en">VERIFY</text>
      </view>
      <view class="quick__item" @tap="goAbout">
        <view class="quick__icon quick__icon--indigo">
          <AppIcon name="info" :size="36" color="#b9c9ff" />
        </view>
        <text class="quick__label">关于我们</text>
        <text class="quick__en">ABOUT</text>
      </view>
      <view class="quick__item" @tap="goProfile">
        <view class="quick__icon quick__icon--rose">
          <AppIcon name="user" :size="36" color="#f4bcd2" />
        </view>
        <text class="quick__label">我的资料</text>
        <text class="quick__en">PROFILE</text>
      </view>
    </view> -->

    <!-- ============ 项目卡片 ============ -->
    <view v-if="loading" class="home__skeletons">
      <view v-for="i in 2" :key="i" class="skeleton">
        <view class="skeleton__pic"></view>
        <view class="skeleton__line skeleton__line--title"></view>
        <view class="skeleton__line"></view>
        <view class="skeleton__block"></view>
      </view>
    </view>
    <view v-else-if="filteredActivities.length" class="activity-list">
      <view
        v-for="(a, i) in filteredActivities"
        :key="a.projectId"
        class="anim-up"
        :style="{ animationDelay: i * 0.07 + 's' }"
      >
        <ActivityCard :activity="a" :index="i" @book="onBook" />
      </view>
    </view>
    <EmptyState v-else name="feather" :text="keyword ? '没有找到相关项目' : '暂无活动'" />

    <!-- ============ 预约动态 ============ -->
    <view v-if="noticeItems.length" class="notice anim-up--3">
      <view class="notice__head">
        <view class="notice__icon">
          <AppIcon name="bell" :size="28" color="#f0cf94" />
        </view>
        <view class="notice__titles">
          <text class="notice__title">预约动态</text>
          <text class="notice__en">UPCOMING RESERVATIONS</text>
        </view>
        <text class="notice__count">{{ noticeItems.length }} 人已约</text>
      </view>
      <view class="notice__divider"></view>
      <swiper
        v-if="noticeItems.length > 3"
        class="notice__swiper"
        vertical
        autoplay
        circular
        :interval="2600"
        :duration="480"
        :display-multiple-items="3"
      >
        <swiper-item v-for="(item, i) in noticeItems" :key="i">
          <view class="notice__item text-ellipsis">
            <text class="notice__bullet"></text>{{ item }}
          </view>
        </swiper-item>
      </swiper>
      <view v-else class="notice__list">
        <view v-for="(item, i) in noticeItems" :key="i" class="notice__item text-ellipsis">
          <text class="notice__bullet"></text>{{ item }}
        </view>
      </view>
    </view>
    <view v-else class="notice anim-up--3">
      <view class="notice__head">
        <view class="notice__icon">
          <AppIcon name="bell" :size="28" color="#f0cf94" />
        </view>
        <view class="notice__titles">
          <text class="notice__title">预约动态</text>
          <text class="notice__en">UPCOMING RESERVATIONS</text>
        </view>
        <text class="notice__count">{{ noticeItems.length }} 人已约</text>
      </view>
      <view class="notice__divider"></view>
      <view class="notice__item text-ellipsis">
        <text class="notice__bullet"></text>
        <text>暂无预约动态</text>
      </view>
    </view>


    <!-- 页脚落款 -->
    <view class="home__footer">
      <view class="home__footer-rule"></view>
      <text class="home__footer-star">✦</text>
      <text>太阳宫艺术空间 · 与美相约</text>
      <view class="home__footer-rule"></view>
    </view>
  </view>
</template>

<style scoped lang="scss">
.home {
  min-height: 100vh;
  padding-bottom: 60rpx;
}

/* ==================== Hero ==================== */
.hero {
  position: relative;
  margin: 16rpx 24rpx 0;
  height: 340rpx;
  border-radius: 36rpx;
  overflow: hidden;
  box-sizing: border-box;
  padding: 32rpx 30rpx;
  background-color: rgba(36, 58, 114, 0.82);
  background-image: linear-gradient(118deg, rgba(32, 51, 106, 0.94) 0%, rgba(30, 92, 130, 0.86) 52%, rgba(31, 138, 132, 0.76) 100%);
  -webkit-backdrop-filter: blur(20px) saturate(170%);
  backdrop-filter: blur(20px) saturate(170%);
  box-shadow: 0 20rpx 52rpx rgba(20, 36, 82, 0.3), inset 0 1rpx 0 rgba(255, 255, 255, 0.18);
}
/* 底部水波弧：两层半透明椭圆叠出洱海水光 */
.hero__wave {
  position: absolute;
  left: -30%;
  right: -30%;
  bottom: -230rpx;
  height: 300rpx;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.09);
  box-shadow: 0 -30rpx 0 rgba(255, 255, 255, 0.05);
}
.hero__corner {
  position: absolute;
  right: -60rpx;
  top: -80rpx;
  width: 260rpx;
  height: 260rpx;
  border-radius: 50%;
  border: 1rpx solid rgba(240, 207, 148, 0.28);
}
.hero__body {
  position: relative;
  z-index: 2;
  width: 66%;
}
.hero__eyebrow {
  display: flex;
  align-items: center;
  gap: 12rpx;
  font-size: 18rpx;
  letter-spacing: 2rpx;
  color: rgba(240, 207, 148, 0.94);
  margin-bottom: 12rpx;
}
.hero__rule {
  @include art-rule(40rpx);
}
.hero__title {
  font-size: 42rpx;
  font-weight: 700;
  line-height: 1.24;
  letter-spacing: 2rpx;
  color: #fffaf0;
  text-shadow: 0 2rpx 12rpx rgba(12, 28, 70, 0.28);
}
.hero__title-accent {
  display: block;
  font-size: 26rpx;
  font-weight: 500;
  letter-spacing: 6rpx;
  color: #f0cf94;
  margin-top: 4rpx;
}
.hero__slogan {
  margin-top: 10rpx;
  font-size: 20rpx;
  letter-spacing: 1rpx;
  color: rgba(255, 255, 255, 0.74);
}
.hero__chips {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-top: 18rpx;
}
.hero__chip {
  display: flex;
  align-items: center;
  gap: 8rpx;
  padding: 8rpx 16rpx;
  border-radius: 24rpx;
  font-size: 20rpx;
  color: #fff;
  background: rgba(255, 255, 255, 0.14);
  -webkit-backdrop-filter: blur(8px);
  backdrop-filter: blur(8px);
}
.hero__live-dot {
  width: 10rpx;
  height: 10rpx;
  border-radius: 50%;
  background: #8ce6cf;
  box-shadow: 0 0 10rpx #8ce6cf;
  animation: notice-pulse 1.8s ease-in-out infinite;
}
.hero__emblem {
  position: absolute;
  right: -8rpx;
  top: 58rpx;
  z-index: 1;
  opacity: 0.96;
}

/* ==================== 搜索 ==================== */
.home__search {
  padding: 24rpx 24rpx 0;
}
.search-box {
  @include glass-inset(36rpx);
  background: rgba(255, 255, 255, 0.66);
  height: 76rpx;
  display: flex;
  align-items: center;
  padding: 0 26rpx;
  gap: 16rpx;
}
.search-box__input {
  flex: 1;
  min-width: 0;
  height: 76rpx;
  font-size: 26rpx;
  color: $uni-text-color;
}
.search-box__ph {
  color: $uni-text-color-placeholder;
  font-size: 26rpx;
}
.search-box__clear {
  width: 48rpx;
  height: 48rpx;
  border-radius: 50%;
  background: rgba(138, 147, 178, 0.14);
  display: flex;
  align-items: center;
  justify-content: center;
}

/* ==================== 快捷入口：与 Hero / 海报卡同色系 ==================== */
.quick {
  display: flex;
  margin: 24rpx 24rpx 0;
  @include art-dark-glass(28rpx);
  padding: 26rpx 10rpx 22rpx;
}
.quick__item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4rpx;
}
.quick__icon {
  @include art-icon-well(rgba(255, 255, 255, 0.1));
  width: 84rpx;
  height: 84rpx;
  border-radius: 28rpx;
  margin-bottom: 10rpx;
  box-shadow: inset 0 1rpx 0 rgba(255, 255, 255, 0.14);
}
.quick__icon--gold { background: rgba(240, 207, 148, 0.14); }
.quick__icon--jade { background: rgba(140, 224, 210, 0.13); }
.quick__icon--indigo { background: rgba(185, 201, 255, 0.13); }
.quick__icon--rose { background: rgba(244, 188, 210, 0.13); }
.quick__label {
  font-size: 24rpx;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.94);
}
.quick__en {
  font-size: 16rpx;
  letter-spacing: 1rpx;
  color: rgba(255, 255, 255, 0.46);
}

/* ==================== 骨架屏 ==================== */
.home__skeletons {
  padding: 32rpx 24rpx 0;
}
.skeleton {
  @include art-dark-glass(32rpx);
  padding-bottom: 32rpx;
  margin-bottom: 28rpx;
  overflow: hidden;
}
.skeleton__pic {
  height: 300rpx;
  background: linear-gradient(100deg, rgba(255, 255, 255, 0.04) 20%, rgba(255, 255, 255, 0.13) 50%, rgba(255, 255, 255, 0.04) 80%);
  background-size: 300% 100%;
  animation: skeleton-flow 1.6s linear infinite;
}
.skeleton__line {
  height: 24rpx;
  margin: 28rpx 32rpx 0;
  border-radius: 12rpx;
  background: rgba(255, 255, 255, 0.12);
  width: 60%;
}
.skeleton__line--title {
  width: 78%;
  height: 34rpx;
}
.skeleton__block {
  height: 120rpx;
  margin: 28rpx 32rpx 0;
  border-radius: 20rpx;
  background: rgba(255, 255, 255, 0.08);
}
@keyframes skeleton-flow {
  0% { background-position: 100% 0; }
  100% { background-position: -100% 0; }
}

.activity-list {
  padding: 32rpx 24rpx 0;
}

/* ==================== 预约动态 ==================== */
.notice {
  padding: 24rpx 26rpx 20rpx;
  margin: 36rpx 24rpx 0;
  @include art-dark-glass(28rpx);
  overflow: hidden;
}
.notice__head {
  display: flex;
  align-items: center;
  gap: 16rpx;
}
.notice__icon {
  @include art-icon-well(rgba(240, 207, 148, 0.14));
  width: 56rpx;
  height: 56rpx;
  box-shadow: inset 0 1rpx 0 rgba(255, 255, 255, 0.14);
}
.notice__titles {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2rpx;
}
.notice__title {
  font-size: 27rpx;
  font-weight: 700;
  letter-spacing: 1rpx;
  color: rgba(255, 255, 255, 0.94);
}
.notice__en {
  font-size: 16rpx;
  letter-spacing: 1.6rpx;
  color: rgba(255, 255, 255, 0.46);
}
.notice__count {
  font-size: 22rpx;
  color: #f0cf94;
  font-weight: 600;
}
.notice__divider {
  height: 1rpx;
  margin: 20rpx 0 12rpx;
  background: linear-gradient(90deg, $art-gradient-gold, rgba(200, 163, 92, 0.04));
}
.notice__swiper {
  height: 132rpx;
}
.notice__item {
  height: 44rpx;
  line-height: 44rpx;
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.72);
  display: flex;
  align-items: center;
}
.notice__bullet {
  width: 8rpx;
  height: 8rpx;
  border-radius: 50%;
  background: #8ce0d2;
  margin-right: 12rpx;
  flex-shrink: 0;
  opacity: 0.72;
}
@keyframes notice-pulse {
  0%,
  100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.4; transform: scale(0.7); }
}

/* ==================== 通知公告条（紧贴 Hero 下方） ==================== */
.announce {
  display: flex;
  align-items: center;
  gap: 10rpx;
  margin: 16rpx 24rpx 0;
  padding: 0 22rpx;
  height: 68rpx;
  @include art-dark-glass(20rpx);
  overflow: hidden;
}
.announce__icon {
  flex-shrink: 0;
  width: 36rpx;
  height: 44rpx;
}
.announce__label {
  flex-shrink: 0;
  font-size: 22rpx;
  font-weight: 600;
  letter-spacing: 1rpx;
  color: #f0cf94;
}
.announce__swiper {
  flex: 1;
  min-width: 0;
  height: 68rpx;
}
.announce__item {
  height: 68rpx;
  display: flex;
  align-items: center;
}
.announce__text {
  flex: 1;
  min-width: 0;
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.78);
}
.announce__date {
  flex-shrink: 0;
  margin-left: 16rpx;
  font-size: 20rpx;
  color: rgba(255, 255, 255, 0.4);
}

/* ==================== 页脚 ==================== */
.home__footer {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16rpx;
  margin-top: 56rpx;
  font-size: 20rpx;
  letter-spacing: 2rpx;
  color: $uni-text-color-grey;
}
.home__footer-rule {
  @include art-rule(56rpx);
  opacity: 0.6;
}
.home__footer-star {
  color: $art-gold;
  font-size: 22rpx;
}
.notic {
    background-color: rgba(0,0,0,0.5);
    padding: 0rpx 10rpx;
    margin-top: 16rpx;
    margin-bottom: 0rpx;
    margin-right: 0rpx;
    margin-left: 0rpx;
    height: 56rpx;
}
</style>
