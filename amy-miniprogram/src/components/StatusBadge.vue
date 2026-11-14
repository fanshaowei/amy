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
  background: rgba(255, 255, 255, 0.08);
  -webkit-backdrop-filter: blur(10px) saturate(160%);
  backdrop-filter: blur(10px) saturate(160%);
  box-shadow: inset 0 1rpx 0 rgba(240, 207, 148, 0.16);
}
/* 深靛卡上的状态色：统一提亮，原来的深橙/深绿在深底上会糊成一片 */
.badge--pending {
  background: rgba(255, 196, 122, 0.16);
  color: #ffcb85;
}
.badge--done {
  background: rgba(127, 224, 168, 0.16);
  color: #9be8bf;
}
.badge--expired {
  background: rgba(255, 255, 255, 0.08);
  color: rgba(255, 255, 255, 0.6);
}
.badge--cancel,
.badge--fail {
  background: rgba(255, 156, 156, 0.14);
  color: #ffabab;
}
.badge--booking {
  background: rgba(140, 224, 210, 0.14);
  color: $art-accent-jade;
}
</style>
