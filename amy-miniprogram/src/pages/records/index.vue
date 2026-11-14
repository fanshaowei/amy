<script setup lang="ts">
import PageHero from '@/components/PageHero.vue'
import { ref } from 'vue'
import { onShow, onPullDownRefresh } from '@dcloudio/uni-app'
import type { ReserveRecord } from '@/types'
import { useUserStore } from '@/stores/user'
import { useReserveStore } from '@/stores/reserve'
import { showError } from '@/utils/request'
import RecordCard from '@/components/RecordCard.vue'
import EmptyState from '@/components/EmptyState.vue'

const userStore = useUserStore()
const reserveStore = useReserveStore()

const records = ref<ReserveRecord[]>([])
const loading = ref(true)

async function load() {
  if (!userStore.loggedIn) {
    uni.navigateTo({ url: '/pages/login/index' })
    return
  }
  loading.value = true
  try {
    await reserveStore.fetchList()
    records.value = reserveStore.records
  } catch (e) {
    showError(e, '加载失败')
  } finally {
    loading.value = false
    uni.stopPullDownRefresh()
  }
}

function onCardClick(orderNum: string) {
  uni.navigateTo({ url: `/pages/records-detail/index?orderNum=${orderNum}` })
}

onShow(() => {
  load()
})

onPullDownRefresh(() => {
  load()
})
</script>

<template>
  <view class="records">
    <PageHero title="预约记录" eyebrow="MY RESERVATIONS" subtitle="查看与管理您的艺术之约" compact />
    <view v-if="loading" class="records__loading">加载中...</view>
    <view v-else-if="records.length">
      <RecordCard
        v-for="r in records"
        :key="r.reservationNum"
        :record="r"
        @click="onCardClick"
      />
    </view>
    <EmptyState v-else name="list" text="暂无预约记录" />
  </view>
</template>

<style scoped lang="scss">
.records {
  min-height: 100vh;
  padding: 0 0 $art-page-bottom;
}
.records__loading {
  text-align: center;
  color: $uni-text-color-grey;
  padding: 100rpx 0;
  font-size: 26rpx;
}
</style>
