<template>
  <view v-if="show" class="captcha-mask" @tap="close">
    <view class="captcha-box" @tap.stop>
      <view class="captcha-top">
        <text class="captcha-top__title">请完成安全验证</text>
        <text class="captcha-top__close" @tap="close">×</text>
      </view>

      <view class="captcha-body">
        <!-- 拼图区域 -->
        <view class="captcha-img-panel" :style="imgPanelStyle">
          <image
            class="captcha-img"
            :src="backImgBase ? 'data:image/jpeg;base64,' + backImgBase : ''"
            mode="scaleToFill"
          />
          <view class="captcha-refresh" @tap="refresh">⟳</view>
          <!-- 品牌字样：用 CSS 文字柔和渲染，替代后端画进底图的锯齿白字 -->
          <text class="captcha-brand">太阳宫艺术空间</text>
          <text v-if="tipWords" class="captcha-tips" :class="passFlag ? 'captcha-tips--ok' : 'captcha-tips--err'">
            {{ tipWords }}
          </text>
        </view>

        <!-- 滑动条 -->
        <view class="captcha-bar-area" :style="barStyle">
          <text class="captcha-bar-msg">{{ text }}</text>
          <view class="captcha-left-bar" :style="leftBarStyle">
            <text class="captcha-bar-msg">{{ finishText }}</text>
            <view
              class="captcha-move-block"
              :style="moveBlockStyle"
              @touchstart="onTouchStart"
              @touchmove.stop.prevent="onTouchMove"
              @touchend="onTouchEnd"
              @touchcancel="onTouchCancel"
            >
              <text class="captcha-move-icon" :style="{ color: iconColor }">{{ iconText }}</text>
              <view class="captcha-sub-block" :style="subBlockStyle">
                <image
                  v-if="blockBackImgBase"
                  class="captcha-img"
                  :src="'data:image/png;base64,' + blockBackImgBase"
                  mode="scaleToFill"
                />
              </view>
            </view>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
/**
 * 滑动拼图验证码弹窗（aj-captcha 官方 uni-app SDK 移植版）
 *
 * 流程：
 *  1. POST /spas/mina/app/captcha/get    → 底图 + 滑块图 + token + secretKey
 *  2. 用户拖动滑块到拼图缺口，坐标 AES 加密后 POST /captcha/check
 *  3. 校验通过返回一次性 captchaVerification，emit('success', ...) 给调用方，
 *     由调用方随业务请求提交，后端做二次校验（防脚本刷单）
 */
import { ref, computed, watch, nextTick, getCurrentInstance } from 'vue'
import { request } from '@/utils/request'
import { aesEncrypt } from '@/utils/aes'

const props = defineProps<{ show: boolean }>()
const emit = defineEmits<{
  (e: 'update:show', v: boolean): void
  (e: 'success', captchaVerification: string): void
}>()

/* 与后端 aj-captcha 底图尺寸对应：原图宽 620px，前端按 310px 展示 */
const IMG_WIDTH = 310
const IMG_HEIGHT = 155
const BAR_HEIGHT = 40
/** 上报坐标缩放系数：1.4.0 内置底图 310 原生宽、显示 1:1；老版 620 宽底图需改 2 */
const COORD_SCALE = 1
const BLOCK_SRC_WIDTH = 47
/** 图片面板与滑动条之间的间距，滑块小图定位必须同步扣掉这段距离 */
const IMG_BAR_GAP = 14

const instance = getCurrentInstance()

const backImgBase = ref('')
const blockBackImgBase = ref('')
const backToken = ref('')
const secretKey = ref('')

const text = ref('向右滑动完成验证')
const finishText = ref('')
const tipWords = ref('')
const passFlag = ref(false)

const moveBlockLeft = ref(0)
const leftBarWidth = ref<number | null>(null)
const moveBlockBg = ref('#ffffff')
const leftBarBorder = ref('#e4e7eb')
const iconColor = ref('#909399')
const iconText = ref('»')
const transitioning = ref(false)

const status = ref(false)
const isEnd = ref(false)
const startMoveTime = ref(0)
const endMoveTime = ref(0)

/** 滑动条相对页面的 left（touchstart 时测量一次） */
let barAreaLeft = 0
let barAreaWidth = IMG_WIDTH

/* 持久化的设备标识（aj-captcha 频控依赖 clientUid） */
function getClientUid(): string {
  const KEY = 'spas_captcha_uid'
  let uid = uni.getStorageSync(KEY) as string
  if (!uid) {
    const hex = '0123456789abcdef'
    const s: string[] = []
    for (let i = 0; i < 36; i++) s[i] = hex.substr(Math.floor(Math.random() * 16), 1)
    s[14] = '4'
    s[19] = hex.substr((parseInt(s[19], 16) & 0x3) | 0x8, 1)
    s[8] = s[13] = s[18] = s[23] = '-'
    uid = 'slider-' + s.join('')
    uni.setStorageSync(KEY, uid)
  }
  return uid
}

