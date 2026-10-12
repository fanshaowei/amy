<script setup lang="ts">
import { computed } from 'vue'
import { RESERVE_STATUS } from '@/types'

const props = defineProps<{ status: number }>()

const info = computed(() => RESERVE_STATUS[props.status] || { text: '未知', type: 'expired' })
</script>

<template>
  <text class="badge" :class="`badge--${info.type}`">{{ info.text }}</text>
</template>

<style scoped lang="scss">
.badge {
  padding: 6rpx 20rpx;
  border-radius: 24rpx;
  font-size: 22rpx;
  line-height: 1.4;
  flex-shrink: 0;
}
.badge--pending {
  background: #fff7e8;
  color: #ff9500;
}
.badge--done {
  background: #e8f8f0;
  color: #00b96b;
}
.badge--expired {
  background: #f2f3f5;
  color: #86909c;
}
.badge--cancel {
  background: #fff1f0;
  color: #f53f3f;
}
</style>
