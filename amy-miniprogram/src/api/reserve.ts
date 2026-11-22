/**
 * 预约接口
 *
 * 后端：MinaAppController
 *  - POST /spas/mina/app/reservation/submit                     提交预约（需登录）
 *  - GET  /spas/mina/app/user/reservation/records               我的预约记录（需登录）
 *  - GET  /spas/mina/app/reservation/next/day/order             预约动态（按生效放号日期，首页滚动展示）
 *  - GET  /spas/mina/app/reservation/{orderNum}                 预约详情（按预约单号）
 *  - GET  /spas/mina/app/reservation/{orderNum}/verify-code      刷新核销码（仅本人、仅待核销）
 *  - POST /spas/mina/app/reservation/cancel/{orderNum}           取消预约（仅待核销状态）
 */

import type { NextDayReservation, ReservePerson, ReserveRecord } from '@/types'
import { cardTypeToIdType } from '@/types'
import { request, requestPage } from '@/utils/request'

export interface CreateReservePayload {
  projectId: number
  /** 会员ID（当前登录会员） */
  reservationUserId: number
  /** 预约日期 yyyy-MM-dd */
  date: string
  /** 预约时段 HH:mm:ss */
  time: string
  /** 预约人姓名 */
  name: string
  phone: string
  /** 证件类型 1身份证 2护照 3港澳通行证 4台湾通行证 5军官证 */
  cardType: number
  idNum: string
  /** 随行人员（不含本人） */
  companions: ReservePerson[]
  /** 滑动拼图验证码二次校验凭证（滑块校验通过后返回，必传） */
  captchaVerification: string
}

/** 后端订单 VO：compUsers 为 JSON 字符串列表 */
interface RawReserveRecord extends Omit<ReserveRecord, 'compUsers'> {
  compUsers: string[] | null
}

/** 解析随行人员（后端 comp_users 元素为 UserIdentity 序列化后的 JSON 字符串） */
function parseCompUsers(list: string[] | null): ReservePerson[] {
  if (!list || !list.length) return []
  const result: ReservePerson[] = []
  list.forEach((item) => {
    try {
      const obj = JSON.parse(item)
      result.push({
        name: obj.name || '',
        idType: Number(obj.idType ?? 1),
        idNum: obj.idNum || '',
        phone: obj.phone || '',
        gender: obj.gender,
      })
    } catch (e) {
      // 兼容历史脏数据：忽略无法解析的元素
      console.warn('解析随行人员失败', item)
    }
  })
  return result
}

/** 统一时段格式为 HH:mm:ss，后端 @JsonFormat 要求 yyyy-MM-dd HH:mm:ss */
function toHms(time: string): string {
  const parts = (time || '').split(':')
  while (parts.length < 3) parts.push('00')
  return parts
    .slice(0, 3)
    .map((item) => (item || '00').padStart(2, '0'))
    .join(':')
}

function normalizeRecord(raw: RawReserveRecord): ReserveRecord {
  return {
    ...raw,
    reservationStatusName: raw.reservationStatusName || '',
    statusMsg: raw.statusMsg || '',
    compUsers: parseCompUsers(raw.compUsers),
  }
}

/**
 * 提交预约
 * POST /spas/mina/app/reservation/submit
 */
export async function createReserve(payload: CreateReservePayload): Promise<boolean> {
  return request<boolean>({
    url: '/spas/mina/app/reservation/submit',
    method: 'POST',
    data: {
      projectId: payload.projectId,
      reservationUserId: payload.reservationUserId,
      name: payload.name,
      phone: payload.phone,
      idType: cardTypeToIdType(payload.cardType),
      idNum: payload.idNum,
      guestsNum: 1 + payload.companions.length,
      reservationTime: `${payload.date} ${toHms(payload.time)}`,
      // 后端 SubmitReservationReq.compUsers 为 List<FollowerReq>（对象数组），
      // 仅接收 name / idNum / phone 三个字段，且 phone 为必填，故这里直接传对象
      compUsers: payload.companions.map((item) => ({
        name: item.name,
        idNum: item.idNum,
        phone: item.phone || payload.phone,
      })),
      // 滑动拼图验证码二次校验凭证（后端校验 token 一次性）
      captchaVerification: payload.captchaVerification,
    },
  })
}