function measureBarArea(): Promise<void> {
  return new Promise((resolve) => {
    uni
      .createSelectorQuery()
      .in(instance?.proxy as any)
      .select('.captcha-bar-area')
      .boundingClientRect((rect: any) => {
        if (rect) {
          barAreaLeft = Math.ceil(rect.left)
          barAreaWidth = Math.ceil(rect.width)
        }
        resolve()
      })
      .exec()
  })
}

async function getPictrue() {
  backImgBase.value = ''
  blockBackImgBase.value = ''
  try {
    const data: any = await request<any>({
      url: '/spas/mina/app/captcha/get',
      method: 'POST',
      data: {
        captchaType: 'blockPuzzle',
        clientUid: getClientUid(),
        ts: Date.now(),
      },
      errorMessage: '验证码加载失败',
    })
    backImgBase.value = data.originalImageBase64 || ''
    blockBackImgBase.value = data.jigsawImageBase64 || ''
    backToken.value = data.token || ''
    secretKey.value = data.secretKey || ''
  } catch (e) {
    // 请求层已统一 toast，这里仅提示刷新入口
    text.value = '加载失败，点击 ⟳ 重试'
  }
  await nextTick()
  await measureBarArea()
}

function resetVisual() {
  transitioning.value = true
  moveBlockLeft.value = 0
  leftBarWidth.value = null
  moveBlockBg.value = '#ffffff'
  leftBarBorder.value = '#e4e7eb'
  iconColor.value = '#909399'
  iconText.value = '»'
  setTimeout(() => {
    transitioning.value = false
    text.value = '向右滑动完成验证'
  }, 300)
}

function refresh() {
  tipWords.value = ''
  passFlag.value = false
  finishText.value = ''
  isEnd.value = false
  status.value = false
  resetVisual()
  getPictrue()
}

/* ---- 触摸事件 ---- */
function onTouchStart() {
  if (isEnd.value) return
  startMoveTime.value = Date.now()
  text.value = ''
  moveBlockBg.value = '#2b5cff'
  leftBarBorder.value = '#2b5cff'
  iconColor.value = '#ffffff'
  status.value = true
}

function onTouchMove(e: any) {
  if (!status.value || isEnd.value) return
  const x = Math.ceil(e.touches?.[0]?.pageX ?? 0)
  let moveLeft = x - barAreaLeft
  // 小滑块中心不超过滑动条右端
  const maxLeft = barAreaWidth - Math.floor(BLOCK_SRC_WIDTH / 2) - 2
  if (moveLeft >= maxLeft) moveLeft = maxLeft
  if (moveLeft <= 0) moveLeft = Math.floor(BLOCK_SRC_WIDTH / 2)
  const target = moveLeft - Math.floor(BLOCK_SRC_WIDTH / 2)
  moveBlockLeft.value = target < 0 ? 0 : target
  leftBarWidth.value = moveBlockLeft.value
}

async function onTouchEnd() {
  endMoveTime.value = Date.now()
  if (!status.value || isEnd.value) {
    status.value = false
    return
  }
  status.value = false

  // 上报坐标 = 滑块左缘位移 × (后端底图原生宽 / 前端显示宽)。
  // captcha-1.4.0 内置底图为 310x155 原生、显示 1:1，缩放系数为 1；
  // 若后端改用 620 宽自定义底图（官方老示例），需把 COORD_SCALE 改回 2。
  const moveLeftDistance = Math.round((moveBlockLeft.value || 0) * COORD_SCALE)
  const pointJson = secretKey.value
    ? aesEncrypt(JSON.stringify({ x: moveLeftDistance, y: 5.0 }), secretKey.value)
    : JSON.stringify({ x: moveLeftDistance, y: 5.0 })
  const captchaVerification = secretKey.value
    ? aesEncrypt(`${backToken.value}---${JSON.stringify({ x: moveLeftDistance, y: 5.0 })}`, secretKey.value)
    : `${backToken.value}---${JSON.stringify({ x: moveLeftDistance, y: 5.0 })}`

  try {
    const repData: any = await request<any>({
      url: '/spas/mina/app/captcha/check',
      method: 'POST',
      data: { captchaType: 'blockPuzzle', pointJson, token: backToken.value },
      errorMessage: '校验失败',
    })
    // 校验通过：绿色提示后回调成功并关闭弹窗
    moveBlockBg.value = '#3fbf7f'
    leftBarBorder.value = '#3fbf7f'
    iconColor.value = '#ffffff'
    iconText.value = '✓'
    passFlag.value = true
    isEnd.value = true
    tipWords.value = `${((endMoveTime.value - startMoveTime.value) / 1000).toFixed(2)}s 验证成功`
    const verification = repData?.captchaVerification || captchaVerification
    setTimeout(() => {
      emit('success', verification)
      close()
      refresh()
    }, 600)
  } catch (e) {
    // 校验失败：红色提示并自动刷新重试
    moveBlockBg.value = '#e35b5b'
    leftBarBorder.value = '#e35b5b'
    iconColor.value = '#ffffff'
    iconText.value = '✕'
    passFlag.value = false
    tipWords.value = '验证失败，请重试'
    setTimeout(() => refresh(), 800)
  }
}

