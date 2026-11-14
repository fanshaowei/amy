<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'

const props = defineProps<{ seconds: number }>()
const emit = defineEmits<{ (e: 'timeout'): void }>()

let timer: ReturnType<typeof setInterval> | null = null
const remain = ref(props.seconds)

const display = computed(() => {
  const m = Math.floor(remain.value / 60)
  const s = remain.value % 60
  return `${m}:${s.toString().padStart(2, '0')}`
})

const isUrgent = computed(() => remain.value <= 60)

function tick() {
  remain.value--
  if (remain.value <= 0) {
    stop()
    emit('timeout')
  }
}

function start() {
  stop()
  remain.value = props.seconds
  timer = setInterval(tick, 1000)
}

function stop() {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

onMounted(start)
onBeforeUnmount(stop)

watch(
  () => props.seconds,
  (val) => {
    remain.value = val
  }
)
</script>

<template>
  <view class="countdown-bar" :class="{ urgent: isUrgent }">
    <text>预约剩余时间：</text>
    <text class="countdown-bar__time">{{ display }}</text>
  </view>
</template>

<style scoped lang="scss">
.countdown-bar {
  @include art-card(0.9, 14px, 0rpx);
  border-bottom: 1rpx solid rgba(240, 207, 148, 0.26);
  color: rgba(255, 255, 255, 0.76);
  padding: 20rpx 32rpx;
  text-align: center;
  font-size: 26rpx;
  font-weight: 500;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
}
.countdown-bar.urgent {
  background-image: linear-gradient(120deg, rgba(158, 62, 62, 0.5) 0%, rgba(20, 38, 78, 0) 82%);
}
.countdown-bar__time {
  font-size: 34rpx;
  font-weight: 700;
  letter-spacing: 2rpx;
  color: $art-accent;
  font-family: 'DIN Alternate', 'Helvetica Neue', sans-serif;
}
.countdown-bar.urgent .countdown-bar__time {
  color: #ffb0a0;
}
</style>