/**
 * 我的预约记录
 * GET /spas/mina/app/user/reservation/records
 */
export async function getRecords(): Promise<ReserveRecord[]> {
  const page = await requestPage<RawReserveRecord>({
    url: '/spas/mina/app/user/reservation/records',
    pageNum: 1,
    pageSize: 100,
  })
  return (page.rows || []).map(normalizeRecord)
}

/**
 * 预约动态列表：后端按「生效放号库存 → 对应日期」返回订单，
 * 用于首页「预约动态」滚动展示
 * GET /spas/mina/app/reservation/next/day/order
 *
 * 说明：后端该接口未加 @MinaApiAuth，属于首页公开信息，游客也要能看到预约动态；
 * 因此显式 auth:false（否则 request() 默认索要登录态，游客请求发不出，播报恒为空）。
 * 失败时静默降级为空，不影响首页其他内容展示；手机号后端已脱敏。
 */
export async function getNextDayReservations(): Promise<NextDayReservation[]> {
  const list = await request<NextDayReservation[]>({
    url: '/spas/mina/app/reservation/next/day/order',
    auth: false,
    skipAuthRedirect: true,
    // 首页滚动播报属于展示增强，失败时静默降级为空，不弹全局错误提示
    silent: true,
  })
  return Array.isArray(list) ? list : []
}

/**
 * 预约详情（按预约单号查询）
 * GET /spas/mina/app/reservation/{orderNum}
 */
export async function getRecordDetail(orderNum: string): Promise<ReserveRecord> {
  const raw = await request<RawReserveRecord>({
    url: `/spas/mina/app/reservation/${encodeURIComponent(orderNum)}`,
  })
  if (!raw || !raw.reservationNum) {
    throw new Error('未找到该预约单')
  }
  return normalizeRecord(raw)
}

/** 刷新核销码响应（对应后端 MinaVerifyCodeResp） */
export interface VerifyCodeRefreshResult {
  /** 新核销码 */
  verifyCode: string
  /** 有效持续时长，单位分钟 */
  verifyCodeEffectiveDuration: number
}

/**
 * 刷新核销码：GET /spas/mina/app/reservation/{orderNum}/verify-code
 *
 * 后端每次强制换发一枚新码并重置 Redis 有效期（校验订单本人 + 待核销状态），
 * 供预约详情页打开时 / 倒计时归零时调用。
 * 详情接口 getRecordDetail 只会复用未过期旧码，不能拿来做倒计时刷新，两者语义不同。
 */
export async function refreshVerifyCode(orderNum: string): Promise<VerifyCodeRefreshResult> {
  const data = await request<VerifyCodeRefreshResult>({
    url: `/spas/mina/app/reservation/${encodeURIComponent(orderNum)}/verify-code`,
    // GET 带写副作用（每次换发新码），加时间戳避免任何一层缓存返回上一枚码
    data: { _: Date.now() },
  })
  const code = typeof data?.verifyCode === 'string' ? data.verifyCode.trim() : ''
  if (!code) throw new Error('未获取到新的核验码')
  return { ...data, verifyCode: code }
}

/**
 * 取消预约（仅订单本人、仅「待核销」状态可取消）
 * POST /spas/mina/app/reservation/cancel/{orderNum}
 */
export async function cancelReserve(orderNum: string): Promise<boolean> {
  return request<boolean>({
    url: `/spas/mina/app/reservation/cancel/${encodeURIComponent(orderNum)}`,
    method: 'POST',
  })
}
