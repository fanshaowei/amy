/**
 * 通用工具函数
 */

import type { ReservePerson } from '@/types'

/** 补零 */
export function pad(n: number): string {
  return n.toString().padStart(2, '0')
}

/** 格式化日期为 YYYY-MM-DD */
export function formatDate(d: Date): string {
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

/** 格式化日期时间为 YYYY-MM-DD HH:mm:ss */
export function formatDateTime(d: Date): string {
  return `${formatDate(d)} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

/** 时分秒 -> HH:mm（去掉秒） */
export function hmsToHm(t: string): string {
  return t ? t.slice(0, 5) : ''
}

/** 生成未来 N 天的日期列表（不含今天） */
export function getDatesByDay(day: number): Date[] {
  const dates: Date[] = []
  const today = new Date()
  for (let i = 1; i <= day; i++) {
    const d = new Date(today)
    d.setDate(today.getDate() + i)
    dates.push(d)
  }
  return dates
}

/** 根据开始/结束/间隔生成时段列表（分钟） */
export function genTimes(startHour: string, endHour: string, interval: number): string[] {
  const times: string[] = []
  const [sh, sm] = startHour.split(':').map(Number)
  const [eh, em] = endHour.split(':').map(Number)
  let startMin = sh * 60 + sm
  const endMin = eh * 60 + em
  while (startMin + interval <= endMin) {
    const h = Math.floor(startMin / 60)
    const m = startMin % 60
    times.push(`${pad(h)}:${pad(m)}`)
    startMin += interval
  }
  return times
}

/**
 * 根据开始/结束时间与间隔秒数生成时段列表（HH:mm:ss）
 * 与后端 MinaAppService#calculateReservationTimes 规则保持一致
 */
export function genTimesBySecond(
  startTime: string,
  endTime: string,
  intervalSecond: number
): string[] {
  const times: string[] = []
  if (!startTime || !endTime || !intervalSecond || intervalSecond <= 0) return times
  const toSecond = (t: string) => {
    const [h = 0, m = 0, s = 0] = t.split(':').map(Number)
    return h * 3600 + m * 60 + s
  }
  const toHms = (sec: number) =>
    `${pad(Math.floor(sec / 3600))}:${pad(Math.floor((sec % 3600) / 60))}:${pad(sec % 60)}`
  let cur = toSecond(startTime)
  const end = toSecond(endTime)
  while (cur < end) {
    times.push(toHms(cur))
    cur += intervalSecond
  }
  return times
}

/** HH:mm:ss 列表 -> 间隔分钟数 */
export function slotIntervalMinutes(times: string[]): number {
  if (!times || times.length < 2) return 0
  const toSecond = (t: string) => {
    const [h = 0, m = 0, s = 0] = t.split(':').map(Number)
    return h * 3600 + m * 60 + s
  }
  return Math.round((toSecond(times[1]) - toSecond(times[0])) / 60)
}

/** 手机号脱敏 */
export function maskTel(tel: string): string {
  if (!tel || tel.length !== 11) return tel || ''
  return tel.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2')
}

/** 身份证号脱敏 */
export function maskIdCard(id: string): string {
  if (!id) return ''
  if (id.length < 8) return id
  return id.slice(0, 4) + '**********' + id.slice(-4)
}

/** 显示姓名（多人时） */
export function displayNames(persons: ReservePerson[]): string {
  if (!persons || persons.length === 0) return ''
  if (persons.length === 1) return persons[0].name
  return `${persons[0].name} 等${persons.length}人`
}
