/**
 * 项目/活动接口
 *
 * 后端：MinaAppController
 *  - GET /spas/mina/app/homepage/project                       首页可预约项目列表
 *  - GET /spas/mina/app/project/reservation/info/{projectId}    预约须知 + 各日期号源明细
 */

import type { Activity, ReservationInfo, TimeSlot } from '@/types'
import { request, requestPage } from '@/utils/request'
import { hmsToHm, slotIntervalMinutes } from '@/utils'

/** 后端首页项目列表项（MinaHomePageProjectResp） */
interface MinaHomePageProjectResp {
  projectId: number
  projectName: string
  coverArtUrl: string
  reservationStartDate: string
  reservationStartTime: string
  reservationEndDate: string
  reservationEndTime: string
  remainingReservationCount: number
  totalReservationCount: number
  cutOffTime: string
  reservationStatus: number
  travelerCount: number
  reservationStaySecond: number
}

/** 默认下单倒计时（秒），后端未配置时兜底 */
const DEFAULT_STAY_SECOND = 180
/** 默认参观时长（分钟），无法推导时兜底 */
const DEFAULT_INTERVAL_MINUTES = 30

/** 首页项目列表缓存，供详情页复用，避免重复请求 */
let cachedProjects: Activity[] = []
/** 最近一次详情缓存，供 getSlots 复用 */
let cachedInfoProjectId = 0
let cachedInfo: ReservationInfo | null = null

function toActivity(raw: MinaHomePageProjectResp): Activity {
  return {
    projectId: raw.projectId,
    projectName: raw.projectName || '',
    coverArtUrl: raw.coverArtUrl || '',
    reservationStartDate: raw.reservationStartDate || '',
    reservationEndDate: raw.reservationEndDate || '',
    reservationStartTime: raw.reservationStartTime || '',
    reservationEndTime: raw.reservationEndTime || '',
    remainingReservationCount: raw.remainingReservationCount || 0,
    totalReservationCount: raw.totalReservationCount || 0,
    cutOffTime: raw.cutOffTime || '',
    reservationStatus: raw.reservationStatus || 1,
    travelerCount: raw.travelerCount || 0,
    reservationStaySecond: raw.reservationStaySecond || DEFAULT_STAY_SECOND,
  }
}

/**
 * 首页可预约项目列表
 * GET /spas/mina/app/homepage/project
 */
export async function getActivities(): Promise<Activity[]> {
  const page = await requestPage<MinaHomePageProjectResp>({
    url: '/spas/mina/app/homepage/project',
    pageNum: 1,
    pageSize: 100,
  })
  cachedProjects = (page.rows || []).map(toActivity)
  return cachedProjects
}

/**
 * 项目预约信息（预约须知 + 各日期号源明细）
 * GET /spas/mina/app/project/reservation/info/{projectId}
 */
export async function getReservationInfo(projectId: number): Promise<ReservationInfo> {
  const info = await request<ReservationInfo>({
    url: `/spas/mina/app/project/reservation/info/${projectId}`,
  })
  cachedInfoProjectId = projectId
  cachedInfo = {
    reservationNotes: info?.reservationNotes || '',
    reservationDetails: info?.reservationDetails || [],
  }
  return cachedInfo
}

/** 从缓存或接口中获取项目基本信息 */
async function findProject(projectId: number): Promise<Activity> {
  let base = cachedProjects.find((item) => item.projectId === projectId)
  if (!base) {
    await getActivities()
    base = cachedProjects.find((item) => item.projectId === projectId)
  }
  if (!base) throw new Error('项目不存在或已下架')
  return base
}

/**
 * 项目详情（预约页所需：项目基本信息 + 预约须知 + 可预约日期）
 * 说明：后端未提供小程序版项目详情接口，此处由「列表 + 预约信息」组合而成
 */
export async function getActivityDetail(projectId: number): Promise<Activity> {
  const info = await getReservationInfo(projectId)
  const base = await findProject(projectId)

  const details = info.reservationDetails || []
  const times = (details[0]?.reservationTimeDetails || []).map((item) => item.reservationTime)
  const intervalMinutes = slotIntervalMinutes(times) || DEFAULT_INTERVAL_MINUTES
  const perSlotCount = times.length
    ? Math.round(base.totalReservationCount / times.length)
    : base.totalReservationCount

  return {
    ...base,
    reservationNotes: info.reservationNotes,
    reservationDates: details.map((item) => item.reservationDate),
    intervalMinutes,
    desc: `开放时间 ${hmsToHm(base.reservationStartTime)}-${hmsToHm(base.reservationEndTime)}，每时段 ${perSlotCount} 人，每日 ${base.totalReservationCount} 人`,
  }
}

/**
 * 指定日期的可预约时段号源
 * （数据来源于项目预约信息接口）
 */
export async function getSlots(projectId: number, date: string): Promise<TimeSlot[]> {
  let info = cachedInfoProjectId === projectId ? cachedInfo : null
  if (!info) {
    info = await getReservationInfo(projectId)
  }
  const detail = (info.reservationDetails || []).find((item) => item.reservationDate === date)
  return (detail?.reservationTimeDetails || []).map((item) => {
    const remain = item.remainingReservationCount || 0
    return {
      time: item.reservationTime,
      remain,
      status: item.reservationStatus,
      full: item.reservationStatus === 3 || remain <= 0,
    }
  })
}
