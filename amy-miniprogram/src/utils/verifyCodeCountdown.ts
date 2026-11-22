/**
 * 核销码有效期倒计时（预约详情页专用）。
 *
 * 与后端 Redis TTL 对齐：每次 open() 都通过 refresh() 换发一枚新码，
 * 拿到 verifyCodeEffectiveDuration（分钟）后按绝对 deadline 倒计时，
 * 归零时再次 open()。调度只依赖 vue 的 ref 与定时器，不直接碰 uni 生命周期，
 * 由页面在 onShow / onHide / onUnload 里调用 resume / stop / dispose，
 * 因此「倒计时只在打开预约详情页期间生效」。
 */

import { ref } from 'vue'
import type { Ref } from 'vue'

/** 刷新接口返回的新码与有效时长（分钟） */
export interface VerifyCodeRefreshResult {
  verifyCode: string
  verifyCodeEffectiveDuration: number
}

export interface CountdownOptions {
  /** 取一枚新核销码 */
  refresh: () => Promise<VerifyCodeRefreshResult>
  /** 把新码 / 新时长写回页面数据，触发二维码重绘 */
  onUpdate: (result: VerifyCodeRefreshResult) => void
  /** 刷新失败回调（由页面决定是否提示，避免与全局错误提示重复） */
  onError?: (err: unknown) => void
  /** 后端未返回合法时长时的兜底分钟数 */
  fallbackMinutes?: number
}

export interface VerifyCodeCountdown {
  /** 剩余有效秒数（0 = 未启动 / 已停表） */
  remaining: Ref<number>
  /** 是否正在获取新核销码 */
  refreshing: Ref<boolean>
  /** 首次进入或需要强制换码：取新码并从满额开始倒计时 */
  open: () => Promise<void>
  /** 页面重新可见：按真实经过时间恢复；已过期则立即取新码 */
  resume: () => void
  /** 页面不可见：停表但保留进度，回来按真实时间校正 */
  stop: () => void
  /** 页面销毁：停表并永久丢弃在途请求 */
  dispose: () => void
}

/** 心跳间隔：按绝对 deadline 算剩余时间，切后台被节流后回到前台也能立刻校正 */
const TICK_MS = 250
/** 后端未返回 / 返回非法时长时的兜底分钟数 */
const DEFAULT_MINUTES = 1

/** 解析后端返回的核销码有效时长（分钟），非法值兜底 1 分钟 */
export function resolveDurationMinutes(value?: number): number {
  const n = Number(value)
  return Number.isFinite(n) && n > 0 ? Math.floor(n) : DEFAULT_MINUTES
}

/** 剩余秒数 -> mm:ss；超过 1 小时显示 hh:mm:ss */
export function formatCountdown(seconds: number): string {
  const total = Math.max(0, Math.floor(seconds))
  const pad = (v: number) => String(v).padStart(2, '0')
  const h = Math.floor(total / 3600)
  const m = Math.floor((total % 3600) / 60)
  const s = total % 60
  return h > 0 ? `${pad(h)}:${pad(m)}:${pad(s)}` : `${pad(m)}:${pad(s)}`
}

export function createVerifyCodeCountdown(options: CountdownOptions): VerifyCodeCountdown {
  const remaining = ref(0)
  const refreshing = ref(false)
  const fallbackMinutes = options.fallbackMinutes ?? DEFAULT_MINUTES

  let deadline = 0
  let timer: ReturnType<typeof setInterval> | null = null
  let inFlight = false
  let paused = false
  let disposed = false

  function clearTimer() {
    if (timer !== null) {
      clearInterval(timer)
      timer = null
    }
  }

  /** 只装心跳：每次 tick 都按 deadline 重算，归零时停表并换码 */
  function startTicking() {
    clearTimer()
    timer = setInterval(() => {
      if (disposed || paused) return
      const left = Math.max(0, Math.ceil((deadline - Date.now()) / 1000))
      remaining.value = left
      if (left <= 0) {
        clearTimer()
        void open()
      }
    }, TICK_MS)
  }

  /** 设置本轮到期时间；页面隐藏时只记时间、不起心跳，等 resume 校正 */
  function arm(minutes: number) {
    deadline = Date.now() + minutes * 60 * 1000
    remaining.value = minutes * 60
    if (!paused) startTicking()
  }

  async function open(): Promise<void> {
    if (disposed || inFlight) return
    inFlight = true
    refreshing.value = true
    try {
      const result = await options.refresh()
      if (disposed) return
      const normalized: VerifyCodeRefreshResult = {
        ...result,
        verifyCode: String(result.verifyCode || '').trim(),
      }
      options.onUpdate(normalized)
      arm(resolveDurationMinutes(result.verifyCodeEffectiveDuration ?? fallbackMinutes))
    } catch (e) {
      if (disposed) return
      clearTimer()
      deadline = 0
      remaining.value = 0
      options.onError?.(e)
    } finally {
      inFlight = false
      if (!disposed) refreshing.value = false
    }
  }

  function resume() {
    if (disposed) return
    // 先解除暂停：若此刻有在途请求，它返回后 arm() 会据此直接起心跳
    paused = false
    if (inFlight || refreshing.value) return
    // 从未启动过（如首屏刷新失败）→ 取新码
    if (!deadline) {
      void open()
      return
    }
    const left = Math.max(0, Math.ceil((deadline - Date.now()) / 1000))
    // 页面隐藏期间已经过期 → 立即换码，避免继续展示失效二维码
    if (left <= 0) {
      void open()
      return
    }
    remaining.value = left
    startTicking()
  }

  function stop() {
    paused = true
    clearTimer()
  }

  function dispose() {
    disposed = true
    paused = true
    clearTimer()
    deadline = 0
    remaining.value = 0
    refreshing.value = false
  }

  return { remaining, refreshing, open, resume, stop, dispose }
}