function onTouchCancel() {
  if (!isEnd.value) {
    status.value = false
    resetVisual()
  }
}

function close() {
  emit('update:show', false)
}

watch(
  () => props.show,
  (v) => {
    if (v) refresh()
  }
)

/* ---- 样式计算 ---- */
const imgPanelStyle = computed(() => ({ width: `${IMG_WIDTH}px`, height: `${IMG_HEIGHT}px` }))
const barStyle = computed(() => ({
  width: `${IMG_WIDTH}px`,
  height: `${BAR_HEIGHT}px`,
  'line-height': `${BAR_HEIGHT}px`,
  'margin-top': `${IMG_BAR_GAP}px`,
}))
const leftBarStyle = computed(() => ({
  width: leftBarWidth.value != null ? `${leftBarWidth.value}px` : `${BLOCK_SRC_WIDTH}px`,
  height: `${BAR_HEIGHT}px`,
  'border-color': leftBarBorder.value,
  transition: transitioning.value ? 'width .3s' : 'none',
}))
const moveBlockStyle = computed(() => ({
  width: `${BLOCK_SRC_WIDTH}px`,
  height: `${BAR_HEIGHT}px`,
  left: `${moveBlockLeft.value}px`,
  'background-color': moveBlockBg.value,
  transition: transitioning.value ? 'left .3s' : 'none',
}))
const subBlockStyle = computed(() => ({
  width: `${BLOCK_SRC_WIDTH}px`,
  height: `${IMG_HEIGHT}px`,
  top: `-${IMG_HEIGHT + IMG_BAR_GAP}px`,
}))
</script>

<style scoped>
.captcha-mask {
  position: fixed;
  top: 0;
  left: 0;
  z-index: 1001;
  width: 100%;
  height: 100vh;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
}
.captcha-box {
  width: 340px;
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.25);
}
.captcha-top {
  position: relative;
  height: 46px;
  line-height: 46px;
  padding: 0 15px;
  font-size: 15px;
  color: #303133;
  border-bottom: 1px solid #eef0f3;
}
.captcha-top__close {
  position: absolute;
  top: 0;
  right: 0;
  width: 44px;
  height: 46px;
  text-align: center;
  font-size: 22px;
  color: #909399;
}
.captcha-body {
  padding: 15px;
}
.captcha-img-panel {
  position: relative;
  margin: 0 auto;
  border-radius: 6px;
  overflow: hidden;
  background: #f5f7fa;
}
.captcha-img {
  width: 100%;
  height: 100%;
  display: block;
  /* 保持默认平滑插值：锐化插值会把底图上的文字边缘放大成锯齿 */
}
.captcha-refresh {
  position: absolute;
  top: 0;
  right: 0;
  z-index: 3;
  width: 28px;
  height: 28px;
  line-height: 28px;
  text-align: center;
  font-size: 16px;
  color: #fff;
  background: rgba(0, 0, 0, 0.25);
  border-bottom-left-radius: 6px;
}
.captcha-brand {
  position: absolute;
  right: 10px;
  bottom: 8px;
  z-index: 2;
  font-size: 11px;
  font-weight: 400;
  letter-spacing: 1px;
  color: rgba(255, 255, 255, 0.99);
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.35);
  pointer-events: none;
}
.captcha-tips {
  position: absolute;
  left: 0;
  bottom: 0;
  z-index: 3;
  width: 100%;
  height: 26px;
  line-height: 26px;
  text-align: center;
  font-size: 12px;
  color: #fff;
}
.captcha-tips--ok {
  background: rgba(63, 191, 127, 0.85);
}
.captcha-tips--err {
  background: rgba(227, 91, 91, 0.85);
}
.captcha-bar-area {
  position: relative;
  margin: 0 auto;
  background: #f7f9fc;
  text-align: center;
  border: 1px solid #e4e7eb;
  border-radius: 4px;
  box-sizing: content-box;
}
.captcha-bar-msg {
  position: absolute;
  z-index: 2;
  width: 100%;
  height: 100%;
  font-size: 13px;
  color: #909399;
  user-select: none;
}
.captcha-left-bar {
  position: absolute;
  top: -1px;
  left: -1px;
  z-index: 3;
  background: #eef3ff;
  border: 1px solid #e4e7eb;
  border-radius: 4px 0 0 4px;
  box-sizing: content-box;
}
.captcha-move-block {
  position: absolute;
  top: 0;
  left: 0;
  z-index: 4;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  box-shadow: 0 0 3px rgba(0, 0, 0, 0.25);
  border-radius: 4px;
  cursor: pointer;
}
.captcha-move-icon {
  font-size: 16px;
  font-weight: bold;
}
.captcha-sub-block {
  position: absolute;
  top: 0;
  left: 0;
  z-index: 5;
  text-align: center;
}
</style>
